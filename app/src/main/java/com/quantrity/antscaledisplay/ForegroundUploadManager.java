package com.quantrity.antscaledisplay;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Activity-independent owner of one retained foreground upload operation. */
final class ForegroundUploadManager implements AutoCloseable {
    private final Application application;
    private final SecurityProviderCoordinator securityProvider;
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
    private SecurityProviderCoordinator.Request providerRequest;
    private Future<?> task;
    private long operationSequence;
    private long activeOperationId;

    ForegroundUploadManager(Application application,
                            SecurityProviderCoordinator securityProvider) {
        this(application, securityProvider, new UploadCoordinator());
    }

    ForegroundUploadManager(Application application,
                            SecurityProviderCoordinator securityProvider,
                            UploadCoordinator coordinator) {
        this.application = application;
        this.securityProvider = securityProvider;
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
        if (uploadToGarmin) {
            providerRequest = securityProvider.request(new SecurityProviderCoordinator.Callback() {
                @Override public void onReady() {
                    submit(operationId, weight, user, true, prepareEmail, null);
                }

                @Override public void onUnavailable() {
                    submit(operationId, weight, user, false, prepareEmail,
                            application.getString(R.string.garmin_security_provider_unavailable));
                }
            });
        } else {
            submit(operationId, weight, user, false, prepareEmail, null);
        }
        return true;
    }

    private synchronized void submit(long operationId, Weight weight, User user,
                                     boolean uploadToGarmin, boolean prepareEmail,
                                     String providerError) {
        if (activeOperationId != operationId) return;
        providerRequest = null;
        task = executor.submit(() -> {
            if (providerError != null) incrementProgress(operationId);
            UploadResult completed = coordinator.run(application, weight, user,
                    uploadToGarmin, prepareEmail,
                    () -> incrementProgress(operationId),
                    () -> requestMfaCode(operationId));
            if (providerError != null) {
                completed = new UploadResult(true, false, providerError,
                        completed.emailMessage, completed.emailError);
            }
            finish(operationId, completed);
        });
    }

    synchronized void cancel() {
        if (activeOperationId == 0) return;
        activeOperationId = 0;
        if (providerRequest != null) providerRequest.cancel();
        providerRequest = null;
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
        if (providerRequest != null) providerRequest.cancel();
        providerRequest = null;
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
