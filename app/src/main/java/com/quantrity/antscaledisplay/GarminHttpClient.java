package com.quantrity.antscaledisplay;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.CookieHandler;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Generic Garmin HTTP transport with explicit cookies, redirects, headers, and decoding. */
class GarminHttpClient {
    static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    interface Transport {
        Response execute(Request request) throws Exception;
    }

    static final class Request {
        final String method;
        final String url;
        final Map<String, String> headers;
        final byte[] body;
        final boolean followRedirects;

        Request(String method, String url, Map<String, String> headers, byte[] body,
                boolean followRedirects) {
            this.method = method;
            this.url = url;
            this.headers = headers == null ? Collections.emptyMap() : headers;
            this.body = body;
            this.followRedirects = followRedirects;
        }
    }

    static final class Response {
        final int code;
        final String body;
        final String url;
        final Map<String, List<String>> headers;

        Response(int code, String body, String url, Map<String, List<String>> headers) {
            this.code = code;
            this.body = body == null ? "" : body;
            this.url = url;
            this.headers = headers == null ? Collections.emptyMap() : headers;
        }

        String firstHeader(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)
                        && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    return entry.getValue().get(0);
                }
            }
            return null;
        }
    }

    private final Transport transport;

    GarminHttpClient() {
        this(false);
    }

    GarminHttpClient(boolean installProcessCookieHandler) {
        this(new UrlConnectionTransport(installProcessCookieHandler));
    }

    GarminHttpClient(Transport transport) {
        this.transport = transport;
    }

    Response execute(String method, String url, Map<String, String> query,
                     Map<String, String> form, Map<String, String> headers,
                     boolean followRedirects) throws Exception {
        String fullUrl = appendQuery(url, query);
        byte[] body = form == null || form.isEmpty()
                ? null : encode(form).getBytes(StandardCharsets.UTF_8);
        return executeRaw(method, fullUrl, headers, body, followRedirects);
    }

    Response executeRaw(String method, String url, Map<String, String> headers, byte[] body,
                        boolean followRedirects) throws Exception {
        return transport.execute(new Request(method, url, headers, body, followRedirects));
    }

    void clearCookies() {
        if (transport instanceof UrlConnectionTransport) {
            ((UrlConnectionTransport) transport).clearCookies();
        }
    }

    private static String appendQuery(String url, Map<String, String> query) throws Exception {
        if (query == null || query.isEmpty()) return url;
        return url + (url.contains("?") ? "&" : "?") + encode(query);
    }

    private static String encode(Map<String, String> values) throws Exception {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (encoded.length() > 0) encoded.append('&');
            encoded.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
        }
        return encoded.toString();
    }

    private static final class UrlConnectionTransport implements Transport {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        UrlConnectionTransport(boolean installProcessCookieHandler) {
            if (installProcessCookieHandler) CookieHandler.setDefault(cookies);
        }

        @Override
        public Response execute(Request request) throws Exception {
            URI uri = URI.create(request.url);
            HttpURLConnection connection = (HttpURLConnection) new URL(request.url).openConnection();
            connection.setRequestMethod(request.method);
            connection.setInstanceFollowRedirects(request.followRedirects);
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            for (Map.Entry<String, List<String>> cookie : cookies.get(uri,
                    Collections.emptyMap()).entrySet()) {
                connection.setRequestProperty(cookie.getKey(), join(cookie.getValue()));
            }
            for (Map.Entry<String, String> header : request.headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (request.body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(request.body.length);
                try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
                    output.write(request.body);
                }
            }

            int code = connection.getResponseCode();
            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            cookies.put(uri, responseHeaders);
            InputStream stream = code >= 400
                    ? connection.getErrorStream() : connection.getInputStream();
            String body = read(stream);
            String finalUrl = connection.getURL().toString();
            connection.disconnect();
            return new Response(code, body, finalUrl, responseHeaders);
        }

        void clearCookies() { cookies.getCookieStore().removeAll(); }

        private static String join(List<String> values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) result.append("; ");
                result.append(value);
            }
            return result.toString();
        }

        private static String read(InputStream stream) throws Exception {
            if (stream == null) return "";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                return result.toString();
            }
        }
    }
}
