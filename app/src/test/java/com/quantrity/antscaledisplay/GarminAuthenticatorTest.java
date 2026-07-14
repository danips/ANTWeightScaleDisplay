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
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":3600}"));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInResult result = authenticator(transport, tokens, () -> null)
                .signIn(" user@example.com\n", "password\r");

        assertEquals(GarminAuthenticator.SignInResult.SUCCESS, result);
        assertEquals("oauth-one", tokens.oauth1Token);
        assertEquals("oauth-secret", tokens.oauth1Secret);
        assertEquals("access", tokens.accessToken);
        assertEquals(NOW + 3600, tokens.accessExpiry);
        assertEquals(3, transport.requests.size());
        assertTrue(body(transport.requests.get(0)).contains("\"username\":\"user@example.com\""));
        assertFalse(body(transport.requests.get(0)).contains("\\n"));
        assertTrue(transport.requests.get(0).url.contains("/mobile/api/login"));
    }

    @Test
    public void mfaLoginUsesInjectedCodeProvider() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()),
                response(200, mobileSuccess("mfa-ticket")),
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":60}"));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInResult result = authenticator(
                transport, tokens, () -> "123456").signIn("user", "password");

        assertEquals(GarminAuthenticator.SignInResult.SUCCESS, result);
        assertTrue(body(transport.requests.get(1)).contains(
                "\"mfaVerificationCode\":\"123456\""));
        assertTrue(transport.requests.get(1).url.contains("/mobile/api/mfa/verifyCode"));
    }

    @Test
    public void cancellingMfaStopsAuthentication() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()));

        GarminAuthenticator.SignInResult result = authenticator(
                transport, new FakeTokenStore(), () -> "").signIn("user", "password");

        assertEquals(GarminAuthenticator.SignInResult.CANCELLED, result);
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void invalidCredentialsAreRejected() {
        FakeTransport transport = new FakeTransport(response(200, mobileInvalid()));

        assertEquals(GarminAuthenticator.SignInResult.INVALID,
                authenticator(transport, new FakeTokenStore(), () -> null)
                        .signIn("user", "bad-password"));
    }

    @Test
    public void temporaryLoginFailureCanBeRetried() {
        FakeTransport transport = new FakeTransport(response(503, "unavailable"));

        assertEquals(GarminAuthenticator.SignInResult.RETRY,
                authenticator(transport, new FakeTokenStore(), () -> null)
                        .signIn("user", "password"));
    }

    @Test
    public void directLoginReportIdentifiesCompletedStageWithoutMfa() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":3600}"));

        GarminAuthenticator.SignInReport report = authenticator(
                transport, new FakeTokenStore(), () -> null)
                .signInDetailed("user", "password", true);

        assertTrue(report.isSuccess());
        assertEquals(GarminAuthenticator.Stage.OAUTH2_EXCHANGE, report.stage);
        assertEquals(200, report.httpStatus);
        assertFalse(report.usedMfa);
    }

    @Test
    public void mfaLoginReportRecordsThatVerificationWasUsed() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileMfaRequired()),
                response(200, mobileSuccess("mfa-ticket")),
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":60}"));

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
                throw new IOException("Failed https://connectapi.garmin.com/oauth?"
                        + "ticket=secret-ticket&oauth_signature=secret-signature");
            }
        };

        GarminAuthenticator.SignInReport report = new GarminAuthenticator(
                new GarminHttpClient(transport), new FakeTokenStore(), () -> null, () -> NOW)
                .signInDetailed("user", "password", true);

        assertEquals(GarminAuthenticator.FailureKind.NETWORK, report.failure);
        assertEquals(GarminAuthenticator.Stage.OAUTH1_EXCHANGE, report.stage);
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
    public void credentialTesterCapturesVerifiedTokensForLaterProfileSave() {
        FakeTransport transport = new FakeTransport(
                response(200, mobileSuccess("service-ticket")),
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":3600}"));
        GarminCredentialTester tester = new GarminCredentialTester(
                new GarminHttpClient(transport), () -> null, () -> NOW);

        GarminCredentialTester.Attempt attempt = tester.test("user", "password");
        User user = new User();
        assertNotNull(attempt.tokens);
        attempt.tokens.applyTo(user);

        assertEquals("oauth-one", user.garminOauth1Token);
        assertEquals("oauth-secret", user.garminOauth1TokenSecret);
        assertEquals("access", user.garminOauth2Token);
        assertEquals(NOW + 3600, user.garminOauth2ExpiryTimestamp);
    }

    @Test
    public void backgroundRenewalUsesSavedOAuthCredentials() {
        FakeTransport transport = new FakeTransport(
                response(200, "{\"access_token\":\"renewed\",\"expires_in\":7200}"));
        FakeTokenStore tokens = renewalTokens();

        GarminAuthenticator.RenewalResult result = authenticator(
                transport, tokens, () -> null).renewInBackground();

        assertEquals(GarminAuthenticator.RenewalResult.SUCCESS, result);
        assertEquals("renewed", tokens.accessToken);
        assertTrue(tokens.lastSaveWasTokensOnly);
        assertTrue(transport.requests.get(0).headers.containsKey("Authorization"));
    }

    @Test
    public void rejectedRenewalCredentialsAreInvalid() {
        FakeTransport transport = new FakeTransport(response(401, "rejected"));

        assertEquals(GarminAuthenticator.RenewalResult.INVALID,
                authenticator(transport, renewalTokens(), () -> null).renewInBackground());
    }

    @Test
    public void temporaryRenewalFailureCanBeRetried() {
        FakeTransport transport = new FakeTransport(response(503, "unavailable"));

        assertEquals(GarminAuthenticator.RenewalResult.RETRY,
                authenticator(transport, renewalTokens(), () -> null).renewInBackground());
    }

    private static GarminAuthenticator authenticator(FakeTransport transport,
                                                       FakeTokenStore tokens,
                                                       MfaCodeProvider mfa) {
        return new GarminAuthenticator(new GarminHttpClient(transport), tokens, mfa, () -> NOW);
    }

    private static FakeTokenStore renewalTokens() {
        FakeTokenStore tokens = new FakeTokenStore();
        tokens.oauth1Token = "oauth-one";
        tokens.oauth1Secret = "oauth-secret";
        tokens.mfaToken = "mfa-token";
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

    private static String oauth1Response() {
        return "oauth_token=oauth-one&oauth_token_secret=oauth-secret"
                + "&mfa_token=mfa-token&mfa_expiration_timestamp=1767225600000";
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
        String mfaToken;
        boolean lastSaveWasTokensOnly;
        boolean refreshScheduled;

        @Override public String accessToken() { return accessToken; }
        @Override public long accessExpiry() { return accessExpiry; }
        @Override public String oauth1Token() { return oauth1Token; }
        @Override public String oauth1Secret() { return oauth1Secret; }
        @Override public String mfaToken() { return mfaToken; }
        @Override public void storeOAuth1(String token, String secret, String mfa, long expiry) {
            oauth1Token = token;
            oauth1Secret = secret;
            mfaToken = mfa;
        }
        @Override public boolean storeAccess(String token, long expiry, boolean tokensOnly) {
            accessToken = token;
            accessExpiry = expiry;
            lastSaveWasTokensOnly = tokensOnly;
            return true;
        }
        @Override public void scheduleRefresh() { refreshScheduled = true; }
    }
}
