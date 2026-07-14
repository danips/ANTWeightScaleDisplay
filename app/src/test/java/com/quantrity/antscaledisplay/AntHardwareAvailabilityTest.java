package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AntHardwareAvailabilityTest {
    @Test
    public void radioServiceIsAlwaysRequired() {
        assertEquals(AntHardwareAvailability.Result.RADIO_SERVICE_MISSING,
                AntHardwareAvailability.determine(false, true, true, true));
    }

    @Test
    public void integratedAntNeedsNoUsbComponents() {
        assertEquals(AntHardwareAvailability.Result.AVAILABLE,
                AntHardwareAvailability.determine(true, true, false, false));
    }

    @Test
    public void externalAntNeedsAConnectedUsbDevice() {
        assertEquals(AntHardwareAvailability.Result.USB_DEVICE_MISSING,
                AntHardwareAvailability.determine(true, false, false, true));
    }

    @Test
    public void externalAntNeedsTheUsbService() {
        assertEquals(AntHardwareAvailability.Result.USB_SERVICE_MISSING,
                AntHardwareAvailability.determine(true, false, true, false));
    }

    @Test
    public void externalAntIsAvailableWithUsbDeviceAndService() {
        assertEquals(AntHardwareAvailability.Result.AVAILABLE,
                AntHardwareAvailability.determine(true, false, true, true));
    }
}
