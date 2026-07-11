package com.quantrity.antscaledisplay;

final class GarminTokenStatus {
    enum State {
        NOT_AUTHENTICATED,
        ACTIVE,
        ACCESS_EXPIRED,
        CONNECTION_EXPIRED,
        UNKNOWN
    }

    private GarminTokenStatus() {}

    static State getState(User user, long nowSeconds) {
        if (user == null) return State.NOT_AUTHENTICATED;

        boolean hasAccessToken = hasValue(user.garminOauth2Token);
        boolean hasRenewalCredentials = GarminTokenRefreshScheduler.hasRenewalCredentials(user);
        if (!hasAccessToken && !hasRenewalCredentials) return State.NOT_AUTHENTICATED;

        if (hasRenewalCredentials && user.garminOauth1MfaExpirationTimestamp > 0
                && user.garminOauth1MfaExpirationTimestamp <= nowSeconds) {
            return State.CONNECTION_EXPIRED;
        }
        if (hasAccessToken && user.garminOauth2ExpiryTimestamp > nowSeconds) {
            return State.ACTIVE;
        }
        if (hasRenewalCredentials) return State.ACCESS_EXPIRED;
        return State.UNKNOWN;
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }
}
