package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MfaNotificationParserTest {
    @Test
    public void acceptsGarminKeywordInBodyOrTitle() {
        assertEquals("123456", MfaNotificationParser.findCode(
                null, "Your Garmin verification code is 123456"));
        assertEquals("654321", MfaNotificationParser.findCode(
                "Garmin", "Your verification code is 654321"));
    }

    @Test
    public void rejectsUnrelatedSixDigitNotifications() {
        assertNull(MfaNotificationParser.findCode(
                "Bank", "Your verification code is 123456"));
    }

    @Test
    public void requiresAStandaloneSixDigitCode() {
        assertNull(MfaNotificationParser.findCode(
                "Garmin", "Your verification code is 1234567"));
        assertNull(MfaNotificationParser.findCode(
                "Garmin", "Your verification code is 12345"));
    }

    @Test
    public void keywordMatchingIsCaseInsensitiveButStillStandalone() {
        assertEquals("123456", MfaNotificationParser.findCode(
                "GARMIN", "Verification code: 123456"));
        assertNull(MfaNotificationParser.findCode(
                "NotGarmin", "Verification code: 123456"));
    }
}
