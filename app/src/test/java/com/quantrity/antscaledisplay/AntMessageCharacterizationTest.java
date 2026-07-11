package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntMessageCharacterizationTest {
    @Test
    public void acceptsSanitizedAntMessageWithMatchingLength() {
        byte[] message = {3, 0x40, 0x00, 0x45, 0x00};
        assertTrue(RequestWeight.isValidAntMessage(message));
    }

    @Test
    public void rejectsMissingTruncatedAndLengthMismatchedMessages() {
        assertFalse(RequestWeight.isValidAntMessage(null));
        assertFalse(RequestWeight.isValidAntMessage(new byte[] {0, 0}));
        assertFalse(RequestWeight.isValidAntMessage(new byte[] {4, 0x40, 0x00, 0x45, 0x00}));
    }
}
