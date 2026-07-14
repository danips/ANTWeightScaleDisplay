package com.quantrity.antscaledisplay;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class GarminTokenRefreshScheduler {
    static final String EXTRA_USER_UUID = "user_uuid";
    static final long INITIAL_RETRY_BACKOFF_MILLIS = TimeUnit.MINUTES.toMillis(30);
    static final int RETRY_BACKOFF_POLICY = JobInfo.BACKOFF_POLICY_EXPONENTIAL;

    private static final String TAG = "GarminTokenScheduler";
    private static final long ACCESS_REFRESH_LEAD_TIME_MILLIS = TimeUnit.HOURS.toMillis(6);
    private static final int JOB_ID_PREFIX = 0x47500000;
    private static final int JOB_ID_SUFFIX_MASK = 0x000fffff;

    private GarminTokenRefreshScheduler() {}

    static void scheduleAll(Context context, List<User> users) {
        for (User user : users) schedule(context, user);
    }

    static boolean schedule(Context context, User user) {
        Context application = context.getApplicationContext();
        if (!hasRenewalCredentials(user)) {
            cancel(application, user);
            return false;
        }

        JobScheduler scheduler = application.getSystemService(JobScheduler.class);
        if (scheduler == null) {
            Log.e(TAG, "JobScheduler is unavailable");
            return false;
        }

        ComponentName service = new ComponentName(application,
                GarminTokenRefreshJobService.class);
        Map<Integer, String> jobOwners = jobOwners(scheduler, service);
        int jobId = selectJobId(user.uuid, jobOwners);
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_USER_UUID, user.uuid);
        JobInfo job = new JobInfo.Builder(jobId, service)
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(calculateDelayMillis(
                        System.currentTimeMillis(), user.garminOauth2ExpiryTimestamp))
                .setBackoffCriteria(INITIAL_RETRY_BACKOFF_MILLIS, RETRY_BACKOFF_POLICY)
                .setPersisted(true)
                .build();
        if (scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS) return true;

        Log.e(TAG, "Could not schedule Garmin token refresh for user " + user.uuid);
        return false;
    }

    static void cancel(Context context, User user) {
        if (user == null || user.uuid == null) return;
        Context application = context.getApplicationContext();
        JobScheduler scheduler = application.getSystemService(JobScheduler.class);
        if (scheduler == null) return;

        ComponentName service = new ComponentName(application,
                GarminTokenRefreshJobService.class);
        for (JobInfo job : scheduler.getAllPendingJobs()) {
            if (service.equals(job.getService())
                    && user.uuid.equals(job.getExtras().getString(EXTRA_USER_UUID))) {
                scheduler.cancel(job.getId());
            }
        }
    }

    static long calculateDelayMillis(long nowMillis, long accessExpirySeconds) {
        long refreshAtMillis = TimeUnit.SECONDS.toMillis(accessExpirySeconds)
                - ACCESS_REFRESH_LEAD_TIME_MILLIS;
        return Math.max(0, refreshAtMillis - nowMillis);
    }

    static boolean hasRenewalCredentials(User user) {
        return user != null
                && user.uuid != null
                && !user.uuid.isEmpty()
                && user.garminOauth1Token != null
                && !user.garminOauth1Token.isEmpty()
                && user.garminOauth1TokenSecret != null
                && !user.garminOauth1TokenSecret.isEmpty();
    }

    static int baseJobId(String userUuid) {
        int hash = userUuid.hashCode();
        hash ^= hash >>> 16;
        return JOB_ID_PREFIX | (hash & JOB_ID_SUFFIX_MASK);
    }

    static int selectJobId(String userUuid, Map<Integer, String> jobOwners) {
        for (Map.Entry<Integer, String> job : jobOwners.entrySet()) {
            if (userUuid.equals(job.getValue())) return job.getKey();
        }

        int candidate = baseJobId(userUuid);
        for (int attempts = 0; attempts <= JOB_ID_SUFFIX_MASK; attempts++) {
            if (!jobOwners.containsKey(candidate)) return candidate;
            int suffix = ((candidate & JOB_ID_SUFFIX_MASK) + 1) & JOB_ID_SUFFIX_MASK;
            candidate = JOB_ID_PREFIX | suffix;
        }
        throw new IllegalStateException("No Garmin token refresh job IDs are available");
    }

    private static Map<Integer, String> jobOwners(
            JobScheduler scheduler, ComponentName refreshService) {
        Map<Integer, String> owners = new HashMap<>();
        for (JobInfo job : scheduler.getAllPendingJobs()) {
            String owner = refreshService.equals(job.getService())
                    ? job.getExtras().getString(EXTRA_USER_UUID) : null;
            owners.put(job.getId(), owner);
        }
        return owners;
    }
}
