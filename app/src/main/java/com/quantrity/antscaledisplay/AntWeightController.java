package com.quantrity.antscaledisplay;

import android.content.Context;
import android.os.RemoteException;

import com.dsi.ant.IAnt_6;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Coordinates the pure ANT session, Android service client, persistence, and UI events. */
final class AntWeightController implements AntServiceClient.Listener {
    private static final byte CHANNEL = 0;
    private static final byte CHANNEL_TYPE = 0;
    private static final byte NETWORK = 1;
    private static final byte FREQUENCY = 0x39;
    private static final int PERIOD = 8192;
    private static final int DEVICE_NUMBER = 0;
    private static final byte DEVICE_TYPE = 0x77;
    private static final byte TX_TYPE = 0;

    private final Context context;
    private WeakReference<AntWeightListener> listenerRef;
    private final AntServiceClient service;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private AntWeightSession session;
    private IAnt_6 receiver;
    private ScheduledFuture<?> timeout;
    private boolean profileSent;
    private boolean finished;
    private boolean successful;
    private AntWeightSession.Failure lastFailure;
    private String lastFailureDetail;
    private boolean failureDelivered;
    private AntWeightSession.Progress currentProgress = AntWeightSession.Progress.SEARCHING;

    Weight weight = new Weight();
    User user;

    AntWeightController(Context context, AntWeightListener listener) {
        this.context = context.getApplicationContext();
        listenerRef = new WeakReference<>(listener);
        service = new AntServiceClient(this.context, this);
    }

    void setProfile(User user) { this.user = user; }
    synchronized void attachListener(AntWeightListener listener) {
        listenerRef = new WeakReference<>(listener);
        if (finished && successful) listener.onAntSuccess(weight, user);
        else if (finished && lastFailure != null && !failureDelivered) {
            failureDelivered = true;
            listener.onAntFailure(lastFailure, lastFailureDetail);
        }
    }
    void detachListener(AntWeightListener listener) {
        if (listenerRef.get() == listener) listenerRef.clear();
    }
    boolean isRunning() { return session != null && !finished; }
    AntWeightSession.Progress progress() { return currentProgress; }

    synchronized void start() {
        if (finished || session != null || user == null) return;
        session = new AntWeightSession(user);
        weight = session.weight();
        notifyProgress(AntWeightSession.Progress.SEARCHING);
        schedule(5, () -> {
            if (session != null && session.state() == AntWeightSession.State.STARTING
                    && !service.antPermissionsGranted()) {
                fail(AntWeightSession.Failure.PERMISSION, null);
            } else {
                fail(AntWeightSession.Failure.SEARCH_TIMEOUT, null);
            }
        });
        if (!service.start()) fail(AntWeightSession.Failure.BIND, null);
    }

    synchronized void cancel() { fail(AntWeightSession.Failure.CANCELLED, null); }
    void registerReceivers() { service.registerReceivers(); }
    void unregisterReceivers() { service.unregisterReceivers(); }
    AntWeightSession.State state() {
        return session == null ? AntWeightSession.State.IDLE : session.state();
    }

    @Override
    public synchronized void onAntServiceConnected(IAnt_6 receiver) {
        if (finished) return;
        this.receiver = receiver;
        try {
            if (!receiver.claimInterface()) {
                fail(AntWeightSession.Failure.CONFIGURATION, "claimInterface");
                return;
            }
            if (!receiver.isEnabled()) receiver.enable();
            session.start();
            try {
                receiver.ANTResetSystem();
            } catch (Exception ignored) {
                // The legacy ANT service throws after a successful reset.
            }
        } catch (RemoteException exception) {
            fail(AntWeightSession.Failure.CONFIGURATION, "enable/reset");
        }
    }

    @Override public void onAntServiceDisconnected() {
        fail(AntWeightSession.Failure.DISCONNECTED, null);
    }
    @Override public void onAntBindFailed() { fail(AntWeightSession.Failure.BIND, null); }

    @Override
    public synchronized void onAntMessage(byte[] message) {
        if (finished || session == null) return;
        execute(session.onMessage(message));
    }

