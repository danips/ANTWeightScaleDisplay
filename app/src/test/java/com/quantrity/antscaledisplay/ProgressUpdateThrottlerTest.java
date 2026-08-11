package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProgressUpdateThrottlerTest {
    @Test
    public void reportsAtMostOncePerWholePercentage() {
        ProgressUpdateThrottler throttler = new ProgressUpdateThrottler();
        int reports = 0;

        for (int completed = 1; completed <= 10_000; completed++) {
            if (throttler.shouldReport(completed, 10_000)) reports++;
        }

        assertEquals(101, reports);
    }

    @Test
    public void suppressesRepeatedProgressAndResetsForANewTotal() {
        ProgressUpdateThrottler throttler = new ProgressUpdateThrottler();

        assertTrue(throttler.shouldReport(0, 10));
        assertFalse(throttler.shouldReport(0, 10));
        assertTrue(throttler.shouldReport(1, 10));
        assertFalse(throttler.shouldReport(1, 10));
        assertTrue(throttler.shouldReport(1, 20));
    }

    @Test
    public void explicitResetAllowsASecondWorkflowWithTheSameTotal() {
        ProgressUpdateThrottler throttler = new ProgressUpdateThrottler();

        assertTrue(throttler.shouldReport(1, 1));
        assertFalse(throttler.shouldReport(1, 1));
        throttler.reset();

        assertTrue(throttler.shouldReport(0, 1));
        assertTrue(throttler.shouldReport(1, 1));
    }
}
