package com.quantrity.antscaledisplay;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.signature.HmacSha1MessageSigner;

/** Garmin SSO/OAuth authentication independent of Android UI and persistence details. */
final class GarminAuthenticator {
    private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
    private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
    private static final String SSO_ROOT = "https://sso.garmin.com/sso";
    private static final String SSO_EMBED_URL = SSO_ROOT + "/embed";
    private static final String SSO_SIGNIN_URL = SSO_ROOT + "/signin";
    private static final String SSO_MFA_URL = SSO_ROOT + "/verifyMFA/loginEnterMfaCode";
    private static final String OAUTH1_URL =
            "https://connectapi.garmin.com/oauth-service/oauth/preauthorized";
    private static final String OAUTH2_URL =
            "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";
    private static final Pattern TICKET_PATTERN = Pattern.compile("ticket=([^\"'&\\\\]+)");

    enum SignInResult { SUCCESS, CANCELLED, INVALID, RETRY }
    enum RenewalResult { SUCCESS, RETRY, INVALID }

    interface TokenStore {
        String accessToken();
        long accessExpiry();
        String oauth1Token();
        String oauth1Secret();
        String mfaToken();
        void storeOAuth1(String token, String secret, String mfaToken, long mfaExpiry);
        boolean storeAccess(String token, long expiry, boolean tokensOnly);
        void scheduleRefresh();
    }

    private final GarminHttpClient http;
    private final TokenStore tokens;
    private final MfaCodeProvider mfaProvider;
    private final Clock clock;

    interface Clock { long epochSeconds(); }

    GarminAuthenticator(GarminHttpClient http, TokenStore tokens, MfaCodeProvider mfaProvider) {
        this(http, tokens, mfaProvider, () -> System.currentTimeMillis() / 1000);
    }

    GarminAuthenticator(GarminHttpClient http, TokenStore tokens, MfaCodeProvider mfaProvider,
                        Clock clock) {
        this.http = http;
        this.tokens = tokens;
        this.mfaProvider = mfaProvider;
        this.clock = clock;
    }

    SignInResult signIn(String username, String password) {
        if (!hasText(username) || !hasText(password)) return SignInResult.INVALID;
        if (hasValidAccessToken(0)) {
            tokens.scheduleRefresh();
            return SignInResult.SUCCESS;
        }
        if (hasOAuth1() && renew(false) == RenewalResult.SUCCESS) return SignInResult.SUCCESS;

        try {
            http.clearCookies();
            Map<String, String> service = serviceParameters();
            Map<String, String> pageHeaders = standardHeaders();
            GarminHttpClient.Response response = http.execute(
                    "GET", SSO_SIGNIN_URL, service, null, pageHeaders, true);
            if (response.code != 200) return signInFailure(response.code);

            String csrf = extractCsrf(response.body);
            if (csrf.isEmpty()) return SignInResult.INVALID;
            Map<String, String> login = new HashMap<>();
            login.put("username", sanitize(username));
            login.put("password", sanitize(password));
            login.put("embed", "true");
            login.put("_csrf", csrf);
            Map<String, String> formHeaders = standardHeaders();
            formHeaders.put("Referer", SSO_SIGNIN_URL);
            formHeaders.put("Origin", "https://sso.garmin.com");
            formHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            response = http.execute("POST", SSO_SIGNIN_URL, service, login, formHeaders, true);

            String ticket = ticket(response.body);
            if (ticket.isEmpty()) ticket = ticket(response.url);
            if (ticket.isEmpty() && isMfaChallenge(response.body)) {
                String code = mfaProvider == null ? null : mfaProvider.requestCode();
                if (!hasText(code)) return SignInResult.CANCELLED;
                Map<String, String> mfa = new HashMap<>();
                mfa.put("mfa-code", code);
                mfa.put("embed", "true");
                mfa.put("_csrf", valueOr(extractCsrf(response.body), csrf));
                mfa.put("fromPage", "setupEnterMfaCode");
                response = http.execute("POST", SSO_MFA_URL, service, mfa,
                        formHeaders, false);
                String location = response.firstHeader("Location");
                ticket = ticket(location);
                if (ticket.isEmpty() && location != null && location.contains("logintoken=")) {
                    response = http.execute("GET", location, null, null, pageHeaders, true);
                    ticket = valueOr(ticket(response.url), ticket(response.body));
                }
            }
            if (ticket.isEmpty()) return signInFailure(response.code);

            OAuthConsumer consumer = consumer();
            GarminHttpClient.Response oauth1 = http.execute("GET",
                    consumer.sign(OAUTH1_URL + "?ticket=" + ticket
                            + "&login-url=https://sso.garmin.com/sso/embed"
                            + "&accepts-mfa-tokens=true"), null, null, null, true);
            if (oauth1.code != 200) return signInFailure(oauth1.code);
            Map<String, String> oauth1Values = parseForm(oauth1.body);
            String oauth1Token = oauth1Values.get("oauth_token");
            String oauth1Secret = oauth1Values.get("oauth_token_secret");
            if (!hasText(oauth1Token) || !hasText(oauth1Secret)) return SignInResult.INVALID;
            tokens.storeOAuth1(oauth1Token, oauth1Secret, oauth1Values.get("mfa_token"),
                    parseMfaExpirationTimestamp(oauth1Values.get("mfa_expiration_timestamp")));

            consumer.setTokenWithSecret(oauth1Token, oauth1Secret);
            RenewalResult exchange = exchangeOAuth2(consumer, false);
            if (exchange == RenewalResult.SUCCESS) return SignInResult.SUCCESS;
            return exchange == RenewalResult.RETRY ? SignInResult.RETRY : SignInResult.INVALID;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SignInResult.CANCELLED;
        } catch (Exception exception) {
            return SignInResult.RETRY;
        }
    }