    private void execute(AntWeightSession.Action action) {
        try {
            switch (action.type) {
                case ASSIGN_CHANNEL:
                    require(receiver.ANTAssignChannel(CHANNEL, CHANNEL_TYPE, NETWORK),
                            "ANTAssignChannel");
                    require(receiver.ANTDisableEventBuffering(), "ANTDisableEventBuffering");
                    break;
                case SET_POWER:
                    require(receiver.ANTSetChannelTxPower(CHANNEL, (byte) 4),
                            "ANTSetChannelTxPower");
                    break;
                case SET_FREQUENCY:
                    require(receiver.ANTSetChannelRFFreq(CHANNEL, FREQUENCY),
                            "ANTSetChannelRFFreq");
                    break;
                case SET_PERIOD:
                    require(receiver.ANTSetChannelPeriod(CHANNEL, PERIOD), "ANTSetChannelPeriod");
                    break;
                case SET_ID:
                    require(receiver.ANTSetChannelId(CHANNEL, DEVICE_NUMBER, DEVICE_TYPE, TX_TYPE),
                            "ANTSetChannelId");
                    break;
                case SET_SEARCH_TIMEOUT:
                    require(receiver.ANTSetChannelSearchTimeout(CHANNEL, (byte) 10),
                            "ANTSetChannelSearchTimeout");
                    break;
                case OPEN_CHANNEL:
                    require(receiver.ANTTxMessage(new byte[]{0x01, 0x4b, 0x00}),
                            "ANTOpenChannel");
                    break;
                case SEARCH_STARTED:
                    schedule(10, () -> fail(AntWeightSession.Failure.SEARCH_TIMEOUT, null));
                    break;
                case SEND_PROFILE:
                    require(receiver.ANTTxMessage(session.profilePage()), "ANTTxMessage");
                    if (!profileSent) {
                        cancelTimeout();
                        profileSent = true;
                        notifyProgress(AntWeightSession.Progress.FOUND);
                        schedule(25, () -> fail(AntWeightSession.Failure.WEIGHT_TIMEOUT, null));
                    } else {
                        notifyProgress(AntWeightSession.Progress.WAITING);
                    }
                    break;
                case MEASUREMENT_STARTED:
                    schedule(60, () -> fail(
                            AntWeightSession.Failure.MEASUREMENT_TIMEOUT, null));
                    break;
                case COMPLETE:
                    succeed();
                    break;
                case FAIL:
                    fail(action.failure, action.detail);
                    break;
                default:
                    break;
            }
        } catch (RemoteException | CommandFailedException exception) {
            fail(AntWeightSession.Failure.CONFIGURATION, action.type.name());
        }
    }

    private void require(boolean success, String operation) {
        if (!success) throw new CommandFailedException(operation);
    }

    private synchronized void succeed() {
        if (finished || !session.hasCompleteMeasurement()) return;
        Weight result = session.weight();
        if (result.percentFat == -1) {
            result.percentFat = 1.2 * (result.weight / Math.pow(user.height_cm / 100.0, 2))
                    + 0.23 * user.age - (user.isMale ? 10.8 : 0) - 5.4;
        }
        result.activityLevel = user.activity_level + (user.isLifetimeAthlete ? 0x10 : 0);
        result.uuid = user.uuid;
        result.age = user.age;
        result.isMale = user.isMale;
        result.height = user.height_cm;
        AppRepository.get(context).upsertWeight(result, false);
        finished = true;
        successful = true;
        cleanup();
        AntWeightListener listener = listenerRef.get();
        if (listener != null) listener.onAntSuccess(result, user);
    }

    private synchronized void fail(AntWeightSession.Failure failure, String detail) {
        if (finished) return;
        finished = true;
        lastFailure = failure;
        lastFailureDetail = detail;
        weight = new Weight();
        if (session != null) session.finish();
        cleanup();
        AntWeightListener listener = listenerRef.get();
        if (listener != null) {
            failureDelivered = true;
            listener.onAntFailure(failure, detail);
        }
    }

    private void cleanup() {
        cancelTimeout();
        service.stop();
        scheduler.shutdownNow();
        receiver = null;
    }

    private void schedule(long seconds, Runnable task) {
        cancelTimeout();
        timeout = scheduler.schedule(task, seconds, TimeUnit.SECONDS);
    }

    private void cancelTimeout() {
        if (timeout != null) timeout.cancel(false);
        timeout = null;
    }

    private void notifyProgress(AntWeightSession.Progress progress) {
        currentProgress = progress;
        AntWeightListener listener = listenerRef.get();
        if (listener != null) listener.onAntProgress(progress);
    }

    private static final class CommandFailedException extends RuntimeException {
        CommandFailedException(String operation) { super(operation); }
    }
}
