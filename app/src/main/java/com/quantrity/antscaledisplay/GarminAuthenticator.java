package com.quantrity.antscaledisplay;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    private static final String TAG = "GarminAuth";
    private static final String OAUTH1_CONSUMER_KEY = "fc3e99d2-118c-44b8-8ae3-03370dde24c0";
    private static final String OAUTH1_CONSUMER_SECRET = "E08WAR897WEy2knn7aFBrvegVAf0AFdWBBF";
    private static final String MOBILE_LOGIN_URL = "https://sso.garmin.com/mobile/api/login";
    private static final String MOBILE_MFA_URL =
            "https://sso.garmin.com/mobile/api/mfa/verifyCode";
    private static final String MOBILE_CLIENT_ID = "GCM_IOS_DARK";
    private static final String MOBILE_SERVICE_URL =
            "https://mobile.integration.garmin.com/gcm/ios";
    private static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_7 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148";
    private static final String OAUTH1_URL =
            "https://connectapi.garmin.com/oauth-service/oauth/preauthorized";
    private static final String OAUTH2_URL =
            "https://connectapi.garmin.com/oauth-service/oauth/exchange/user/2.0";

    enum SignInResult { SUCCESS, CANCELLED, INVALID, RETRY }
    enum RenewalResult { SUCCESS, RETRY, INVALID }
    enum FailureKind {
        NONE, INVALID_CREDENTIALS, RATE_LIMITED, NETWORK, SERVER, PROTOCOL, STORAGE, CANCELLED
    }
    enum Stage {
        INPUT, SAVED_ACCESS_TOKEN, SAVED_CONNECTION, CREDENTIALS, MFA,
        OAUTH1_EXCHANGE, OAUTH2_EXCHANGE
    }

    static final class SignInReport {
        final SignInResult result;
        final FailureKind failure;
        final Stage stage;
        final int httpStatus;
        final String detail;
        final boolean usedMfa;

        private SignInReport(SignInResult result, FailureKind failure, Stage stage,
                             int httpStatus, String detail, boolean usedMfa) {
            this.result = result;
            this.failure = failure;
            this.stage = stage;
            this.httpStatus = httpStatus;
            this.detail = detail == null ? "" : detail;
            this.usedMfa = usedMfa;
        }

        boolean isSuccess() { return result == SignInResult.SUCCESS; }
    }

    private static final class RenewalAttempt {
        final RenewalResult result;
        final FailureKind failure;
        final int httpStatus;
        final String detail;

        RenewalAttempt(RenewalResult result, FailureKind failure, int httpStatus, String detail) {
            this.result = result;
            this.failure = failure;
            this.httpStatus = httpStatus;
            this.detail = detail;
        }
    }

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

    SignInReport signInDetailed(String username, String password, boolean forceCredentials) {
        if (!hasText(username) || !hasText(password)) {
            return report(SignInResult.INVALID, FailureKind.INVALID_CREDENTIALS, Stage.INPUT,
                    -1, "Garmin username and password are required", false);
        }
        if (!forceCredentials && hasValidAccessToken(0)) {
            tokens.scheduleRefresh();
            return report(SignInResult.SUCCESS, FailureKind.NONE, Stage.SAVED_ACCESS_TOKEN,
                    -1, "A saved Garmin access token is still valid", false);
        }
        if (!forceCredentials && hasOAuth1()) {
            RenewalAttempt renewal = renewDetailed(false);
            if (renewal.result == RenewalResult.SUCCESS) {
                return report(SignInResult.SUCCESS, FailureKind.NONE, Stage.SAVED_CONNECTION,
                        renewal.httpStatus, "A saved Garmin connection was renewed", false);
            }
            if (renewal.result == RenewalResult.RETRY) {
                return report(SignInResult.RETRY, renewal.failure, Stage.SAVED_CONNECTION,
                        renewal.httpStatus, renewal.detail, false);
            }
        }

        boolean usedMfa = false;
        Stage currentStage = Stage.CREDENTIALS;
        try {
            http.clearCookies();
            Map<String, String> mobileParameters = mobileParameters();
            Map<String, String> mobileHeaders = mobileHeaders();
            JSONObject login = new JSONObject();
            login.put("username", sanitize(username));
            login.put("password", sanitize(password));
            login.put("rememberMe", true);
            login.put("captchaToken", "");
            GarminHttpClient.Response response = http.executeJson(
                    "POST", MOBILE_LOGIN_URL, mobileParameters, login, mobileHeaders, true);
            traceMobile(Stage.CREDENTIALS, response);
            if (response.code == 400 || response.code == 401
                    || response.code == 403) {
                return responseFailure(Stage.CREDENTIALS, response, true, false);
            }
            if (response.code == 429 || response.code >= 500) {
                return responseFailure(Stage.CREDENTIALS, response, false, false);
            }
            if (response.code != 200) {
                return responseFailure(Stage.CREDENTIALS, response, false, false);
            }

            JSONObject loginResult;
            try {
                loginResult = new JSONObject(response.body);
            } catch (Exception exception) {
                return report(SignInResult.INVALID, FailureKind.PROTOCOL, Stage.CREDENTIALS,
                        response.code, responseDetail(response,
                                "Garmin mobile login returned a non-JSON response"), false);
            }
            String responseType = mobileResponseType(loginResult);
            if ("INVALID_USERNAME_PASSWORD".equals(responseType)) {
                return report(SignInResult.INVALID, FailureKind.INVALID_CREDENTIALS,
                        Stage.CREDENTIALS, response.code,
                        "Garmin rejected the username or password", false);
            }
            if ("CAPTCHA_REQUIRED".equals(responseType)) {
                return report(SignInResult.RETRY, FailureKind.PROTOCOL, Stage.CREDENTIALS,
                        response.code, "Garmin requires a CAPTCHA for this login attempt", false);
            }

            String ticket = "SUCCESSFUL".equals(responseType)
                    ? loginResult.optString("serviceTicketId", "") : "";
            if ("MFA_REQUIRED".equals(responseType)) {
                usedMfa = true;
                log("stage=MFA challenge=required");
                String code = mfaProvider == null ? null : mfaProvider.requestCode();
                if (!hasText(code)) {
                    return report(SignInResult.CANCELLED, FailureKind.CANCELLED, Stage.MFA,
                            response.code, "Garmin verification was cancelled", true);
                }
                JSONObject mfa = new JSONObject();
                JSONObject mfaInfo = loginResult.optJSONObject("customerMfaInfo");
                mfa.put("mfaMethod", mfaInfo == null
                        ? "email" : mfaInfo.optString("mfaLastMethodUsed", "email"));
                mfa.put("mfaVerificationCode", sanitize(code));
                mfa.put("rememberMyBrowser", true);
                mfa.put("reconsentList", new JSONArray());
                mfa.put("mfaSetup", false);
                currentStage = Stage.MFA;
                response = http.executeJson("POST", MOBILE_MFA_URL, mobileParameters, mfa,
                        mobileHeaders, true);
                traceMobile(Stage.MFA, response);
                if (response.code == 400 || response.code == 401 || response.code == 403
                        || response.code == 429 || response.code >= 500) {
                    return responseFailure(Stage.MFA, response, response.code != 429, true);
                }
                JSONObject mfaResult = new JSONObject(response.body);
                String mfaResponseType = mobileResponseType(mfaResult);
                if (!"SUCCESSFUL".equals(mfaResponseType)) {
                    return report(SignInResult.INVALID, FailureKind.INVALID_CREDENTIALS,
                            Stage.MFA, response.code,
                            "Garmin rejected the verification code ("
                                    + valueOr(mfaResponseType, "unknown response") + ")", true);
                }
                ticket = mfaResult.optString("serviceTicketId", "");
            } else if (!"SUCCESSFUL".equals(responseType)) {
                return report(SignInResult.INVALID, FailureKind.PROTOCOL, Stage.CREDENTIALS,
                        response.code, "Garmin mobile login returned "
                                + valueOr(responseType, "an unknown response"), false);
            }
            if (ticket.isEmpty()) {
                return report(SignInResult.INVALID, FailureKind.PROTOCOL,
                        usedMfa ? Stage.MFA : Stage.CREDENTIALS, response.code,
                        responseDetail(response, "Garmin did not return a service ticket"),
                        usedMfa);
            }

            OAuthConsumer consumer = consumer();
            currentStage = Stage.OAUTH1_EXCHANGE;
            GarminHttpClient.Response oauth1 = http.execute("GET",
                    consumer.sign(OAUTH1_URL + "?ticket=" + ticket
                            + "&login-url=" + MOBILE_SERVICE_URL
                            + "&accepts-mfa-tokens=true"), null, null, null, true);
            trace(Stage.OAUTH1_EXCHANGE, oauth1);
            if (oauth1.code != 200) {
                return responseFailure(Stage.OAUTH1_EXCHANGE, oauth1, false, usedMfa);
            }
            Map<String, String> oauth1Values = parseForm(oauth1.body);
            String oauth1Token = oauth1Values.get("oauth_token");
            String oauth1Secret = oauth1Values.get("oauth_token_secret");
            if (!hasText(oauth1Token) || !hasText(oauth1Secret)) {
                return report(SignInResult.INVALID, FailureKind.PROTOCOL, Stage.OAUTH1_EXCHANGE,
                        oauth1.code, "Garmin's OAuth1 response did not contain both tokens", usedMfa);
            }
            tokens.storeOAuth1(oauth1Token, oauth1Secret, oauth1Values.get("mfa_token"),
                    parseMfaExpirationTimestamp(oauth1Values.get("mfa_expiration_timestamp")));

            consumer.setTokenWithSecret(oauth1Token, oauth1Secret);
            RenewalAttempt exchange = exchangeOAuth2Detailed(consumer, false);
            if (exchange.result == RenewalResult.SUCCESS) {
                return report(SignInResult.SUCCESS, FailureKind.NONE, Stage.OAUTH2_EXCHANGE,
                        exchange.httpStatus, usedMfa
                                ? "Garmin login and email/SMS verification succeeded"
                                : "Garmin login succeeded without a verification code", usedMfa);
            }
            return report(exchange.result == RenewalResult.RETRY
                            ? SignInResult.RETRY : SignInResult.INVALID,
                    exchange.failure, Stage.OAUTH2_EXCHANGE, exchange.httpStatus,
                    exchange.detail, usedMfa);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return report(SignInResult.CANCELLED, FailureKind.CANCELLED,
                    currentStage, -1,
                    "Garmin authentication was cancelled", usedMfa);
        } catch (Exception exception) {
            FailureKind failure = exception instanceof IOException
                    ? FailureKind.NETWORK : FailureKind.PROTOCOL;
            return report(SignInResult.RETRY, failure, currentStage, -1,
                    safeExceptionDetail(exception), usedMfa);
        }
    }

    RenewalResult renewInBackground() { return renewDetailed(true).result; }

    String accessToken() {
        if (hasValidAccessToken(60)) return tokens.accessToken();
        return renewDetailed(false).result == RenewalResult.SUCCESS ? tokens.accessToken() : null;
    }

    private RenewalAttempt renewDetailed(boolean tokensOnly) {
        if (!hasOAuth1()) return new RenewalAttempt(RenewalResult.INVALID,
                FailureKind.PROTOCOL, -1, "No saved Garmin renewal credentials are available");
        try {
            OAuthConsumer consumer = consumer();
            consumer.setTokenWithSecret(tokens.oauth1Token(), tokens.oauth1Secret());
            return exchangeOAuth2Detailed(consumer, tokensOnly);
        } catch (Exception exception) {
            return new RenewalAttempt(RenewalResult.RETRY,
                    exception instanceof IOException ? FailureKind.NETWORK : FailureKind.PROTOCOL,
                    -1, safeExceptionDetail(exception));
        }
    }

    private RenewalAttempt exchangeOAuth2Detailed(OAuthConsumer consumer, boolean tokensOnly) {
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
            trace(Stage.OAUTH2_EXCHANGE, response);
            if (response.code == 200) {
                JSONObject json = new JSONObject(response.body);
                String accessToken = json.getString("access_token");
                long expiry = clock.epochSeconds() + json.optLong("expires_in", 3600);
                if (tokens.storeAccess(accessToken, expiry, tokensOnly)) {
                    return new RenewalAttempt(RenewalResult.SUCCESS, FailureKind.NONE,
                            response.code, "Garmin OAuth2 exchange succeeded");
                }
                return new RenewalAttempt(RenewalResult.RETRY, FailureKind.STORAGE,
                        response.code, "Garmin tokens could not be saved");
            }
            FailureKind failure = classify(response, false);
            return new RenewalAttempt(temporary(response.code)
                    ? RenewalResult.RETRY : RenewalResult.INVALID, failure, response.code,
                    responseDetail(response, defaultResponseDetail(Stage.OAUTH2_EXCHANGE,
                            response.code)));
        } catch (Exception exception) {
            return new RenewalAttempt(RenewalResult.RETRY,
                    exception instanceof IOException ? FailureKind.NETWORK : FailureKind.PROTOCOL,
                    -1, safeExceptionDetail(exception));
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

    private static boolean temporary(int code) { return code == 408 || code == 429 || code >= 500; }
    private static String sanitize(String value) {
        return value.trim().replaceAll("[\\n\\r]", "");
    }
    private static boolean hasText(String value) { return value != null && !value.isEmpty(); }
    private static String valueOr(String first, String second) {
        return hasText(first) ? first : second == null ? "" : second;
    }

    private static SignInReport responseFailure(Stage stage, GarminHttpClient.Response response,
                                                boolean credentialsRejected, boolean usedMfa) {
        FailureKind failure = classify(response, credentialsRejected);
        SignInResult result = temporary(response.code) || failure == FailureKind.NETWORK
                || failure == FailureKind.SERVER ? SignInResult.RETRY : SignInResult.INVALID;
        String detail = response.code == 429
                ? rateLimitDetail(response)
                : responseDetail(response, defaultResponseDetail(stage, response.code));
        return report(result, failure, stage, response.code,
                detail, usedMfa);
    }

    private static String rateLimitDetail(GarminHttpClient.Response response) {
        String retryAfter = response.firstHeader("Retry-After");
        if (!hasText(retryAfter)) {
            return "Garmin is temporarily rate-limiting login attempts. Wait several minutes "
                    + "before trying again; correct credentials will also be rejected meanwhile.";
        }
        String value = retryAfter.trim();
        if (value.matches("\\d+")) {
            return "Garmin is temporarily rate-limiting login attempts. Retry after "
                    + value + " seconds.";
        }
        return "Garmin is temporarily rate-limiting login attempts. Retry after "
                + sanitizeDetail(value) + ".";
    }

    private static FailureKind classify(GarminHttpClient.Response response,
                                        boolean credentialsRejected) {
        if (response.code == 429) return FailureKind.RATE_LIMITED;
        if (response.code == 408 || response.code >= 500) return FailureKind.SERVER;
        if (response.code == 403 && looksLikeBotChallenge(response.body)) {
            return FailureKind.PROTOCOL;
        }
        if (credentialsRejected && (response.code == 400 || response.code == 401
                || response.code == 403)) return FailureKind.INVALID_CREDENTIALS;
        return FailureKind.PROTOCOL;
    }

    private static String defaultResponseDetail(Stage stage, int code) {
        if (code == 429) return "Garmin rate-limited the authentication request";
        if (code == 408) return "Garmin timed out while processing the request";
        if (code >= 500) return "Garmin's authentication service returned a server error";
        if (code == 401) return "Garmin rejected the supplied authentication data";
        if (code == 403) return "Garmin refused the authentication request";
        return "Garmin returned an unexpected response during " + stage.name();
    }

    private static String responseDetail(GarminHttpClient.Response response, String fallback) {
        if (looksLikeBotChallenge(response.body)) {
            return "Garmin or its bot-protection service blocked the request";
        }
        try {
            JSONObject json = new JSONObject(response.body);
            String[] keys = {"error_description", "error", "message", "detail"};
            for (String key : keys) {
                String value = json.optString(key, "").trim();
                if (!value.isEmpty()) return sanitizeDetail(value);
            }
        } catch (Exception ignored) {
            // The response is commonly HTML rather than JSON.
        }
        String pageMessage = firstHtmlMessage(response.body);
        if (!pageMessage.isEmpty()) return pageMessage;
        Matcher title = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(response.body);
        if (title.find()) {
            String value = title.group(1).replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ").trim();
            String lower = value.toLowerCase(Locale.US);
            if (lower.contains("error") || lower.contains("denied")
                    || lower.contains("forbidden") || lower.contains("unavailable")) {
                return sanitizeDetail(value);
            }
        }
        return fallback;
    }

    private static boolean looksLikeBotChallenge(String body) {
        if (body == null) return false;
        String lower = body.toLowerCase(Locale.US);
        boolean challengeTitle = Pattern.compile(
                "(?is)<title[^>]*>\\s*just a moment(?:\\.{3})?\\s*</title>")
                .matcher(body).find();
        boolean hasLoginForm = Pattern.compile(
                "(?is)<input[^>]*\\sname=[\"'](?:username|password)[\"']")
                .matcher(body).find();
        boolean challengeAssets = lower.contains("cf-chl-")
                || lower.contains("/challenge-platform/")
                || lower.contains("cloudflare ray id");
        // Garmin's normal sign-in page loads Cloudflare challenge support pre-emptively. It is
        // only a challenge when the challenge page replaces the real username/password form.
        return challengeTitle || (challengeAssets && !hasLoginForm);
    }

    private static String safeExceptionDetail(Exception exception) {
        String type = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) return type;
        return type + ": " + sanitizeDetail(message);
    }

    private static String sanitizeDetail(String value) {
        String sanitized = value.replaceAll(
                "(?i)(https?://[^\\s?]+)\\?[^\\s]+", "$1?<query-redacted>");
        sanitized = sanitized.replaceAll(
                "(?i)(password|ticket|token|secret|code|oauth_[a-z_]+)=([^&\\s]+)",
                "$1=<redacted>");
        sanitized = sanitized.replaceAll(
                "(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9.-]+\\.[a-z]{2,}",
                "<email-redacted>");
        return sanitized.length() > 240 ? sanitized.substring(0, 240) + "…" : sanitized;
    }

    private static void trace(Stage stage, GarminHttpClient.Response response) {
        log("stage=" + stage.name() + " http=" + response.code
                + " endpoint=" + safeEndpoint(response.url)
                + " responseBytes=" + response.body.length());
    }

    private static void traceMobile(Stage stage, GarminHttpClient.Response response) {
        trace(stage, response);
        try {
            String type = mobileResponseType(new JSONObject(response.body));
            if (!type.isEmpty()) log("stage=" + stage.name() + " responseType=" + type);
        } catch (Exception ignored) {
            // responseDetail reports non-JSON failures without logging response content.
        }
    }

    private static String mobileResponseType(JSONObject response) {
        JSONObject status = response.optJSONObject("responseStatus");
        return status == null ? "" : status.optString("type", "")
                .replaceAll("[^A-Za-z0-9_-]", "");
    }

    private static String firstHtmlMessage(String body) {
        Matcher matcher = messageElementMatcher(body);
        while (matcher.find()) {
            String value = cleanHtmlText(matcher.group(1));
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static Matcher messageElementMatcher(String body) {
        Pattern element = Pattern.compile(
                "(?is)<(?:div|span|p|li)[^>]*(?:id|class)=[\"'][^\"']*"
                        + "(?:error|alert|message|validation|notification)[^\"']*[\"'][^>]*>"
                        + "(.*?)</(?:div|span|p|li)>");
        return element.matcher(body == null ? "" : body);
    }

    private static String cleanHtmlText(String value) {
        if (value == null) return "";
        return sanitizeDetail(value.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ").trim());
    }

    private static String safeEndpoint(String value) {
        if (value == null || value.isEmpty()) return "unknown";
        try {
            URI uri = URI.create(value);
            return uri.getHost() + (uri.getPath() == null ? "" : uri.getPath());
        } catch (Exception ignored) {
            return "unparseable";
        }
    }

    private static SignInReport report(SignInResult result, FailureKind failure, Stage stage,
                                       int httpStatus, String detail, boolean usedMfa) {
        SignInReport report = new SignInReport(result, failure, stage, httpStatus,
                sanitizeDetail(detail), usedMfa);
        log("result=" + result.name() + " failure=" + failure.name()
                + " stage=" + stage.name() + " http=" + httpStatus
                + " mfa=" + usedMfa + " detail=" + report.detail);
        return report;
    }

    private static void log(String message) {
        try {
            Log.i(TAG, message);
        } catch (RuntimeException ignored) {
            // android.util.Log is unavailable in local JVM unit tests.
        }
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

    private static Map<String, String> mobileParameters() {
        Map<String, String> values = new HashMap<>();
        values.put("clientId", MOBILE_CLIENT_ID);
        values.put("locale", "en-US");
        values.put("service", MOBILE_SERVICE_URL);
        return values;
    }

    private static Map<String, String> mobileHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", MOBILE_USER_AGENT);
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");
        headers.put("Origin", "https://sso.garmin.com");
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
