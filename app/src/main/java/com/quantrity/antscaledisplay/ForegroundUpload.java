package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** UI owner for the foreground measurement-upload workflow. */
class ForegroundUpload {
    private static final String TAG = "ForegroundUpload";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final WeakReference<MainActivity> activityRef;
    private final Weight weight;
    private final User user;
    private final boolean tryGarmin;
    private final boolean tryEmail;
    private final UploadCoordinator coordinator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private Future<?> submittedTask;
    private volatile boolean cancelled;

    ForegroundUpload(MainActivity activity, Weight weight, User user, boolean tryGarmin,
                boolean tryEmail) {
        this(activity, weight, user, tryGarmin, tryEmail, new UploadCoordinator());
    }

    ForegroundUpload(MainActivity activity, Weight weight, User user, boolean tryGarmin,
                boolean tryEmail, UploadCoordinator coordinator) {
        activityRef = new WeakReference<>(activity);
        this.weight = weight;
        this.user = user;
        this.tryGarmin = tryGarmin;
        this.tryEmail = tryEmail;
        this.coordinator = coordinator;
    }

    public synchronized void execute() {
        MainActivity activity = activityRef.get();
        if (activity == null || submittedTask != null) return;

        boolean uploadToGarmin = hasText(user.gc_user) && hasText(user.gc_pass) && tryGarmin;
        boolean prepareEmail = hasText(user.email_to) && tryEmail;
        showProgressDialog(activity, (uploadToGarmin ? 1 : 0) + (prepareEmail ? 1 : 0));

        submittedTask = executor.submit(() -> {
            MainActivity currentActivity = activityRef.get();
            if (currentActivity == null || cancelled) {
                finish(null);
                return;
            }
            if (uploadToGarmin) installSecurityProvider(currentActivity);
            UploadResult result = coordinator.run(currentActivity, weight, user, uploadToGarmin,
                    prepareEmail, this::incrementProgress);
            finish(result);
        });
    }

    public synchronized void cancel() {
        cancelled = true;
        if (submittedTask != null) submittedTask.cancel(true);
        executor.shutdownNow();
        mainHandler.post(this::dismissProgressDialog);
    }

    private void finish(UploadResult result) {
        mainHandler.post(() -> {
            try {
                MainActivity activity = activityRef.get();
                if (!cancelled && result != null && activity != null
                        && !activity.isFinishing() && !activity.isDestroyed()) {
                    renderResult(activity, result);
                }
            } finally {
                dismissProgressDialog();
                executor.shutdown();
            }
        });
    }

    private void renderResult(MainActivity activity, UploadResult result) {
        if (result.garminSucceeded) {
            Toast.makeText(activity, String.format(
                    activity.getString(R.string.weight_fragment_msg_updating_success),
                    activity.getString(R.string.edit_user_fragment_garmin_connect_category)),
                    Toast.LENGTH_SHORT).show();
        }

        StringBuilder errors = new StringBuilder();
        appendError(errors, activity.getString(
                R.string.edit_user_fragment_garmin_connect_category), result.garminError);
        appendError(errors, activity.getString(R.string.edit_user_fragment_email_category),
                result.emailError);
        if (errors.length() > 0) activity.showMessage(errors.toString());

        if (result.emailMessage != null) {
            MeasurementTextFormatter.EmailMessage message = result.emailMessage;
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{message.recipient});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, message.subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message.body);
            activity.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }

    private void installSecurityProvider(MainActivity activity) {
        if (Build.VERSION.SDK_INT >= 29 || cancelled) return;
        try {
            ProviderInstaller.installIfNeeded(activity);
        } catch (GooglePlayServicesRepairableException exception) {
            Log.e(TAG, "Google Play Services needs repair", exception);
            GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
            int errorCode = exception.getConnectionStatusCode();
            mainHandler.post(() -> {
                MainActivity currentActivity = activityRef.get();
                if (currentActivity == null || currentActivity.isFinishing()
                        || currentActivity.isDestroyed()) return;
                Dialog dialog = availability.getErrorDialog(currentActivity, errorCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST);
                if (dialog != null) dialog.show();
            });
        } catch (GooglePlayServicesNotAvailableException exception) {
            Log.e(TAG, "Google Play Services is unavailable", exception);
        }
    }

    private void showProgressDialog(MainActivity activity, int max) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView message = new TextView(activity);
        message.setText(R.string.weight_fragment_msg_uploading);
        message.setPadding(0, 0, 0, 20);
        layout.addView(message);

        progressBar = new ProgressBar(activity, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(max);
        layout.addView(progressBar);

        progressDialog = new AlertDialog.Builder(activity).setView(layout)
                .setCancelable(false).create();
        progressDialog.show();
    }

    private void incrementProgress() {
        mainHandler.post(() -> {
            if (!cancelled && progressBar != null) progressBar.incrementProgressBy(1);
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        progressDialog = null;
        progressBar = null;
    }

    private static void appendError(StringBuilder output, String category, String error) {
        if (error == null || error.isEmpty()) return;
        if (output.length() > 0) output.append('\n');
        output.append(category).append(": ").append(error);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
