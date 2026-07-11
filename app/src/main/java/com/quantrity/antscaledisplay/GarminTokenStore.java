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
    @Override public String mfaToken() { return user == null ? null : user.garminOauth1MfaToken; }

    @Override
    public void storeOAuth1(String token, String secret, String mfaToken, long mfaExpiry) {
        user.garminOauth1Token = token;
        user.garminOauth1TokenSecret = secret;
        user.garminOauth1MfaToken = mfaToken;
        user.garminOauth1MfaExpirationTimestamp = mfaExpiry;
    }

    @Override
    public boolean storeAccess(String token, long expiry, boolean tokensOnly) {
        user.garminOauth2Token = token;
        user.garminOauth2ExpiryTimestamp = expiry;
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
