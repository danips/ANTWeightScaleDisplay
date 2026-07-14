package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.job.JobInfo;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GarminTokenRefreshSchedulerTest {
    @Test
    public void schedulesSixHoursBeforeAccessTokenExpiry() {
        long now = TimeUnit.DAYS.toMillis(10);
        long accessExpirySeconds = TimeUnit.MILLISECONDS.toSeconds(
                now + TimeUnit.HOURS.toMillis(24));

        assertEquals(
                TimeUnit.HOURS.toMillis(18),
                GarminTokenRefreshScheduler.calculateDelayMillis(now, accessExpirySeconds));
    }

    @Test
    public void schedulesImmediatelyInsideSafetyWindow() {
        long now = TimeUnit.DAYS.toMillis(10);
        long expirySeconds = TimeUnit.MILLISECONDS.toSeconds(now + TimeUnit.HOURS.toMillis(2));

        assertEquals(0, GarminTokenRefreshScheduler.calculateDelayMillis(now, expirySeconds));
    }

    @Test
    public void schedulesImmediatelyAfterLocalExpiry() {
        long now = TimeUnit.DAYS.toMillis(10);
        long expirySeconds = TimeUnit.MILLISECONDS.toSeconds(now - TimeUnit.MINUTES.toMillis(1));

        assertEquals(0, GarminTokenRefreshScheduler.calculateDelayMillis(now, expirySeconds));
    }

    @Test
    public void requiresSavedOAuth1Credentials() {
        User user = new User();
        assertFalse(GarminTokenRefreshScheduler.hasRenewalCredentials(user));

        user.uuid = "user";
        user.garminOauth1Token = "oauth1";
        user.garminOauth1TokenSecret = "secret";
        assertTrue(GarminTokenRefreshScheduler.hasRenewalCredentials(user));

        user.garminOauth1Token = "";
        assertFalse(GarminTokenRefreshScheduler.hasRenewalCredentials(user));

        user.garminOauth1Token = "oauth1";
        user.uuid = "";
        assertFalse(GarminTokenRefreshScheduler.hasRenewalCredentials(user));
    }

    @Test
    public void jobIdIsStableForAUser() {
        Map<Integer, String> owners = new HashMap<>();
        int jobId = GarminTokenRefreshScheduler.selectJobId("user", owners);

        owners.put(jobId, "user");
        assertEquals(jobId, GarminTokenRefreshScheduler.selectJobId("user", owners));
    }

    @Test
    public void collidingUuidHashesReceiveDifferentStableJobIds() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertEquals(GarminTokenRefreshScheduler.baseJobId("Aa"),
                GarminTokenRefreshScheduler.baseJobId("BB"));

        Map<Integer, String> owners = new HashMap<>();
        int first = GarminTokenRefreshScheduler.selectJobId("Aa", owners);
        owners.put(first, "Aa");
        int second = GarminTokenRefreshScheduler.selectJobId("BB", owners);
        owners.put(second, "BB");

        assertNotEquals(first, second);
        assertEquals(second, GarminTokenRefreshScheduler.selectJobId("BB", owners));
    }

    @Test
    public void retriesStartWithThirtyMinuteExponentialBackoff() {
        assertEquals(TimeUnit.MINUTES.toMillis(30),
                GarminTokenRefreshScheduler.INITIAL_RETRY_BACKOFF_MILLIS);
        assertEquals(JobInfo.BACKOFF_POLICY_EXPONENTIAL,
                GarminTokenRefreshScheduler.RETRY_BACKOFF_POLICY);
    }
}
