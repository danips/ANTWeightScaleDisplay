package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class LocalizedNumberParserTest {
    @Test
    public void parsesCompleteLocalizedValues() {
        assertParsed(1234.5, LocalizedNumberParser.parse("1,234.5", Locale.US));
        assertParsed(1234.5, LocalizedNumberParser.parse("1.234,5", Locale.GERMANY));
        assertParsed(-12.75, LocalizedNumberParser.parse("  -12,75  ", Locale.FRANCE));
    }

    @Test
    public void rejectsBlankInvalidAndPartiallyParsedValues() {
        assertFalse(LocalizedNumberParser.parse(null, Locale.US).isValid());
        assertFalse(LocalizedNumberParser.parse("", Locale.US).isValid());
        assertFalse(LocalizedNumberParser.parse("   ", Locale.US).isValid());
        assertFalse(LocalizedNumberParser.parse("invalid", Locale.US).isValid());
        assertFalse(LocalizedNumberParser.parse("12 kg", Locale.US).isValid());
    }

    @Test
    public void suppliesTheRequestedFallback() {
        assertEquals(-1, LocalizedNumberParser.parseOrDefault("invalid", -1), 0);
        assertEquals(0, LocalizedNumberParser.parseOrDefault("", 0), 0);
    }

    private static void assertParsed(double expected, LocalizedNumberParser.Result actual) {
        assertTrue(actual.isValid());
        assertEquals(expected, actual.value(), 0.000001);
    }
}
