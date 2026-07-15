package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GarminAuthenticatorTest {
    private static final long NOW = 1_000;

    @Test
    public void successfulLoginExchangesAndStoresTokens() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, diResponse("access", "refresh", 3600)));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInReport report = authenticator(transport, tokens, () -> null)
                .signInDetailed(" user@example.com\n", "password\r", false);

        assertEquals(GarminAuthenticator.SignInResult.SUCCESS, report.result);
        assertEquals("access", tokens.accessToken);
        assertEquals("refresh", tokens.diRefreshToken);
        assertEquals("GARMIN_CONNECT_MOBILE_ANDROID_DI_2025Q2", tokens.diClientId);
        assertEquals(NOW + 3600, tokens.accessExpiry);
        assertEquals(2, transport.requests.size());
        assertTrue(body(transport.requests.get(0)).contains("\"username\":\"user@example.com\""));
        assertFalse(body(transport.requests.get(0)).contains("\\n"));
        assertTrue(transport.requests.get(0).url.contains("/mobile/api/login"));
        assertTrue(transport.requests.get(1).url.contains("diauth.garmin.com"));
        assertEquals("Basic "
                        + "R0FSTUlOX0NPTk5FQ1RfTU9CSUxFX0FORFJPSURfRElfMjAyNVEyOg==",
                transport.requests.get(1).headers.get("Authorization"));
        assertTrue(body(transport.requests.get(1)).contains("service_ticket=service-ticket"));
    }

    @Test
    public void diExchangeFallsBackToAnAcceptedClientId() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(400, "{\"error\":\"invalid_client\"}"),
                response(200, diResponse("access", "refresh", 3600)));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInReport report = authenticator(
                transport, tokens, () -> null).signInDetailed("user", "password", true);

        assertTrue(report.isSuccess());
        assertEquals("GARMIN_CONNECT_MOBILE_ANDROID_DI_2024Q4", tokens.diClientId);
        assertEquals(3, transport.requests.size());
    }

    @Test
    public void diRateLimitFailsFastWithoutTryingMoreClientIds() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(429, "too many requests"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.RATE_LIMITED, report.failure);
        assertEquals(GarminAuthenticator.Stage.DI_EXCHANGE, report.stage);
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void mfaLoginUsesInjectedCodeProvider() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()),
                response(200, mobileSuccess("mfa-ticket")),
                response(200, diResponse("access", "refresh", 60)));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInReport report = authenticator(
                transport, tokens, () -> "123456")
                .signInDetailed("user", "password", false);

        assertEquals(GarminAuthenticator.SignInResult.SUCCESS, report.result);
        assertTrue(body(transport.requests.get(1)).contains(
                "\"mfaVerificationCode\":\"123456\""));
        assertTrue(transport.requests.get(1).url.contains("/mobile/api/mfa/verifyCode"));
    }

    @Test
    public void cancellingMfaStopsAuthentication() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> "")
                .signInDetailed("user", "password", false);

        assertEquals(GarminAuthenticator.SignInResult.CANCELLED, report.result);
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void temporaryLoginFailureCanBeRetried() {
        FakeTransport transport = new FakeTransport(response(503, "unavailable"));

        assertEquals(GarminAuthenticator.SignInResult.RETRY,
                authenticator(transport, new FakeTokenStore(), () -> null)
                        .signInDetailed("user", "password", false).result);
    }

    @Test
    public void directLoginReportIdentifiesCompletedStageWithoutMfa() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, diResponse("access", "refresh", 3600)));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertTrue(report.isSuccess());
        assertEquals(GarminAuthenticator.Stage.DI_EXCHANGE, report.stage);
        assertEquals(200, report.httpStatus);
        assertFalse(report.usedMfa);
    }

    @Test
    public void mfaLoginReportRecordsThatVerificationWasUsed() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()),
                response(200, mobileSuccess("mfa-ticket")),
                response(200, diResponse("access", "refresh", 60)));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> "123456")
                .signInDetailed("user", "password", true);

        assertTrue(report.isSuccess());
        assertTrue(report.usedMfa);
        assertTrue(report.detail.contains("verification succeeded"));
    }

    @Test
    public void invalidCredentialReportIncludesStageAndHttpStatus() {
        FakeTransport transport = new FakeTransport(response(200, mobileInvalid()));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "bad-password", true);

        assertEquals(GarminAuthenticator.SignInResult.INVALID, report.result);
        assertEquals(GarminAuthenticator.FailureKind.INVALID_CREDENTIALS, report.failure);
        assertEquals(GarminAuthenticator.Stage.CREDENTIALS, report.stage);
        assertEquals(200, report.httpStatus);
    }

    @Test
    public void ordinaryCloudflareAndCaptchaReferencesAreNotABotChallenge() {
        String loginPage = "<title>Garmin Sign In</title>"
                + "<script src=\"/challenge-platform/scripts/jsd/main.js\"></script>"
                + "<input name=\"username\"><input name=\"password\">"
                + "<div class=\"captcha-container\">Invalid credentials</div>";
        FakeTransport transport = new FakeTransport(response(401, loginPage));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "bad-password", true);

        assertEquals(GarminAuthenticator.FailureKind.INVALID_CREDENTIALS, report.failure);
        assertEquals("Garmin rejected the supplied authentication data", report.detail);
    }

    @Test
    public void cloudflareAssetsWithoutLoginFormAreABotChallenge() {
        FakeTransport transport = new FakeTransport(
                response(403, "<script src=\"/challenge-platform/h/g/orchestrate\"></script>"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.PROTOCOL, report.failure);
        assertTrue(report.detail.contains("bot-protection"));
    }

    @Test
    public void diagnosticDetailsRedactEmailAddresses() {
        FakeTransport transport = new FakeTransport(
                response(500, "{\"message\":\"Account user@example.com was rejected\"}"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertFalse(report.detail.contains("user@example.com"));
        assertTrue(report.detail.contains("<email-redacted>"));
    }

    @Test
    public void repeatedLoginPageReportsGarminValidationMessage() {
        FakeTransport transport = new FakeTransport(
                response(200, "<title>Garmin Sign In</title>"
                        + "<div class=\"login-error-message\">"
                        + "Additional verification is unavailable for user@example.com"
                        + "</div><input name=\"username\"><input name=\"password\">"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.PROTOCOL, report.failure);
        assertEquals("Additional verification is unavailable for <email-redacted>",
                report.detail);
    }

    @Test
    public void genericLegacyGarminPageDoesNotTriggerVerificationPopup() {
        int[] promptCount = {0};
        FakeTransport transport = new FakeTransport(
                response(200, "<title>GARMIN Authentication Application</title>"
                        + "<input name=\"username\"><input name=\"password\">"
                        + "<input name=\"_csrf\" value=\"mfa-csrf\">"
                        + "<div class=\"error-message\">An unexpected error has occurred.</div>"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> {
                    promptCount[0]++;
                    return "123456";
                })
                .signInDetailed("user", "password", true);

        assertFalse(report.isSuccess());
        assertFalse(report.usedMfa);
        assertEquals(0, promptCount[0]);
        assertEquals("An unexpected error has occurred.", report.detail);
    }

    @Test
    public void rateLimitIsNotReportedAsBadPassword() {
        FakeTransport transport = new FakeTransport(response(429, "too many requests"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.SignInResult.RETRY, report.result);
        assertEquals(GarminAuthenticator.FailureKind.RATE_LIMITED, report.failure);
        assertEquals(429, report.httpStatus);
        assertTrue(report.detail.contains("Wait several minutes"));
        assertFalse(report.detail.contains("request-id"));
    }

    @Test
    public void rateLimitReportUsesRetryAfterHeaderWhenAvailable() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Retry-After", Collections.singletonList("120"));
        FakeTransport transport = new FakeTransport(new GarminHttpClient.Response(
                429, "{\"status-code\":\"429\"}", "https://response", headers));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.RATE_LIMITED, report.failure);
        assertEquals("Garmin is temporarily rate-limiting login attempts. "
                + "Retry after 120 seconds.", report.detail);
    }

    @Test
    public void botChallengeIsReportedAsProtocolFailure() {
        FakeTransport transport = new FakeTransport(
                response(403, "<title>Just a moment...</title> Cloudflare cf-chl-test"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.PROTOCOL, report.failure);
        assertTrue(report.detail.contains("bot-protection"));
    }

    @Test
    public void diagnosticDetailsRedactSensitiveValues() {
        FakeTransport transport = new FakeTransport(
                response(500, "{\"message\":\"token=secret-value password=hunter2\"}"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertFalse(report.detail.contains("secret-value"));
        assertFalse(report.detail.contains("hunter2"));
        assertTrue(report.detail.contains("<redacted>"));
    }

    @Test
    public void networkFailureReportsExactStageAndRedactsUrlQuery() {
        GarminHttpClient.Transport transport = new GarminHttpClient.Transport() {
            int requestCount;

            @Override
            public GarminHttpClient.Response execute(GarminHttpClient.Request request)
                    throws Exception {
                requestCount++;
                if (requestCount == 1) return response(200, mobileSuccess("service-ticket"));
                throw new IOException("Failed https://diauth.garmin.com/token?"
                        + "ticket=secret-ticket&oauth_signature=secret-signature");
            }
        };

        GarminAuthenticator.SignInReport report = new GarminAuthenticator(
                new GarminHttpClient(transport), new FakeTokenStore(), () -> null, () -> NOW)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.NETWORK, report.failure);
        assertEquals(GarminAuthenticator.Stage.DI_EXCHANGE, report.stage);
        assertFalse(report.detail.contains("secret-ticket"));
        assertFalse(report.detail.contains("secret-signature"));
        assertTrue(report.detail.contains("query-redacted"));
    }

    @Test
    public void forcedCredentialTestDoesNotAcceptCachedAccessToken() {
        FakeTokenStore tokens = new FakeTokenStore();
        tokens.accessToken = "cached";
        tokens.accessExpiry = NOW + 3600;
        FakeTransport transport = new FakeTransport(response(401, "rejected"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, tokens, () -> null).signInDetailed("user", "password", true);

        assertFalse(report.isSuccess());
        assertEquals(1, transport.requests.size());
        assertFalse(tokens.refreshScheduled);
    }

    @Test
    public void savedAccessTokenIsReusedWithoutNetworkOrMfa() {
        FakeTokenStore tokens = new FakeTokenStore();
        tokens.accessToken = "cached";
        tokens.accessExpiry = NOW + 3600;
        FakeTransport transport = new FakeTransport();
        int[] promptCount = {0};

        GarminAuthenticator.SignInReport report = authenticator(transport, tokens, () -> {
            promptCount[0]++;
            return "123456";
        }).signInDetailed("user", "password", false);

        assertTrue(report.isSuccess());
        assertEquals(GarminAuthenticator.Stage.SAVED_ACCESS_TOKEN, report.stage);
        assertEquals(0, transport.requests.size());
        assertEquals(0, promptCount[0]);
        assertTrue(tokens.refreshScheduled);
    }

    @Test
    public void legacySessionIsDiscardedBeforeValidAccessAndFreshLoginIsForced() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, diResponse("access", "refresh", 3600)));
        FakeTokenStore tokens = legacyTokens();
        tokens.accessToken = "legacy-access";
        tokens.accessExpiry = NOW + 3600;

        GarminAuthenticator.SignInReport report = authenticator(
                transport, tokens, () -> null)
                .signInDetailed("user", "password", false);

        assertTrue(report.isSuccess());
        assertTrue(tokens.legacySessionDiscarded);
        assertEquals(null, tokens.oauth1Token);
        assertEquals(null, tokens.oauth1Secret);
        assertEquals("access", tokens.accessToken);
        assertEquals("refresh", tokens.diRefreshToken);
        assertEquals(2, transport.requests.size());
        assertTrue(transport.requests.get(0).url.contains("/mobile/api/login"));
    }

    @Test
    public void legacySessionDiscardFailureStopsBeforeFreshLogin() {
        FakeTokenStore tokens = legacyTokens();
        tokens.discardLegacySucceeds = false;
        FakeTransport transport = new FakeTransport();

        GarminAuthenticator.SignInReport report = authenticator(
                transport, tokens, () -> null)
                .signInDetailed("user", "password", false);

        assertEquals(GarminAuthenticator.SignInResult.RETRY, report.result);
        assertEquals(GarminAuthenticator.FailureKind.STORAGE, report.failure);
        assertEquals(GarminAuthenticator.Stage.SAVED_CONNECTION, report.stage);
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void credentialTesterCapturesVerifiedTokensForLaterProfileSave() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, diResponse("access", "refresh", 3600)));
        GarminCredentialTester tester = new GarminCredentialTester(
                new GarminHttpClient(transport), () -> null, () -> NOW);

        GarminCredentialTester.Attempt attempt = tester.test("user", "password");
        User user = new User();
        assertNotNull(attempt.tokens);
        attempt.tokens.applyTo(user);

        assertEquals("access", user.garminOauth2Token);
        assertEquals(NOW + 3600, user.garminOauth2ExpiryTimestamp);
        assertEquals("refresh", user.garminDiRefreshToken);
        assertEquals("GARMIN_CONNECT_MOBILE_ANDROID_DI_2025Q2", user.garminDiClientId);
    }

    @Test
    public void backgroundRenewalPrefersSavedDiRefreshToken() {
        FakeTransport transport = new FakeTransport(
                response(200, diResponse("renewed", "rotated", 7200)));
        FakeTokenStore tokens = diRenewalTokens();

        GarminAuthenticator.RenewalResult result = authenticator(
                transport, tokens, () -> null).renewInBackground();

        assertEquals(GarminAuthenticator.RenewalResult.SUCCESS, result);
        assertEquals("renewed", tokens.accessToken);
        assertEquals("rotated", tokens.diRefreshToken);
        assertTrue(tokens.lastSaveWasTokensOnly);
        assertTrue(body(transport.requests.get(0)).contains("grant_type=refresh_token"));
    }

    @Test
    public void backgroundRenewalRejectsLegacyOAuthCredentials() {
        FakeTransport transport = new FakeTransport();
        FakeTokenStore tokens = legacyTokens();

        GarminAuthenticator.RenewalResult result = authenticator(
                transport, tokens, () -> null).renewInBackground();

        assertEquals(GarminAuthenticator.RenewalResult.INVALID, result);
        assertEquals(0, transport.requests.size());
    }

    private static GarminAuthenticator authenticator(FakeTransport transport,
                                                       FakeTokenStore tokens,
                                                       MfaCodeProvider mfa) {
        return new GarminAuthenticator(new GarminHttpClient(transport), tokens, mfa, () -> NOW);
    }

    private static FakeTokenStore legacyTokens() {
        FakeTokenStore tokens = new FakeTokenStore();
        tokens.oauth1Token = "oauth-one";
        tokens.oauth1Secret = "oauth-secret";
        return tokens;
    }

    private static FakeTokenStore diRenewalTokens() {
        FakeTokenStore tokens = new FakeTokenStore();
        tokens.diRefreshToken = "di-refresh";
        tokens.diClientId = "GARMIN_CONNECT_MOBILE_ANDROID_DI_2025Q2";
        return tokens;
    }

    private static GarminHttpClient.Response response(int code, String body) {
        return new GarminHttpClient.Response(code, body, "https://response", null);
    }

    private static String mobileSuccess(String ticket) {
        return "{\"responseStatus\":{\"type\":\"SUCCESSFUL\"},"
                + "\"serviceTicketId\":\"" + ticket + "\"}";
    }

    private static String mobileMfaRequired() {
        return "{\"responseStatus\":{\"type\":\"MFA_REQUIRED\"},"
                + "\"customerMfaInfo\":{\"mfaLastMethodUsed\":\"email\"}}";
    }

    private static String mobileInvalid() {
        return "{\"responseStatus\":{\"type\":\"INVALID_USERNAME_PASSWORD\"}}";
    }

    private static String diResponse(String access, String refresh, long expiresIn) {
        return "{\"access_token\":\"" + access + "\",\"refresh_token\":\""
                + refresh + "\",\"expires_in\":" + expiresIn + "}";
    }

    private static String body(GarminHttpClient.Request request) {
        return request.body == null ? "" : new String(request.body, StandardCharsets.UTF_8);
    }

    private static final class FakeTransport implements GarminHttpClient.Transport {
        final Deque<GarminHttpClient.Response> responses = new ArrayDeque<>();
        final List<GarminHttpClient.Request> requests = new ArrayList<>();

        FakeTransport(GarminHttpClient.Response... responses) {
            Collections.addAll(this.responses, responses);
        }

        @Override
        public GarminHttpClient.Response execute(GarminHttpClient.Request request) {
            requests.add(request);
            if (responses.isEmpty()) throw new AssertionError("Unexpected request " + request.url);
            return responses.removeFirst();
        }
    }

    private static final class FakeTokenStore implements GarminAuthenticator.TokenStore {
        String accessToken;
        long accessExpiry = -1;
        String oauth1Token;
        String oauth1Secret;
        String diRefreshToken;
        String diClientId;
        boolean lastSaveWasTokensOnly;
        boolean refreshScheduled;
        boolean legacySessionDiscarded;
        boolean discardLegacySucceeds = true;

        @Override public String accessToken() { return accessToken; }
        @Override public long accessExpiry() { return accessExpiry; }
        @Override public String oauth1Token() { return oauth1Token; }
        @Override public String oauth1Secret() { return oauth1Secret; }
        @Override public String diRefreshToken() { return diRefreshToken; }
        @Override public String diClientId() { return diClientId; }
        @Override public boolean discardLegacySession() {
            if (!discardLegacySucceeds) return false;
            legacySessionDiscarded = true;
            oauth1Token = null;
            oauth1Secret = null;
            accessToken = null;
            accessExpiry = -1;
            return true;
        }
        @Override public boolean storeDi(String access, long expiry, String refresh,
                                         String clientId, boolean tokensOnly) {
            accessToken = access;
            accessExpiry = expiry;
            diRefreshToken = refresh;
            diClientId = clientId;
            oauth1Token = null;
            oauth1Secret = null;
            lastSaveWasTokensOnly = tokensOnly;
            return true;
        }
        @Override public void scheduleRefresh() { refreshScheduled = true; }
    }
}
