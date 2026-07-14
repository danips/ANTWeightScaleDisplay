package com.quantrity.antscaledisplay;

/** Decides whether the app can start an ANT measurement with the available hardware. */
final class AntHardwareAvailability {
    enum Result {
        AVAILABLE,
        RADIO_SERVICE_MISSING,
        USB_DEVICE_MISSING,
        USB_SERVICE_MISSING
    }

    private AntHardwareAvailability() { }

    static Result determine(boolean radioServiceInstalled,
                            boolean integratedAnt,
                            boolean usbDeviceConnected,
                            boolean antUsbServiceInstalled) {
        if (!radioServiceInstalled) return Result.RADIO_SERVICE_MISSING;
        if (integratedAnt) return Result.AVAILABLE;
        if (!usbDeviceConnected) return Result.USB_DEVICE_MISSING;
        if (!antUsbServiceInstalled) return Result.USB_SERVICE_MISSING;
        return Result.AVAILABLE;
    }
}
