package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntMessageParserTest {
    @Test
    public void decodesStandardWeightCompositionSequence() {
        AntMessageParser parser = new AntMessageParser(() -> 123_456L);
        Weight weight = new Weight();

        assertEquals(AntMessageParser.Outcome.FIRST_WEIGHT,
                parser.apply(page(1, 0, 0, 0, 0, 0, 0x10, 0x27), weight));
        parser.apply(page(2, 0, 0, 0, 0x50, 0x14, 0x46, 0x0a), weight);
        parser.apply(page(3, 0, 0, 0, 0x40, 0x1f, 0x00, 0x19), weight);
        assertEquals(AntMessageParser.Outcome.COMPLETE,
                parser.apply(page(4, 0, 0, 0, 0, 0x9e, 0x11, 24), weight));

        assertEquals(123_456L, weight.date);
        assertEquals(100.0, weight.weight, 0.001);
        assertEquals(52.0, weight.percentHydration, 0.001);
        assertEquals(26.3, weight.percentFat, 0.001);
        assertEquals(2000, weight.activeMet, 0.001);
        assertEquals(1600, weight.basalMet, 0.001);
        assertEquals(45.1, weight.muscleMass, 0.001);
        assertEquals(2.4, weight.boneMass, 0.001);
    }

    @Test
    public void incompleteSequenceNeverReportsComplete() {
        AntMessageParser parser = new AntMessageParser(() -> 1L);
        Weight weight = new Weight();

        parser.apply(page(1, 0, 0, 0, 0, 0, 0x10, 0x27), weight);

        assertFalse(parser.isComplete());
        assertEquals(-1, weight.percentFat, 0);
    }

    @Test
    public void reportsScaleNotReadyAndWeightOnlyCompletion() {
        AntMessageParser parser = new AntMessageParser(() -> 1L);
        Weight weight = new Weight();
        assertEquals(AntMessageParser.Outcome.SCALE_NOT_READY,
                parser.apply(page(1, 0, 0, 0, 0, 0, 0xff, 0xff), weight));

        parser = new AntMessageParser(() -> 1L);
        weight = new Weight();
        parser.apply(page(1, 0, 0, 0, 0, 0, 0x10, 0x27), weight);
        assertEquals(AntMessageParser.Outcome.WEIGHT_ONLY_COMPLETE,
                parser.apply(page(0xf1, 0xff, 0xa2, 0, 0, 0, 0xff, 0xff), weight));
        assertTrue(parser.isComplete());
    }

    @Test
    public void decodesSanitizedTanitaSegmentalSequence() {
        AntMessageParser parser = new AntMessageParser(() -> 2L);
        Weight weight = new Weight();
        parser.apply(page(1, 0, 0, 0, 0, 0, 0x10, 0x27), weight);
        parser.apply(special(0xc5, 0xff, 0, 0, 0, 0), weight); // announces segmental data
        parser.apply(special(0xa2, 0, 0x46, 0x0a, 0x50, 0x14), weight);
        parser.apply(special(0xa3, 0, 0x9e, 0x11, 6, 0), weight);
        parser.apply(special(0xa9, 0, 0x60, 0x09, 0x4c, 0x1d), weight);
        parser.apply(special(0xd4, 0, 0x80, 0x3e, 35, 0), weight);
        parser.apply(special(0xc5, 0, 0x10, 0x10, 0x20, 0x20), weight);
        parser.apply(special(0xbc, 0, 0x08, 0x07, 0x10, 0x07), weight);
        parser.apply(special(0xc8, 0, 0x6c, 0x07, 0x30, 0x10), weight);
        parser.apply(special(0xb9, 0, 0x40, 0x20, 0x20, 0x4e), weight);
        assertEquals(AntMessageParser.Outcome.COMPLETE,
                parser.apply(special(0xb0, 0, 0x34, 0x08, 0x98, 0x08), weight));

        assertEquals(4.112, weight.leftArmMuscleMass, 0.001);
        assertEquals(8.224, weight.rightLegMuscleMass, 0.001);
        assertEquals(18.0, weight.rightArmPercentFat, 0.001);
        assertEquals(20.0, weight.trunkMuscleMass, 0.001);
        assertTrue(parser.isComplete());
    }

    private static byte[] special(int type, int flags, int low1, int high1,
                                  int low2, int high2) {
        return page(0xf1, flags, type, low1, high1, 0, low2, high2);
    }

    static byte[] page(int page, int d4, int d5, int d6, int d7, int d8, int d9, int d10) {
        return new byte[]{9, 0x4e, 0, (byte) page, (byte) d4, (byte) d5,
                (byte) d6, (byte) d7, (byte) d8, (byte) d9, (byte) d10};
    }
}
