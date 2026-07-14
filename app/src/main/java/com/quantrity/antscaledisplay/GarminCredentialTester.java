package com.quantrity.antscaledisplay;

/** Tests freshly entered Garmin credentials without reading or writing the app's saved users. */
final class GarminCredentialTester {
    static final class Attempt {
        final GarminAuthenticator.SignInReport report;
        final VerifiedTokens tokens;

        Attempt(GarminAuthenticator.SignInReport report, VerifiedTokens tokens) {
            this.report = report;
            this.tokens = report.isSuccess() ? tokens : null;
        }
    }

    static final class VerifiedTokens {
        private String oauth1Token;
        private String oauth1Secret;
        private String mfaToken;
        private long mfaExpiry = -1;
        private String accessToken;
        private long accessExpiry = -1;

        void applyTo(User user) {
            user.garminOauth1Token = oauth1Token;
            user.garminOauth1TokenSecret = oauth1Secret;
            user.garminOauth1MfaToken = mfaToken;
            user.garminOauth1MfaExpirationTimestamp = mfaExpiry;
            user.garminOauth2Token = accessToken;
            user.garminOauth2ExpiryTimestamp = accessExpiry;
        }
    }

    private final GarminAuthenticator authenticator;
    private final VerifiedTokens tokens;

    GarminCredentialTester(GarminHttpClient http, MfaCodeProvider mfaProvider) {
        this(http, mfaProvider, () -> System.currentTimeMillis() / 1000);
    }

    GarminCredentialTester(GarminHttpClient http, MfaCodeProvider mfaProvider,
                           GarminAuthenticator.Clock clock) {
        tokens = new VerifiedTokens();
        authenticator = new GarminAuthenticator(
                http, new MemoryTokenStore(tokens), mfaProvider, clock);
    }

    Attempt test(String username, String password) {
        return new Attempt(authenticator.signInDetailed(username, password, true), tokens);
    }

    private static final class MemoryTokenStore implements GarminAuthenticator.TokenStore {
        private final VerifiedTokens tokens;

        MemoryTokenStore(VerifiedTokens tokens) { this.tokens = tokens; }

        @Override public String accessToken() { return null; }
        @Override public long accessExpiry() { return -1; }
        @Override public String oauth1Token() { return null; }
        @Override public String oauth1Secret() { return null; }
        @Override public String mfaToken() { return tokens.mfaToken; }
        @Override public void storeOAuth1(String token, String secret, String mfa, long expiry) {
            tokens.oauth1Token = token;
            tokens.oauth1Secret = secret;
            tokens.mfaToken = mfa;
            tokens.mfaExpiry = expiry;
        }
        @Override public boolean storeAccess(String token, long expiry, boolean tokensOnly) {
            tokens.accessToken = token;
            tokens.accessExpiry = expiry;
            return true;
        }
        @Override public void scheduleRefresh() {}
    }
}
