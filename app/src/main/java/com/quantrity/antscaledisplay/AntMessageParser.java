package com.quantrity.antscaledisplay;

/** Pure validation and ANT+ weight/body-composition data-page decoding. */
final class AntMessageParser {
    interface Clock { long currentTimeMillis(); }

    enum Outcome {
        IGNORED, UPDATED, FIRST_WEIGHT, COMPLETE, WEIGHT_ONLY_COMPLETE, SCALE_NOT_READY
    }

    private final Clock clock;
    private final AntDeviceInfo deviceInfo = new AntDeviceInfo();
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
        } else if (page == 0x50) {
            applyManufacturerPage(message);
        } else if (page == 0x51) {
            applyProductPage(message);
        } else if (page == 0x52) {
            applyBatteryPage(message);
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
            if (both(message[9], message[10], (byte) 0xff) && weight.weight != -1) {
                complete = true;
                return Outcome.WEIGHT_ONLY_COMPLETE;
            }
            return Outcome.UPDATED;
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

    private void applyManufacturerPage(byte[] message) {
        int hardwareRevision = message[6] & 0xff;
        int manufacturerId = (int) unsigned16(message[7], message[8]);
        int modelNumber = (int) unsigned16(message[9], message[10]);
        deviceInfo.updateManufacturer(
                hardwareRevision == 0xff ? -1 : hardwareRevision,
                manufacturerId == 0xffff ? -1 : manufacturerId,
                modelNumber == 0xffff ? -1 : modelNumber);
    }

    private void applyProductPage(byte[] message) {
        int softwareRevision = message[6] & 0xff;
        long serialNumber = unsigned32(message[7], message[8], message[9], message[10]);
        deviceInfo.updateProduct(
                softwareRevision == 0xff ? -1 : softwareRevision,
                serialNumber == 0xffffffffL ? -1 : serialNumber);
    }

    private void applyBatteryPage(byte[] message) {
        int identifierAndCount = message[5] & 0xff;
        long operatingTime = unsigned24(message[6], message[7], message[8]);
        int fractionalVoltage = message[9] & 0xff;
        int descriptiveVoltage = message[10] & 0xff;
        int voltageInteger = descriptiveVoltage & 0x0f;
        int statusCode = (descriptiveVoltage >> 4) & 0x07;
        int batteryIdentifier = (identifierAndCount >> 4) & 0x0f;
        int batteryCount = identifierAndCount & 0x0f;
        if (identifierAndCount == 0xff) {
            batteryIdentifier = -1;
            batteryCount = -1;
        }
        double voltage = voltageInteger == 0x0f
                ? -1 : voltageInteger + fractionalVoltage / 256.0;
        deviceInfo.updateBattery(
                batteryIdentifier,
                batteryCount,
                operatingTime == 0xffffffL ? -1 : operatingTime * 2,
                voltage,
                statusCode == 0x07 ? -1 : statusCode);
    }

    private static boolean isSegmentType(int type) {
        return type == 0xc5 || type == 0xbc || type == 0xc8 || type == 0xb9 || type == 0xb0;
    }
    private static double unsigned16(byte low, byte high) {
        return (low & 0xff) + 256.0 * (high & 0xff);
    }
    private static long unsigned24(byte low, byte middle, byte high) {
        return (low & 0xffL) | ((middle & 0xffL) << 8) | ((high & 0xffL) << 16);
    }
    private static long unsigned32(byte b0, byte b1, byte b2, byte b3) {
        return (b0 & 0xffL) | ((b1 & 0xffL) << 8) | ((b2 & 0xffL) << 16)
                | ((b3 & 0xffL) << 24);
    }
    private static boolean both(byte first, byte second, byte value) {
        return first == value && second == value;
    }
    private static boolean unavailable(byte first, byte second, byte lowMarker) {
        return first == lowMarker && second == (byte) 0xff;
    }

    boolean isComplete() { return complete; }
    AntDeviceInfo deviceInfo() { return deviceInfo.copy(); }

    static boolean isCommonDataPage(byte[] message) {
        return message != null && message.length >= 11 && message[1] == (byte) 0x4e
                && (message[3] == (byte) 0x50 || message[3] == (byte) 0x51
                || message[3] == (byte) 0x52);
    }
}