    RenewalResult renewInBackground() { return renew(true); }

    String accessToken() {
        if (hasValidAccessToken(60)) return tokens.accessToken();
        return renew(false) == RenewalResult.SUCCESS ? tokens.accessToken() : null;
    }

    private RenewalResult renew(boolean tokensOnly) {
        if (!hasOAuth1()) return RenewalResult.INVALID;
        try {
            OAuthConsumer consumer = consumer();
            consumer.setTokenWithSecret(tokens.oauth1Token(), tokens.oauth1Secret());
            return exchangeOAuth2(consumer, tokensOnly);
        } catch (Exception exception) {
            return RenewalResult.RETRY;
        }
    }

    private RenewalResult exchangeOAuth2(OAuthConsumer consumer, boolean tokensOnly) {
        try {
            String body = hasText(tokens.mfaToken()) ? "mfa_token=" + URLEncoder.encode(
                    tokens.mfaToken(), StandardCharsets.UTF_8.name()) : "";
            VirtualRequest signed = new VirtualRequest(OAUTH2_URL, "POST", body,
                    "application/x-www-form-urlencoded");
            consumer.sign(signed);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            String authorization = signed.getHeader("Authorization");
            if (authorization != null) headers.put("Authorization", authorization);
            GarminHttpClient.Response response = http.executeRaw("POST", OAUTH2_URL, headers,
                    body.getBytes(StandardCharsets.UTF_8), true);
            if (response.code == 200) {
                JSONObject json = new JSONObject(response.body);
                String accessToken = json.getString("access_token");
                long expiry = clock.epochSeconds() + json.optLong("expires_in", 3600);
                return tokens.storeAccess(accessToken, expiry, tokensOnly)
                        ? RenewalResult.SUCCESS : RenewalResult.RETRY;
            }
            return temporary(response.code) ? RenewalResult.RETRY : RenewalResult.INVALID;
        } catch (Exception exception) {
            return RenewalResult.RETRY;
        }
    }

    private boolean hasValidAccessToken(long safetySeconds) {
        return hasText(tokens.accessToken())
                && tokens.accessExpiry() > clock.epochSeconds() + safetySeconds;
    }

    private boolean hasOAuth1() {
        return hasText(tokens.oauth1Token()) && hasText(tokens.oauth1Secret());
    }

