package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SecurityProviderPreflightTest {
    @Test
    public void readyAlwaysProceeds() {
        assertEquals(SecurityProviderPreflight.Action.PROCEED,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.READY, false));
        assertEquals(SecurityProviderPreflight.Action.PROCEED,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.READY, true));
    }

    @Test
    public void repairIsLaunchedOnlyOnce() {
        assertEquals(SecurityProviderPreflight.Action.LAUNCH_REPAIR,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.REPAIRABLE, false));
        assertEquals(SecurityProviderPreflight.Action.FAIL,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.REPAIRABLE, true));
    }

    @Test
    public void unavailableAlwaysFails() {
        assertEquals(SecurityProviderPreflight.Action.FAIL,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.UNAVAILABLE, false));
        assertEquals(SecurityProviderPreflight.Action.FAIL,
                SecurityProviderPreflight.actionFor(
                        SecurityProviderPreflight.Status.UNAVAILABLE, true));
    }

    @Test
    public void repairWithoutResolutionIsUnavailable() {
        assertEquals(SecurityProviderPreflight.Status.UNAVAILABLE,
                SecurityProviderPreflight.Outcome.repairable(null).status);
    }
}
