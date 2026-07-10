package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class GarminTokenRefreshSchedulerTest {
    @Test
    public void schedulesSixHoursBeforeExpiry() {
        long now = TimeUnit.DAYS.toMillis(10);
        long expirySeconds = TimeUnit.MILLISECONDS.toSeconds(now + TimeUnit.HOURS.toMillis(24));

        assertEquals(
                TimeUnit.HOURS.toMillis(18),
                GarminTokenRefreshScheduler.calculateDelayMillis(now, expirySeconds));
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
}