    private static OAuthConsumer consumer() {
        OAuthConsumer consumer = new DefaultOAuthConsumer(
                OAUTH1_CONSUMER_KEY, OAUTH1_CONSUMER_SECRET);
        consumer.setMessageSigner(new HmacSha1MessageSigner());
        return consumer;
    }

    private static SignInResult signInFailure(int code) {
        return temporary(code) ? SignInResult.RETRY : SignInResult.INVALID;
    }

    private static boolean temporary(int code) { return code == 408 || code == 429 || code >= 500; }
    private static boolean isMfaChallenge(String body) {
        return body != null && (body.contains("MFA") || body.contains("mfa-code"));
    }
    private static String sanitize(String value) {
        return value.trim().replaceAll("[\\n\\r]", "");
    }
    private static boolean hasText(String value) { return value != null && !value.isEmpty(); }
    private static String valueOr(String first, String second) {
        return hasText(first) ? first : second == null ? "" : second;
    }

    static String extractCsrf(String html) {
        if (html == null) return "";
        Matcher first = Pattern.compile(
                "name=[\"']_csrf[\"'][^>]*value=[\"']([^\"']+)[\"']").matcher(html);
        if (first.find()) return first.group(1);
        Matcher second = Pattern.compile(
                "value=[\"']([^\"']+)[\"'][^>]*name=[\"']_csrf[\"']").matcher(html);
        return second.find() ? second.group(1) : "";
    }

    static String ticket(String content) {
        if (content == null) return "";
        Matcher matcher = TICKET_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static Map<String, String> parseForm(String form) throws Exception {
        Map<String, String> values = new HashMap<>();
        if (form == null) return values;
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) values.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()));
        }
        return values;
    }

    private static Map<String, String> serviceParameters() {
        Map<String, String> values = new HashMap<>();
        values.put("id", "gauth-widget");
        values.put("embedWidget", "true");
        values.put("gauthHost", SSO_ROOT);
        values.put("clientId", "GarminConnect");
        values.put("locale", "en");
        values.put("redirectAfterAccountLoginUrl", SSO_EMBED_URL);
        values.put("redirectAfterAccountCreationUrl", SSO_EMBED_URL);
        values.put("createAccountShown", "true");
        values.put("openCreateAccount", "false");
        values.put("displayNameShown", "false");
        values.put("consumeServiceTicket", "false");
        values.put("initialFocus", "true");
        values.put("generateExtraServiceTicket", "true");
        values.put("generateTwoFactorAuthTicket", "false");
        values.put("generateNoServiceTicket", "false");
        values.put("globalExitUrl", SSO_EMBED_URL);
        return values;
    }

    private static Map<String, String> standardHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,"
                + "image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        return headers;
    }

    static long parseMfaExpirationTimestamp(String value) {
        if (!hasText(value)) return -1;
        try {
            long numeric = Long.parseLong(value);
            return numeric > 10_000_000_000L ? numeric / 1000 : numeric;
        } catch (NumberFormatException ignored) {
            // Try Garmin's timestamp representations below.
        }
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss.SSS"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return Objects.requireNonNull(format.parse(value)).getTime() / 1000;
            } catch (Exception ignored) {
                // Try the next format.
            }
        }
        return -1;
    }

    private static final class VirtualRequest implements HttpRequest {
        private String url;
        private final String method;
        private final Map<String, String> headers = new HashMap<>();
        private final byte[] payload;
        private final String contentType;

        VirtualRequest(String url, String method, String payload, String contentType) {
            this.url = url;
            this.method = method;
            this.payload = payload.getBytes(StandardCharsets.UTF_8);
            this.contentType = contentType;
            headers.put("Content-Type", contentType);
        }

        @Override public String getMethod() { return method; }
        @Override public String getRequestUrl() { return url; }
        @Override public void setRequestUrl(String url) { this.url = url; }
        @Override public void setHeader(String name, String value) { headers.put(name, value); }
        @Override public String getHeader(String name) { return headers.get(name); }
        @Override public Map<String, String> getAllHeaders() { return headers; }
        @Override public InputStream getMessagePayload() { return new ByteArrayInputStream(payload); }
        @Override public String getContentType() { return contentType; }
        @Override public Object unwrap() { return null; }
    }
}
