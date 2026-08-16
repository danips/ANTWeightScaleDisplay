package com.quantrity.antscaledisplay;

/** Decoded ANT+ common-page information reported by a weight scale. */
final class AntDeviceInfo {
    enum BatteryStatus {
        UNKNOWN,
        NEW,
        GOOD,
        OK,
        LOW,
        CRITICAL,
        INVALID,
        UNRECOGNIZED
    }

    private int hardwareRevision = -1;
    private int manufacturerId = -1;
    private int modelNumber = -1;
    private int softwareRevision = -1;
    private long serialNumber = -1;
    private int batteryIdentifier = -1;
    private int batteryCount = -1;
    private long cumulativeOperatingTimeSeconds = -1;
    private double batteryVoltage = -1;
    private int batteryStatusCode = -1;
    private BatteryStatus batteryStatus = BatteryStatus.UNKNOWN;

    void updateManufacturer(int hardwareRevision, int manufacturerId, int modelNumber) {
        if (hardwareRevision >= 0) this.hardwareRevision = hardwareRevision;
        if (manufacturerId >= 0) this.manufacturerId = manufacturerId;
        if (modelNumber >= 0) this.modelNumber = modelNumber;
    }

    void updateProduct(int softwareRevision, long serialNumber) {
        if (softwareRevision >= 0) this.softwareRevision = softwareRevision;
        if (serialNumber >= 0) this.serialNumber = serialNumber;
    }

    void updateBattery(int batteryIdentifier, int batteryCount,
                       long cumulativeOperatingTimeSeconds, double batteryVoltage,
                       int batteryStatusCode) {
        if (batteryIdentifier >= 0) this.batteryIdentifier = batteryIdentifier;
        if (batteryCount >= 0) this.batteryCount = batteryCount;
        if (cumulativeOperatingTimeSeconds >= 0) {
            this.cumulativeOperatingTimeSeconds = cumulativeOperatingTimeSeconds;
        }
        if (batteryVoltage >= 0) this.batteryVoltage = batteryVoltage;
        if (batteryStatusCode >= 0) {
            this.batteryStatusCode = batteryStatusCode;
            this.batteryStatus = statusForCode(batteryStatusCode);
        }
    }

    boolean hasManufacturerInfo() {
        return hardwareRevision >= 0 || manufacturerId >= 0 || modelNumber >= 0;
    }

    boolean hasProductInfo() {
        return softwareRevision >= 0 || serialNumber >= 0;
    }

    boolean hasBatteryStatus() {
        return batteryIdentifier >= 0 || batteryCount >= 0
                || cumulativeOperatingTimeSeconds >= 0 || batteryVoltage >= 0
                || batteryStatusCode >= 0;
    }

    boolean hasAnyInfo() {
        return hasManufacturerInfo() || hasProductInfo() || hasBatteryStatus();
    }

    int hardwareRevision() { return hardwareRevision; }
    int manufacturerId() { return manufacturerId; }
    int modelNumber() { return modelNumber; }
    int softwareRevision() { return softwareRevision; }
    long serialNumber() { return serialNumber; }
    int batteryIdentifier() { return batteryIdentifier; }
    int batteryCount() { return batteryCount; }
    long cumulativeOperatingTimeSeconds() { return cumulativeOperatingTimeSeconds; }
    double batteryVoltage() { return batteryVoltage; }
    int batteryStatusCode() { return batteryStatusCode; }
    BatteryStatus batteryStatus() { return batteryStatus; }

    AntDeviceInfo copy() {
        AntDeviceInfo result = new AntDeviceInfo();
        result.hardwareRevision = hardwareRevision;
        result.manufacturerId = manufacturerId;
        result.modelNumber = modelNumber;
        result.softwareRevision = softwareRevision;
        result.serialNumber = serialNumber;
        result.batteryIdentifier = batteryIdentifier;
        result.batteryCount = batteryCount;
        result.cumulativeOperatingTimeSeconds = cumulativeOperatingTimeSeconds;
        result.batteryVoltage = batteryVoltage;
        result.batteryStatusCode = batteryStatusCode;
        result.batteryStatus = batteryStatus;
        return result;
    }

    private static BatteryStatus statusForCode(int code) {
        switch (code) {
            case 1: return BatteryStatus.NEW;
            case 2: return BatteryStatus.GOOD;
            case 3: return BatteryStatus.OK;
            case 4: return BatteryStatus.LOW;
            case 5: return BatteryStatus.CRITICAL;
            case 7: return BatteryStatus.INVALID;
            default: return BatteryStatus.UNRECOGNIZED;
        }
    }
}
