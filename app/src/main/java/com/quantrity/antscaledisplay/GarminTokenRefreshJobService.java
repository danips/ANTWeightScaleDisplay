package com.quantrity.antscaledisplay;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class GarminTokenRefreshJobService extends JobService {
    private static final String TAG = "GarminTokenJob";

    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "garmin-token-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<Integer, RunningJob> runningJobs = new ConcurrentHashMap<>();

    @Override
    public boolean onStartJob(JobParameters parameters) {
        String userUuid = parameters.getExtras().getString(
                GarminTokenRefreshScheduler.EXTRA_USER_UUID);
        if (userUuid == null || userUuid.isEmpty()) return false;

        RunningJob job = new RunningJob(parameters);
        FutureTask<Void> task = new FutureTask<>(() -> {
            runRefresh(job, userUuid);
            return null;
        });
        job.future = task;
        runningJobs.put(parameters.getJobId(), job);
        executor.execute(task);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        RunningJob job = runningJobs.get(parameters.getJobId());
        if (job == null || job.parameters != parameters
                || !runningJobs.remove(parameters.getJobId(), job)) return false;
        synchronized (job) {
            if (job.finished) return false;
            job.stopped = true;
            if (job.future != null) job.future.cancel(true);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void runRefresh(RunningJob job, String userUuid) {
        try {
            RepositoryResult<java.util.List<User>> loaded =
                    AppRepository.get(getApplicationContext()).loadUsers();
            if (!loaded.isSuccess()) {
                finish(job, true);
                return;
            }
            ArrayList<User> users = new ArrayList<>(loaded.value);
            User user = findUser(users, userUuid);
            if (!GarminTokenRefreshScheduler.hasRenewalCredentials(user)) {
                // The user or their Garmin connection was removed while this job was pending.
                finish(job, false);
                return;
            }

            GarminAuthenticator authenticator = new GarminAuthenticator(
                    new GarminHttpClient(false),
                    new GarminTokenStore(getApplicationContext(), user, users),
                    () -> null);
            GarminAuthenticator.RenewalResult result = authenticator.renewInBackground();
            if (result == GarminAuthenticator.RenewalResult.SUCCESS) {
                replaceWithNextRefresh(job, user, userUuid);
            } else if (result == GarminAuthenticator.RenewalResult.RETRY) {
                Log.w(TAG, "Temporary Garmin token refresh failure for user " + userUuid);
                finish(job, true);
            } else {
                Log.w(TAG, "Garmin renewal credentials were rejected for user " + userUuid);
                finish(job, false);
            }
        } catch (RuntimeException exception) {
            Log.e(TAG, "Garmin token refresh failed for user " + userUuid, exception);
            finish(job, true);
        } finally {
            runningJobs.remove(job.parameters.getJobId(), job);
        }
    }

    private void replaceWithNextRefresh(RunningJob job, User user, String userUuid) {
        synchronized (job) {
            if (job.stopped || job.finished) return;
            // Scheduling the same ID stops this running job, so claim completion first. The
            // replacement is scheduled only after the successful renewal has been persisted.
            job.finished = true;
        }

        boolean scheduled = false;
        try {
            scheduled = GarminTokenRefreshScheduler.schedule(getApplicationContext(), user);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Could not schedule the next token refresh for user " + userUuid,
                    exception);
        }
        if (scheduled) return;

        Log.e(TAG, "Could not schedule the next token refresh for user " + userUuid);
        synchronized (job) {
            if (!job.stopped) jobFinished(job.parameters, true);
        }
    }

    private boolean finish(RunningJob job, boolean retry) {
        synchronized (job) {
            if (job.stopped || job.finished) return false;
            job.finished = true;
            jobFinished(job.parameters, retry);
            return true;
        }
    }

    private static User findUser(ArrayList<User> users, String uuid) {
        for (User user : users) {
            if (uuid.equals(user.uuid)) return user;
        }
        return null;
    }

    private static final class RunningJob {
        final JobParameters parameters;
        Future<?> future;
        boolean stopped;
        boolean finished;

        RunningJob(JobParameters parameters) {
            this.parameters = parameters;
        }
    }
}
