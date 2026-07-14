package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

import java.util.List;

/** Detects whether ANT is provided by built-in hardware or an external add-on. */
final class AntSupport {
    private static final String ANT_SHARED_LIBRARY = "com.dsi.ant.antradio_library";
    private static final String SERVICE_INFO_ACTION =
            "com.dsi.ant.intent.request.SERVICE_INFO";
    private static final String ADAPTER_TYPE = "ANT_AdapterType";
    private static final String HARDWARE_TYPE = "ANT_HardwareType";
    private static final String REMOTE_ADAPTER = "remote";
    private static final String BUILT_IN_HARDWARE = "built-in";

    enum Capability { BUILT_IN, ADD_ON, NONE }

    private AntSupport() { }

    static Capability detect(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (hasAntSharedLibrary(packageManager)) return Capability.BUILT_IN;

        boolean addOnFound = false;
        for (ResolveInfo resolved : queryAntServices(packageManager)) {
            ServiceInfo service = resolved == null ? null : resolved.serviceInfo;
            if (service == null) continue;
            Bundle metadata = service.metaData;
            Capability capability = classifyService(
                    metadata != null,
                    metadata == null ? null : metadata.getString(ADAPTER_TYPE),
                    metadata == null ? null : metadata.getString(HARDWARE_TYPE));
            if (capability == Capability.BUILT_IN) return Capability.BUILT_IN;
            if (capability == Capability.ADD_ON) addOnFound = true;
        }
        return addOnFound ? Capability.ADD_ON : Capability.NONE;
    }

    private static boolean hasAntSharedLibrary(PackageManager packageManager) {
        String[] libraries = packageManager.getSystemSharedLibraryNames();
        if (libraries == null) return false;
        for (String library : libraries) {
            if (ANT_SHARED_LIBRARY.equals(library)) return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static List<ResolveInfo> queryAntServices(PackageManager packageManager) {
        return packageManager.queryIntentServices(
                new Intent(SERVICE_INFO_ACTION), PackageManager.GET_META_DATA);
    }

    static Capability classifyService(boolean hasMetadata, String adapterType,
                                      String hardwareType) {
        if (!hasMetadata) return Capability.NONE;
        if (!REMOTE_ADAPTER.equals(adapterType)) {
            return adapterType == null ? Capability.ADD_ON : Capability.BUILT_IN;
        }
        return hardwareType != null && hardwareType.contains(BUILT_IN_HARDWARE)
                ? Capability.BUILT_IN : Capability.ADD_ON;
    }
}
