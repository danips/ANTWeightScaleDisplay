package com.quantrity.antscaledisplay;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

final class GarminTokenRefreshScheduler {
    private static final String UNIQUE_WORK_PREFIX = "garmin-token-refresh-";
    private static final long ACCESS_REFRESH_LEAD_TIME_MILLIS = TimeUnit.HOURS.toMillis(6);

    private GarminTokenRefreshScheduler() {}

    static void scheduleAll(Context context, List<User> users) {
        for (User user : users) schedule(context, user);
    }

    static void schedule(Context context, User user) {
        enqueue(context, user, ExistingWorkPolicy.REPLACE);
    }

    /** Append the next refresh behind the Worker which is currently using this unique work name. */
    static void scheduleAfterCurrentWorker(Context context, User user) {
        enqueue(context, user, ExistingWorkPolicy.APPEND_OR_REPLACE);
    }

    static void cancel(Context context, User user) {
        if (user != null && user.uuid != null) {
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork(uniqueWorkName(user.uuid));
        }
    }

    private static void enqueue(Context context, User user, ExistingWorkPolicy policy) {
        if (!hasRenewalCredentials(user)) {
            cancel(context, user);
            return;
        }

        Data input = new Data.Builder()
                .putString(GarminTokenRefreshWorker.INPUT_USER_UUID, user.uuid)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GarminTokenRefreshWorker.class)
                .setInputData(input)
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayMillis(
                        System.currentTimeMillis(), user.garminOauth2ExpiryTimestamp),
                        TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(uniqueWorkName(user.uuid), policy, request);
    }

    static long calculateDelayMillis(long nowMillis, long accessExpirySeconds) {
        long refreshAtMillis = TimeUnit.SECONDS.toMillis(accessExpirySeconds)
                - ACCESS_REFRESH_LEAD_TIME_MILLIS;
        return Math.max(0, refreshAtMillis - nowMillis);
    }

    static boolean hasRenewalCredentials(User user) {
        return user != null
                && user.uuid != null
                && user.garminOauth1Token != null
                && !user.garminOauth1Token.isEmpty()
                && user.garminOauth1TokenSecret != null
                && !user.garminOauth1TokenSecret.isEmpty();
    }

    @NonNull
    static String uniqueWorkName(String userUuid) {
        return UNIQUE_WORK_PREFIX + userUuid;
    }
}
