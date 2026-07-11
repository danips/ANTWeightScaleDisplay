package com.quantrity.antscaledisplay;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;

public class GarminTokenRefreshWorker extends Worker {
    static final String INPUT_USER_UUID = "user_uuid";
    private static final String TAG = "GarminTokenWorker";

    public GarminTokenRefreshWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParameters) {
        super(appContext, workerParameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userUuid = getInputData().getString(INPUT_USER_UUID);
        if (userUuid == null || userUuid.isEmpty()) return Result.failure();

        ArrayList<User> users = new ArrayList<>();
        User.deserializeUsers(getApplicationContext(), users);
        User user = findUser(users, userUuid);
        if (!GarminTokenRefreshScheduler.hasRenewalCredentials(user)) {
            // The user or their Garmin connection was removed while this job was pending.
            return Result.success();
        }

        GarminConnect garminConnect = new GarminConnect(user, users, getApplicationContext());
        GarminConnect.TokenRenewalResult renewalResult = garminConnect.renewAccessTokenInBackground();
        if (renewalResult == GarminConnect.TokenRenewalResult.SUCCESS) {
            GarminTokenRefreshScheduler.scheduleAfterCurrentWorker(getApplicationContext(), user);
            return Result.success();
        }
        if (renewalResult == GarminConnect.TokenRenewalResult.RETRY) {
            Log.w(TAG, "Temporary Garmin token refresh failure for user " + userUuid);
            return Result.retry();
        }

        Log.w(TAG, "Garmin renewal credentials were rejected for user " + userUuid);
        return Result.failure();
    }

    private static User findUser(ArrayList<User> users, String uuid) {
        for (User user : users) {
            if (uuid.equals(user.uuid)) return user;
        }
        return null;
    }
}
