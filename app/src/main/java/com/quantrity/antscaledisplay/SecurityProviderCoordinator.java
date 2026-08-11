package com.quantrity.antscaledisplay;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Retains provider preflight and a single repair launch across Activity recreation. */
final class SecurityProviderCoordinator implements AutoCloseable {
    interface Callback {
        void onReady();
        void onUnavailable();
    }

    final class Request {
        private final long id;
        private boolean cancelled;

        private Request(long id) {
            this.id = id;
        }

        void cancel() {
            synchronized (SecurityProviderCoordinator.this) {
                if (cancelled) return;
                cancelled = true;
                pending.remove(id);
                invalidateUnusedRepairRequest();
            }
        }
    }

    private static final class Pending {
        final Callback callback;
        boolean repairAttempted;
        boolean waitingForRepair;

        Pending(Callback callback) {
            this.callback = callback;
        }
    }

    private final Application application;
    private final SecurityProviderPreflight preflight;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "security-provider-preflight");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<OperationEvent<SecurityProviderRepairRequest>> repairRequests =
            new MutableLiveData<>();
    private final Map<Long, Pending> pending = new LinkedHashMap<>();
    private long nextRequestId;
    private long repairGeneration;
    private boolean repairInFlight;
    private boolean closed;

    SecurityProviderCoordinator(Application application) {
        this(application, new SecurityProviderPreflight());
    }

    SecurityProviderCoordinator(Application application, SecurityProviderPreflight preflight) {
        this.application = application;
        this.preflight = preflight;
    }

    LiveData<OperationEvent<SecurityProviderRepairRequest>> repairRequests() {
        return repairRequests;
    }

    synchronized Request request(Callback callback) {
        if (closed) {
            mainHandler.post(callback::onUnavailable);
            return null;
        }
        long id = ++nextRequestId;
        Request request = new Request(id);
        pending.put(id, new Pending(callback));
        check(id);
        return request;
    }

    synchronized Intent claimRepair(SecurityProviderRepairRequest request) {
        if (closed || !repairInFlight || request == null
                || request.generation != repairGeneration || !hasWaitingRequest()) return null;
        return request.intent;
    }

    void onRepairResult(boolean succeeded) {
        ArrayList<Long> retry = new ArrayList<>();
        ArrayList<Long> remove = new ArrayList<>();
        ArrayList<Callback> failed = new ArrayList<>();
        synchronized (this) {
            if (closed || !repairInFlight) return;
            repairInFlight = false;
            for (Map.Entry<Long, Pending> entry : pending.entrySet()) {
                Pending action = entry.getValue();
                if (!action.waitingForRepair) continue;
                action.waitingForRepair = false;
                if (succeeded) {
                    action.repairAttempted = true;
                    retry.add(entry.getKey());
                } else {
                    remove.add(entry.getKey());
                    failed.add(action.callback);
                }
            }
            for (Long id : remove) pending.remove(id);
        }
        for (Callback callback : failed) callback.onUnavailable();
        for (Long id : retry) check(id);
    }

    private void check(long id) {
        executor.execute(() -> {
            SecurityProviderPreflight.Outcome outcome = preflight.install(application);
            mainHandler.post(() -> handleOutcome(id, outcome));
        });
    }

    private void handleOutcome(long id, SecurityProviderPreflight.Outcome outcome) {
        Callback callback = null;
        SecurityProviderPreflight.Action action;
        synchronized (this) {
            Pending current = pending.get(id);
            if (closed || current == null) return;
            action = SecurityProviderPreflight.actionFor(
                    outcome.status, current.repairAttempted);
            if (action == SecurityProviderPreflight.Action.PROCEED
                    || action == SecurityProviderPreflight.Action.FAIL) {
                pending.remove(id);
                callback = current.callback;
            } else {
                current.waitingForRepair = true;
                if (!repairInFlight) {
                    repairInFlight = true;
                    SecurityProviderRepairRequest request = new SecurityProviderRepairRequest(
                            ++repairGeneration, outcome.repairIntent);
                    repairRequests.setValue(new OperationEvent<>(request));
                }
            }
        }
        if (callback == null) return;
        if (action == SecurityProviderPreflight.Action.PROCEED) callback.onReady();
        else callback.onUnavailable();
    }

    private boolean hasWaitingRequest() {
        for (Pending action : pending.values()) {
            if (action.waitingForRepair) return true;
        }
        return false;
    }

    private void invalidateUnusedRepairRequest() {
        if (repairInFlight && !hasWaitingRequest()) {
            repairInFlight = false;
            repairGeneration++;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        pending.clear();
        repairInFlight = false;
        repairGeneration++;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }
}
