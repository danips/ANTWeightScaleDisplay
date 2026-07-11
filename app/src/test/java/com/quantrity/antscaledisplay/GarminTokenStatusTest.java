package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GarminTokenStatusTest {
    private static final long NOW = 10_000;

    @Test
    public void reportsNotAuthenticatedWithoutTokens() {
        assertEquals(
                GarminTokenStatus.State.NOT_AUTHENTICATED,
                GarminTokenStatus.getState(new User(), NOW));
    }

    @Test
    public void reportsActiveWithValidAccessAndRenewalCredentials() {
        User user = authenticatedUser();
        assertEquals(GarminTokenStatus.State.ACTIVE, GarminTokenStatus.getState(user, NOW));
    }

    @Test
    public void reportsAccessExpiredWhileRenewalIsAvailable() {
        User user = authenticatedUser();
        user.garminOauth2ExpiryTimestamp = NOW - 1;
        assertEquals(
                GarminTokenStatus.State.ACCESS_EXPIRED,
                GarminTokenStatus.getState(user, NOW));
    }

    @Test
    public void reportsConnectionExpiredEvenWhenAccessIsStillValid() {
        User user = authenticatedUser();
        user.garminOauth1MfaExpirationTimestamp = NOW;
        assertEquals(
                GarminTokenStatus.State.CONNECTION_EXPIRED,
                GarminTokenStatus.getState(user, NOW));
    }

    private static User authenticatedUser() {
        User user = new User();
        user.uuid = "user";
        user.garminOauth1Token = "oauth1";
        user.garminOauth1TokenSecret = "secret";
        user.garminOauth1MfaExpirationTimestamp = NOW + 200;
        user.garminOauth2Token = "access";
        user.garminOauth2ExpiryTimestamp = NOW + 100;
        return user;
    }
}
