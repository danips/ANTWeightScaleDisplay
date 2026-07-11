package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
                response(200, csrfPage()),
                response(200, "ticket=service-ticket"),
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
        assertEquals(4, transport.requests.size());
        assertTrue(body(transport.requests.get(1)).contains("username=user%40example.com"));
        assertFalse(body(transport.requests.get(1)).contains("%0A"));
    }

    @Test
    public void mfaLoginUsesInjectedCodeProvider() {
        Map<String, List<String>> redirect = new HashMap<>();
        redirect.put("Location", Collections.singletonList(
                "https://sso.garmin.com/sso/embed?ticket=mfa-ticket"));
        FakeTransport transport = new FakeTransport(
                response(200, csrfPage()),
                response(200, "<input name=\"_csrf\" value=\"mfa-csrf\">mfa-code"),
                new GarminHttpClient.Response(302, "", "mfa", redirect),
                response(200, oauth1Response()),
                response(200, "{\"access_token\":\"access\",\"expires_in\":60}"));
        FakeTokenStore tokens = new FakeTokenStore();

        GarminAuthenticator.SignInResult result = authenticator(
                transport, tokens, () -> "123456").signIn("user", "password");

        assertEquals(GarminAuthenticator.SignInResult.SUCCESS, result);
        assertTrue(body(transport.requests.get(2)).contains("mfa-code=123456"));
        assertTrue(transport.requests.get(2).url.contains("loginEnterMfaCode"));
    }

    @Test
    public void cancellingMfaStopsAuthentication() {
        FakeTransport transport = new FakeTransport(
                response(200, csrfPage()),
                response(200, "MFA mfa-code"));

        GarminAuthenticator.SignInResult result = authenticator(
                transport, new FakeTokenStore(), () -> "").signIn("user", "password");

        assertEquals(GarminAuthenticator.SignInResult.CANCELLED, result);
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void invalidCredentialsAreRejected() {
        FakeTransport transport = new FakeTransport(
                response(200, csrfPage()), response(401, "invalid"));

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

    private static String csrfPage() {
        return "<input name=\"_csrf\" value=\"csrf-token\">";
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
