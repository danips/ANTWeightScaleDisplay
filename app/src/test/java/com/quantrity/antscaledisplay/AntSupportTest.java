package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AntSupportTest {
    @Test
    public void serviceWithoutMetadataIsIgnored() {
        assertEquals(AntSupport.Capability.NONE,
                AntSupport.classifyService(false, null, null));
    }

    @Test
    public void localAdapterIsBuiltIn() {
        assertEquals(AntSupport.Capability.BUILT_IN,
                AntSupport.classifyService(true, "local", null));
    }

    @Test
    public void remoteBuiltInHardwareIsBuiltIn() {
        assertEquals(AntSupport.Capability.BUILT_IN,
                AntSupport.classifyService(true, "remote", "usb,built-in"));
    }

    @Test
    public void remoteUsbHardwareIsAnAddOn() {
        assertEquals(AntSupport.Capability.ADD_ON,
                AntSupport.classifyService(true, "remote", "usb"));
    }

    @Test
    public void missingAdapterTypeMatchesLegacyAddOnDetection() {
        assertEquals(AntSupport.Capability.ADD_ON,
                AntSupport.classifyService(true, null, null));
    }
}
