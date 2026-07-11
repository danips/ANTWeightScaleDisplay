package com.quantrity.antscaledisplay;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.core.content.ContextCompat;

import com.dsi.ant.IAnt_6;

import java.util.List;

/** Owns ANT service binding, broadcast registration, and idempotent cleanup. */
final class AntServiceClient {
    interface Listener {
        void onAntServiceConnected(IAnt_6 receiver);
        void onAntServiceDisconnected();
        void onAntMessage(byte[] message);
        void onAntBindFailed();
    }

    private static final String RX_ACTION = "com.dsi.ant.intent.action.ANT_RX_MESSAGE_ACTION";
    private static final String MESSAGE_EXTRA = "com.dsi.ant.intent.ANT_MESSAGE";

    private final Context context;
    private final Listener listener;
    private IAnt_6 receiver;
    private boolean bound;
    private boolean receiversRegistered;

    AntServiceClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { /* Status is diagnostic. */ }
    };

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (RX_ACTION.equals(intent.getAction())) {
                listener.onAntMessage(intent.getByteArrayExtra(MESSAGE_EXTRA));
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            receiver = IAnt_6.Stub.asInterface(service);
            if (receiver == null) {
                listener.onAntBindFailed();
                return;
            }
            registerReceivers();
            listener.onAntServiceConnected(receiver);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            receiver = null;
            listener.onAntServiceDisconnected();
        }
    };

    boolean start() {
        if (bound) return true;
        Intent explicit = explicitServiceIntent(new Intent("com.dsi.ant.IAnt_6"));
        if (explicit == null) return false;
        bound = context.bindService(explicit, connection, Context.BIND_AUTO_CREATE);
        return bound;
    }

    synchronized void stop() {
        IAnt_6 current = receiver;
        receiver = null;
        if (current != null) {
            try {
                current.ANTCloseChannel((byte) 0);
                current.ANTUnassignChannel((byte) 0);
            } catch (RemoteException ignored) {
                // The service is already gone; local cleanup must still complete.
            }
        }
        unregisterReceivers();
        if (bound) {
            try {
                context.unbindService(connection);
            } catch (IllegalArgumentException ignored) {
                // Already unbound by the framework.
            }
            bound = false;
        }
    }

    void registerReceivers() {
        if (receiversRegistered || !bound) return;
        IntentFilter status = new IntentFilter();
        status.addAction("com.dsi.ant.intent.action.ANT_ENABLED");
        status.addAction("com.dsi.ant.intent.action.ANT_DISABLED");
        status.addAction("com.dsi.ant.intent.action.ANT_INTERFACE_CLAIMED_ACTION");
        status.addAction("com.dsi.ant.intent.action.ANT_RESET");
        ContextCompat.registerReceiver(context, statusReceiver, status,
                ContextCompat.RECEIVER_EXPORTED);
        IntentFilter data = new IntentFilter(RX_ACTION);
        ContextCompat.registerReceiver(context, dataReceiver, data,
                ContextCompat.RECEIVER_EXPORTED);
        receiversRegistered = true;
    }

    void unregisterReceivers() {
        if (!receiversRegistered) return;
        try { context.unregisterReceiver(statusReceiver); } catch (IllegalArgumentException ignored) { }
        try { context.unregisterReceiver(dataReceiver); } catch (IllegalArgumentException ignored) { }
        receiversRegistered = false;
    }

    boolean antPermissionsGranted() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    "com.dsi.ant.service.socket", PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions == null || info.requestedPermissionsFlags == null) {
                return false;
            }
            for (int index = 0; index < info.requestedPermissions.length; index++) {
                if (info.requestedPermissions[index].startsWith("com.dsi.ant.permission.ANT")
                        && (info.requestedPermissionsFlags[index]
                        & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) return true;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
        return false;
    }

    private Intent explicitServiceIntent(Intent implicit) {
        List<ResolveInfo> matches = context.getPackageManager().queryIntentServices(implicit, 0);
        if (matches.size() != 1) return null;
        ResolveInfo match = matches.get(0);
        Intent explicit = new Intent(implicit);
        explicit.setComponent(new ComponentName(match.serviceInfo.packageName,
                match.serviceInfo.name));
        return explicit;
    }

    static String messageToString(byte[] message) {
        if (message == null) return "null";
        StringBuilder output = new StringBuilder();
        for (byte value : message) {
            if (output.length() > 0) output.append(' ');
            output.append(String.format("%02x", value));
        }
        return output.toString();
    }
}
