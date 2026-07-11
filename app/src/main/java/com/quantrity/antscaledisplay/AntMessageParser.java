package com.quantrity.antscaledisplay;

/** Pure validation and ANT+ weight/body-composition data-page decoding. */
final class AntMessageParser {
    interface Clock { long currentTimeMillis(); }

    enum Outcome { IGNORED, UPDATED, FIRST_WEIGHT, COMPLETE, SCALE_NOT_READY, NOT_BAREFOOT }

    private final Clock clock;
    private boolean weightPage;
    private boolean compositionPage;
    private boolean massPage;
    private boolean bonePage;
    private boolean metabolicPage;
    private boolean segmental;
    private boolean c5;
    private boolean bc;
    private boolean c8;
    private boolean b9;
    private boolean b0;
    private boolean complete;

    AntMessageParser() { this(System::currentTimeMillis); }
    AntMessageParser(Clock clock) { this.clock = clock; }

    static boolean isValid(byte[] message) {
        return message != null && message.length > 2
                && Byte.toUnsignedInt(message[0]) == message.length - 2;
    }

    Outcome apply(byte[] message, Weight weight) {
        if (!isValid(message) || message.length < 11 || message[1] != (byte) 0x4e) {
            return Outcome.IGNORED;
        }
        int page = message[3] & 0xff;
        boolean firstWeight = false;
        if (page == 0x01) {
            if (!unavailable(message[9], message[10], (byte) 0xfe)) {
                if (both(message[9], message[10], (byte) 0xff)) return Outcome.SCALE_NOT_READY;
                firstWeight = !weightPage;
                weightPage = true;
                weight.weight = unsigned16(message[9], message[10]) / 100.0;
                weight.date = clock.currentTimeMillis();
            }
        } else if (page == 0xf1) {
            Outcome special = applyTanita(message, weight);
            if (special != Outcome.UPDATED) return special;
        } else if (page == 0x02 && !compositionPage
                && !unavailable(message[9], message[10], (byte) 0xfe)) {
            compositionPage = true;
            if (!both(message[7], message[8], (byte) 0xff)) {
                weight.percentHydration = unsigned16(message[7], message[8]) / 100.0;
            }
            if (!both(message[9], message[10], (byte) 0xff)) {
                weight.percentFat = unsigned16(message[9], message[10]) / 100.0;
            }
        } else if (page == 0x03 && !metabolicPage
                && !unavailable(message[9], message[10], (byte) 0xfe)) {
            metabolicPage = true;
            if (!both(message[7], message[8], (byte) 0xff)) {
                weight.activeMet = unsigned16(message[7], message[8]) / 4.0;
            }
            if (!both(message[9], message[10], (byte) 0xff)) {
                weight.basalMet = unsigned16(message[9], message[10]) / 4.0;
            }
        } else if (page == 0x04 && !massPage
                && !unavailable(message[8], message[9], (byte) 0xfe)) {
            massPage = bonePage = true;
            if (!both(message[8], message[9], (byte) 0xff)) {
                weight.muscleMass = unsigned16(message[8], message[9]) / 100.0;
            }
            if (message[10] != (byte) 0xff) weight.boneMass = (message[10] & 0xff) / 10.0;
        } else if (page == 0x50 || page == 0x51) {
            return Outcome.IGNORED;
        }

        if (!complete && weightPage && compositionPage && massPage && bonePage && metabolicPage
                && (!segmental || (b0 && b9 && bc && c5 && c8))) {
            complete = true;
            return Outcome.COMPLETE;
        }
        return firstWeight ? Outcome.FIRST_WEIGHT : Outcome.UPDATED;
    }

    private Outcome applyTanita(byte[] message, Weight weight) {
        int type = message[5] & 0xff;
        if (message[4] == (byte) 0xff) {
            if (isSegmentType(type)) segmental = true;
            return both(message[9], message[10], (byte) 0xff) && weight.weight != -1
                    ? Outcome.NOT_BAREFOOT : Outcome.UPDATED;
        }
        if (type == 0xa2 && !compositionPage) {
            compositionPage = true;
            weight.percentFat = unsigned16(message[6], message[7]) / 100.0;
            weight.percentHydration = unsigned16(message[9], message[10]) / 100.0;
        } else if (type == 0xa3 && !massPage) {
            massPage = true;
            weight.muscleMass = unsigned16(message[6], message[7]) / 1000.0
                    + (((message[4] & 0xff) >> 4) * 65.535);
            weight.physiqueRating = (short) unsigned16(message[9], message[10]);
        } else if (type == 0xa9 && !bonePage) {
            bonePage = true;
            weight.boneMass = unsigned16(message[6], message[7]) / 1000.0;
            weight.visceralFatRating = unsigned16(message[9], message[10]) / 1000.0;
        } else if (type == 0xd4 && !metabolicPage) {
            metabolicPage = true;
            weight.basalMet = unsigned16(message[6], message[7]) / 100.0
                    + (((message[4] & 0xff) >> 4) * 655.35);
            weight.metabolicAge = (short) unsigned16(message[9], message[10]);
        } else if (type == 0xc5 && !c5) {
            segmental = c5 = true;
            weight.leftArmMuscleMass = unsigned16(message[6], message[7]) / 1000.0;
            weight.rightLegMuscleMass = unsigned16(message[9], message[10]) / 1000.0;
        } else if (type == 0xbc && !bc) {
            segmental = bc = true;
            weight.rightArmPercentFat = unsigned16(message[6], message[7]) / 100.0;
            weight.leftArmPercentFat = unsigned16(message[9], message[10]) / 100.0;
        } else if (type == 0xc8 && !c8) {
            segmental = c8 = true;
            weight.trunkPercentFat = unsigned16(message[6], message[7]) / 100.0;
            weight.rightArmMuscleMass = unsigned16(message[9], message[10]) / 1000.0;
        } else if (type == 0xb9 && !b9) {
            segmental = b9 = true;
            weight.leftLegMuscleMass = unsigned16(message[6], message[7]) / 1000.0;
            weight.trunkMuscleMass = unsigned16(message[9], message[10]) / 1000.0;
        } else if (type == 0xb0 && !b0) {
            segmental = b0 = true;
            weight.rightLegPercentFat = unsigned16(message[6], message[7]) / 100.0;
            weight.leftLegPercentFat = unsigned16(message[9], message[10]) / 100.0;
        }
        return Outcome.UPDATED;
    }

    private static boolean isSegmentType(int type) {
        return type == 0xc5 || type == 0xbc || type == 0xc8 || type == 0xb9 || type == 0xb0;
    }
    private static double unsigned16(byte low, byte high) {
        return (low & 0xff) + 256.0 * (high & 0xff);
    }
    private static boolean both(byte first, byte second, byte value) {
        return first == value && second == value;
    }
    private static boolean unavailable(byte first, byte second, byte lowMarker) {
        return first == lowMarker && second == (byte) 0xff;
    }

    boolean hasWeight() { return weightPage; }
    boolean isComplete() { return complete; }
}
