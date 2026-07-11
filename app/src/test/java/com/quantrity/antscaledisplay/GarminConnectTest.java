package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GarminConnectTest {
    @Test
    public void parsesMfaExpirationInLegacyGarminFormat() {
        assertEquals(
                1_767_225_600L,
                GarminAuthenticator.parseMfaExpirationTimestamp("2026-01-01 00:00:00.000"));
    }

    @Test
    public void normalizesMillisecondMfaExpiration() {
        assertEquals(
                1_767_225_600L,
                GarminAuthenticator.parseMfaExpirationTimestamp("1767225600000"));
    }
}
