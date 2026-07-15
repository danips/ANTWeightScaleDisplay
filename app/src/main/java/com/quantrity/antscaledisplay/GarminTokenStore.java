package com.quantrity.antscaledisplay;

import android.content.Context;

import java.util.ArrayList;

/** Repository-backed storage for Garmin OAuth credentials. */
final class GarminTokenStore implements GarminAuthenticator.TokenStore {
    private final Context context;
    private final AppRepository repository;
    private final User user;
    private final ArrayList<User> users;

    GarminTokenStore(Context context, User user, ArrayList<User> users) {
        this.context = context.getApplicationContext();
        repository = AppRepository.get(this.context);
        this.user = user;
        this.users = users;
    }

    @Override public String accessToken() { return user == null ? null : user.garminOauth2Token; }
    @Override public long accessExpiry() {
        return user == null ? -1 : user.garminOauth2ExpiryTimestamp;
    }
    @Override public String oauth1Token() { return user == null ? null : user.garminOauth1Token; }
    @Override public String oauth1Secret() {
        return user == null ? null : user.garminOauth1TokenSecret;
    }
    @Override public String diRefreshToken() {
        return user == null ? null : user.garminDiRefreshToken;
    }
    @Override public String diClientId() { return user == null ? null : user.garminDiClientId; }

    @Override
    public boolean discardLegacySession() {
        if (user == null) return false;
        clearLegacyTokens(user, true);
        boolean saved = persist(true);
        if (saved) GarminTokenRefreshScheduler.cancel(context, user);
        return saved;
    }

    @Override
    public boolean storeDi(String accessToken, long accessExpiry, String refreshToken,
                           String clientId, boolean tokensOnly) {
        user.garminOauth2Token = accessToken;
        user.garminOauth2ExpiryTimestamp = accessExpiry;
        user.garminDiRefreshToken = refreshToken;
        user.garminDiClientId = clientId;
        clearLegacyTokens(user, false);
        return persist(tokensOnly);
    }

    private static void clearLegacyTokens(User user, boolean clearAccess) {
        user.garminOauth1Token = null;
        user.garminOauth1TokenSecret = null;
        user.garminOauth1MfaToken = null;
        user.garminOauth1MfaExpirationTimestamp = -1;
        if (clearAccess) {
            user.garminOauth2Token = null;
            user.garminOauth2ExpiryTimestamp = -1;
        }
    }

    private boolean persist(boolean tokensOnly) {
        RepositoryResult<Void> result = tokensOnly
                ? repository.updateGarminTokensSynchronously(user)
                : repository.saveUsersSynchronously(users);
        boolean saved = result.isSuccess();
        if (saved && !tokensOnly) GarminTokenRefreshScheduler.schedule(context, user);
        return saved;
    }

    @Override
    public void scheduleRefresh() {
        GarminTokenRefreshScheduler.schedule(context, user);
    }
}
