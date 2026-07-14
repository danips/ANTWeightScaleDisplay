package com.quantrity.antscaledisplay;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Owns one lifecycle-bound interactive Garmin history workflow and its notification. */
final class GarminHistoryDownloadCoordinator implements DefaultLifecycleObserver {
    private static final String TAG = "GarminHistoryDownload";
    private static final String CHANNEL_ID = "GC";
    private static final int NOTIFICATION_ID = 0;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    interface Listener {
        void onHistoryImported(ArrayList<Weight> weights, int added);
        void onHistoryDownloadFailed(String message);
    }

    private final Context context;
    private final WeakReference<Activity> activityRef;
    private volatile Listener listener;
    private final GarminHistoryImporter importer;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NotificationManager notificationManager;
    private final NotificationCompat.Builder notification;

    private Future<?> task;
    private volatile boolean running;
    private volatile boolean closed;

    GarminHistoryDownloadCoordinator(Activity activity, Listener listener) {
        this(activity, listener, new GarminHistoryImporter());
    }

    GarminHistoryDownloadCoordinator(Activity activity, Listener listener,
                                     GarminHistoryImporter importer) {
        context = activity.getApplicationContext();
        activityRef = new WeakReference<>(activity);
        this.listener = listener;
        this.importer = importer;
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "garmin-history-download");
            thread.setDaemon(true);
            return thread;
        });
        notificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(
                        R.string.history_fragment_download,
                        context.getString(R.string.edit_user_fragment_garmin_connect_category)))
                .setContentText(context.getString(R.string.history_fragment_download_in_progress))
                .setSmallIcon(R.drawable.ic_gc)
                .setLargeIcon(drawableToBitmap(context, R.drawable.ic_gc))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true);
    }

    private static Bitmap drawableToBitmap(Context context, int resourceId) {
        Drawable drawable = ContextCompat.getDrawable(context, resourceId);
        if (drawable == null) return null;
        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    synchronized boolean start(User user, ArrayList<User> users, List<Weight> existing) {
        if (closed || running || user == null) return false;
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;

        if (!installSecurityProvider(activity)) return false;
        running = true;
        showProgress(0, 1);
        ArrayList<Weight> historySnapshot = new ArrayList<>(existing);
        task = executor.submit(() -> download(user, users, historySnapshot));
        return true;
    }

    boolean isRunning() {
        return running;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        close();
        owner.getLifecycle().removeObserver(this);
    }

    synchronized void close() {
        if (closed) return;
        closed = true;
        running = false;
        if (task != null) task.cancel(true);
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        listener = null;
        cancelNotification();
    }

    private void download(User user, ArrayList<User> users, ArrayList<Weight> existing) {
        try {
            Activity activity = activityRef.get();
            if (closed || activity == null || activity.isFinishing() || activity.isDestroyed()) {
                running = false;
                return;
            }
            GarminForegroundSession garmin = new GarminForegroundSession(user, users, activity);
            if (!garmin.signIn()) {
                if (!Thread.currentThread().isInterrupted()) {
                    fail(context.getString(R.string.weight_fragment_msg_wrong_credentials));
                }
                return;
            }

            String json = garmin.downloadHistory();
            if (json == null || Thread.currentThread().isInterrupted()) {
                if (!Thread.currentThread().isInterrupted()) fail(downloadFailureMessage());
                return;
            }
            GarminHistoryImporter.Result result = importer.importHistory(
                    json, user, existing, this::showProgress);
            if (closed || Thread.currentThread().isInterrupted()) return;
            cancelNotification();
            mainHandler.post(() -> deliver(result));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        } catch (Exception exception) {
            Log.e(TAG, "Unable to download Garmin history", exception);
            if (!closed) fail(downloadFailureMessage());
            else running = false;
        }
    }

    private void deliver(GarminHistoryImporter.Result result) {
        running = false;
        if (closed) return;
        Listener currentListener = listener;
        if (currentListener != null) {
            currentListener.onHistoryImported(result.weights, result.added);
        }
    }

    private void fail(String message) {
        showFailure(message);
        mainHandler.post(() -> {
            running = false;
            if (closed) return;
            Listener currentListener = listener;
            if (currentListener != null) currentListener.onHistoryDownloadFailed(message);
        });
    }

    private String downloadFailureMessage() {
        String operation = context.getString(
                R.string.history_fragment_download,
                context.getString(R.string.edit_user_fragment_garmin_connect_category));
        return context.getString(R.string.weight_process_msg_problem_while, operation);
    }

    private void showProgress(int completed, int total) {
        if (closed || !canPostNotifications()) return;
        notification.setContentText(context.getString(R.string.history_fragment_download_in_progress))
                .setProgress(Math.max(total, 1), completed, false);
        notifySafely();
    }

    private void showFailure(String message) {
        if (closed || !canPostNotifications()) return;
        notification.setContentText(message).setProgress(0, 0, false);
        notifySafely();
    }

    private void notifySafely() {
        try {
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, notification.build());
            }
        } catch (SecurityException exception) {
            Log.w(TAG, "Notification permission was not granted", exception);
        }
    }

    private void cancelNotification() {
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
    }

    private boolean canPostNotifications() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26 || notificationManager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.edit_user_fragment_garmin_connect_category),
                NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    private static boolean installSecurityProvider(Activity activity) {
        if (Build.VERSION.SDK_INT >= 29) return true;
        try {
            ProviderInstaller.installIfNeeded(activity);
            return true;
        } catch (GooglePlayServicesRepairableException exception) {
            Log.e(TAG, "Google Play services needs repair", exception);
            Intent resolutionIntent = exception.getIntent();
            if (resolutionIntent != null) {
                activity.startActivityForResult(
                        resolutionIntent, PLAY_SERVICES_RESOLUTION_REQUEST);
            }
            return false;
        } catch (GooglePlayServicesNotAvailableException exception) {
            Log.e(TAG, "Google Play services is unavailable", exception);
            return false;
        }
    }
}
