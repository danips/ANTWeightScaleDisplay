package com.quantrity.antscaledisplay;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Activity-independent owner of one retained foreground upload operation. */
final class ForegroundUploadManager implements AutoCloseable {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final Application application;
    private final UploadCoordinator coordinator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "foreground-upload");
        thread.setDaemon(true);
        return thread;
    });
    private final MutableLiveData<ForegroundUploadState> state =
            new MutableLiveData<>(ForegroundUploadState.idle());
    private final MutableLiveData<OperationEvent<UploadResult>> result = new MutableLiveData<>();
    private WeakReference<MainActivity> activityRef = new WeakReference<>(null);
    private ForegroundUploadState currentState = ForegroundUploadState.idle();
    private Future<?> task;
    private long operationSequence;
    private long activeOperationId;

    ForegroundUploadManager(Application application) {
        this(application, new UploadCoordinator());
    }

    ForegroundUploadManager(Application application, UploadCoordinator coordinator) {
        this.application = application;
        this.coordinator = coordinator;
    }

    LiveData<ForegroundUploadState> state() {
        return state;
    }

    LiveData<OperationEvent<UploadResult>> result() {
        return result;
    }

    synchronized void attach(MainActivity activity) {
        activityRef = new WeakReference<>(activity);
    }

    synchronized void detach(MainActivity activity) {
        if (activityRef.get() == activity) activityRef.clear();
    }

    synchronized boolean start(Weight weight, User user, boolean tryGarmin, boolean tryEmail) {
        if (activeOperationId != 0 || user == null) return false;
        boolean uploadToGarmin = hasText(user.gc_user) && hasText(user.gc_pass) && tryGarmin;
        boolean prepareEmail = hasText(user.email_to) && tryEmail;
        long operationId = ++operationSequence;
        activeOperationId = operationId;
        currentState = ForegroundUploadState.running(
                (uploadToGarmin ? 1 : 0) + (prepareEmail ? 1 : 0));
        state.setValue(currentState);
        task = executor.submit(() -> {
            boolean uploadWithProvider = uploadToGarmin
                    && installSecurityProvider(operationId);
            UploadResult completed = coordinator.run(application, weight, user,
                    uploadWithProvider, prepareEmail,
                    () -> incrementProgress(operationId),
                    () -> requestMfaCode(operationId));
            finish(operationId, completed);
        });
        return true;
    }

    synchronized void cancel() {
        if (activeOperationId == 0) return;
        activeOperationId = 0;
        if (task != null) task.cancel(true);
        task = null;
        currentState = ForegroundUploadState.idle();
        state.postValue(currentState);
    }

    private String requestMfaCode(long operationId) throws InterruptedException {
        MainActivity activity;
        synchronized (this) {
            if (activeOperationId != operationId) return null;
            activity = activityRef.get();
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return null;
        return new DialogMfaCodeProvider(activity).requestCode();
    }

    private boolean installSecurityProvider(long operationId) {
        if (Build.VERSION.SDK_INT >= 29) return isActive(operationId);
        if (!isActive(operationId)) return false;
        try {
            ProviderInstaller.installIfNeeded(application);
            return isActive(operationId);
        } catch (GooglePlayServicesRepairableException exception) {
            Intent resolution = exception.getIntent();
            MainActivity activity;
            synchronized (this) {
                activity = activityRef.get();
            }
            if (resolution != null && activity != null && !activity.isFinishing()
                    && !activity.isDestroyed()) {
                activity.runOnUiThread(() -> activity.startActivityForResult(
                        resolution, PLAY_SERVICES_RESOLUTION_REQUEST));
            }
            return false;
        } catch (GooglePlayServicesNotAvailableException exception) {
            return false;
        }
    }

    private synchronized boolean isActive(long operationId) {
        return activeOperationId == operationId && !Thread.currentThread().isInterrupted();
    }

    private synchronized void incrementProgress(long operationId) {
        if (activeOperationId != operationId) return;
        currentState = currentState.operationCompleted();
        state.postValue(currentState);
    }

    private synchronized void finish(long operationId, UploadResult completed) {
        if (activeOperationId != operationId) return;
        activeOperationId = 0;
        task = null;
        result.postValue(new OperationEvent<>(completed));
        currentState = ForegroundUploadState.idle();
        state.postValue(currentState);
    }

    @Override
    public synchronized void close() {
        activeOperationId = 0;
        if (task != null) task.cancel(true);
        task = null;
        executor.shutdownNow();
        currentState = ForegroundUploadState.idle();
        state.postValue(currentState);
        activityRef.clear();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
