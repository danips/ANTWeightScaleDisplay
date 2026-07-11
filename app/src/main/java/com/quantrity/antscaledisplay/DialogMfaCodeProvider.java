package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.lifecycle.Observer;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Activity-backed MFA UI adapter; authentication itself has no Android UI dependency. */
final class DialogMfaCodeProvider implements MfaCodeProvider {
    private final WeakReference<Activity> activityRef;

    DialogMfaCodeProvider(Activity activity) {
        activityRef = new WeakReference<>(activity);
    }

    @Override
    public String requestCode() throws InterruptedException {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return null;
        BlockingQueue<String> input = new LinkedBlockingQueue<>();
        activity.runOnUiThread(() -> showPermissionOrInput(activity, input));
        return input.take();
    }

    private void showPermissionOrInput(Activity activity, BlockingQueue<String> input) {
        if (!notificationAccessEnabled(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.auth_notification_permission_title)
                    .setMessage(R.string.auth_notification_permission_message)
                    .setPositiveButton(R.string.auth_notification_permission_enable,
                            (dialog, which) -> openNotificationSettings(activity))
                    .setNegativeButton(R.string.auth_notification_permission_skip, null)
                    .setOnDismissListener(dialog -> showInput(activity, input))
                    .show();
        } else {
            showInput(activity, input);
        }
    }

    private void showInput(Activity activity, BlockingQueue<String> inputQueue) {
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.auth_garmin_verification_hint);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.auth_garmin_verification_title)
                .setView(input)
                .setMessage(R.string.auth_garmin_verification_message)
                .setPositiveButton(R.string.auth_garmin_verification_submit,
                        (ignored, id) -> inputQueue.offer(input.getText().toString()))
                .setNegativeButton(R.string.auth_garmin_verification_cancel,
                        (ignored, id) -> inputQueue.offer(""))
                .create();

        Observer<String> observer = code -> {
            if (code != null && code.matches("\\d{6}") && dialog.isShowing()) {
                input.setText(code);
                inputQueue.offer(code);
                dialog.dismiss();
            }
        };
        NotificationRepository.getInstance().getLatestNotification().observeForever(observer);
        dialog.setOnDismissListener(ignored -> {
            NotificationRepository.getInstance().getLatestNotification()
                    .removeObserver(observer);
            if (inputQueue.isEmpty()) inputQueue.offer("");
        });
        dialog.setOnShowListener(ignored -> {
            input.requestFocus();
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private static boolean notificationAccessEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
        if (enabled == null || enabled.isEmpty()) return false;
        for (String name : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(name);
            if (component != null && context.getPackageName().equals(component.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private static void openNotificationSettings(Activity activity) {
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            ComponentName component = new ComponentName(activity, NotificationListener.class);
            intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString());
        } else {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        }
        activity.startActivity(intent);
    }
}
