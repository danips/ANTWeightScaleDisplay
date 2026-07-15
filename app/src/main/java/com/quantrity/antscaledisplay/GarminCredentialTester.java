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
        private String accessToken;
        private long accessExpiry = -1;
        private String diRefreshToken;
        private String diClientId;

        void applyTo(User user) {
            user.garminOauth1Token = null;
            user.garminOauth1TokenSecret = null;
            user.garminOauth1MfaToken = null;
            user.garminOauth1MfaExpirationTimestamp = -1;
            user.garminOauth2Token = accessToken;
            user.garminOauth2ExpiryTimestamp = accessExpiry;
            user.garminDiRefreshToken = diRefreshToken;
            user.garminDiClientId = diClientId;
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
        @Override public String diRefreshToken() { return tokens.diRefreshToken; }
        @Override public String diClientId() { return tokens.diClientId; }
        @Override public boolean discardLegacySession() { return true; }
        @Override public boolean storeDi(String accessToken, long accessExpiry,
                                         String refreshToken, String clientId,
                                         boolean tokensOnly) {
            tokens.accessToken = accessToken;
            tokens.accessExpiry = accessExpiry;
            tokens.diRefreshToken = refreshToken;
            tokens.diClientId = clientId;
            return true;
        }
        @Override public void scheduleRefresh() {}
    }
}
