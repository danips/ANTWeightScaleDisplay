package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

final class AppRepository {
    private static final String SELECTED_USER_KEY = "selected_user";
    private static final String USERS_FILE_NAME = "users";
    private static final String HISTORY_FILE_NAME = "history";
    private static final String GOALS_FILE_NAME = "goals";
    private static volatile AppRepository instance;

    private final AtomicJsonFile usersFile;
    private final AtomicJsonFile historyFile;
    private final AtomicJsonFile goalsFile;
    private final UserJsonCodec userCodec = new UserJsonCodec();
    private final WeightJsonCodec weightCodec = new WeightJsonCodec();
    private final GoalJsonCodec goalCodec = new GoalJsonCodec();
    private final ExecutorService writeExecutor;
    private final SharedPreferences preferences;
    private final File filesDirectory;

    static AppRepository get(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    Context application = context.getApplicationContext();
                    instance = new AppRepository(application.getFilesDir(), application.getSharedPreferences(
                            application.getPackageName() + "_preferences", Context.MODE_PRIVATE));
                }
            }
        }
        return instance;
    }

    AppRepository(File filesDirectory) {
        this(filesDirectory, null);
    }

    private AppRepository(File filesDirectory, SharedPreferences preferences) {
        this.filesDirectory = filesDirectory;
        usersFile = new AtomicJsonFile(new File(filesDirectory, USERS_FILE_NAME));
        historyFile = new AtomicJsonFile(new File(filesDirectory, HISTORY_FILE_NAME));
        goalsFile = new AtomicJsonFile(new File(filesDirectory, GOALS_FILE_NAME));
        this.preferences = preferences;
        writeExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "app-repository-writes");
            thread.setDaemon(true);
            return thread;
        });
    }

    RepositoryResult<List<User>> loadUsers() {
        RepositoryResult<String> read = usersFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return userCodec.decode(read.value);
    }

    RepositoryResult<List<Weight>> loadWeights() {
        RepositoryResult<String> read = historyFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return weightCodec.decode(read.value);
    }

    RepositoryResult<List<Goal>> loadGoals() {
        RepositoryResult<String> read = goalsFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return goalCodec.decode(read.value);
    }

    RepositoryResult<Void> saveUsersSynchronously(List<User> users) {
        ArrayList<User> snapshot = copyUsers(users);
        return await(() -> saveUsersPreservingNewerTokens(snapshot));
    }

    Future<RepositoryResult<Void>> saveWeights(List<Weight> weights) {
        RepositoryResult<String> encoded = weightCodec.encode(new ArrayList<>(weights));
        if (!encoded.isSuccess()) return completed(encoded);
        return writeExecutor.submit(() -> historyFile.write(encoded.value));
    }

    Future<RepositoryResult<Void>> saveGoals(List<Goal> goals) {
        RepositoryResult<String> encoded = goalCodec.encode(new ArrayList<>(goals));
        if (!encoded.isSuccess()) return completed(encoded);
        return writeExecutor.submit(() -> goalsFile.write(encoded.value));
    }

    RepositoryResult<Void> updateGarminTokensSynchronously(User tokenSource) {
        return await(() -> {
            RepositoryResult<List<User>> loaded = loadUsers();
            if (!loaded.isSuccess()) return RepositoryResult.failure(loaded.message, loaded.error);
            User target = findUser(loaded.value, tokenSource.uuid);
            if (target == null) {
                return RepositoryResult.failure("Could not find user for Garmin token update",
                        new IllegalArgumentException("Unknown user " + tokenSource.uuid));
            }
            copyGarminAccessToken(tokenSource, target);
            return writeUsers(loaded.value);
        });
    }

    RepositoryResult<Void> reloadGarminTokens(User target) {
        if (target == null || target.uuid == null) return RepositoryResult.success(null);
        RepositoryResult<List<User>> loaded = loadUsers();
        if (!loaded.isSuccess()) return RepositoryResult.failure(loaded.message, loaded.error);
        User latest = findUser(loaded.value, target.uuid);
        if (latest != null) copyGarminAccessToken(latest, target);
        return RepositoryResult.success(null);
    }

    String getSelectedUserName() {
        return preferences == null ? null : preferences.getString(SELECTED_USER_KEY, null);
    }

    void setSelectedUserName(String name) {
        if (preferences != null) preferences.edit().putString(SELECTED_USER_KEY, name).apply();
    }

    void clearSelectedUser() {
        if (preferences != null) preferences.edit().remove(SELECTED_USER_KEY).apply();
    }

    List<File> dataFiles() {
        return java.util.Arrays.asList(
                new File(filesDirectory, USERS_FILE_NAME),
                new File(filesDirectory, HISTORY_FILE_NAME),
                new File(filesDirectory, GOALS_FILE_NAME));
    }

    void close() {
        writeExecutor.shutdownNow();
    }

    private RepositoryResult<Void> saveUsersPreservingNewerTokens(List<User> users) {
        RepositoryResult<List<User>> loaded = loadUsers();
        if (!loaded.isSuccess()) return RepositoryResult.failure(loaded.message, loaded.error);
        for (User outgoing : users) {
            User latest = findUser(loaded.value, outgoing.uuid);
            if (latest == null) continue;
            if (latest.garminOauth2ExpiryTimestamp > outgoing.garminOauth2ExpiryTimestamp) {
                outgoing.garminOauth2Token = latest.garminOauth2Token;
                outgoing.garminOauth2ExpiryTimestamp = latest.garminOauth2ExpiryTimestamp;
            }
            if (latest.garminOauth1MfaExpirationTimestamp > outgoing.garminOauth1MfaExpirationTimestamp) {
                outgoing.garminOauth1Token = latest.garminOauth1Token;
                outgoing.garminOauth1TokenSecret = latest.garminOauth1TokenSecret;
                outgoing.garminOauth1MfaToken = latest.garminOauth1MfaToken;
                outgoing.garminOauth1MfaExpirationTimestamp = latest.garminOauth1MfaExpirationTimestamp;
            }
        }
        return writeUsers(users);
    }

    private RepositoryResult<Void> writeUsers(List<User> users) {
        RepositoryResult<String> encoded = userCodec.encode(users);
        if (!encoded.isSuccess()) return RepositoryResult.failure(encoded.message, encoded.error);
        return usersFile.write(encoded.value);
    }

    private static ArrayList<User> copyUsers(List<User> users) {
        UserJsonCodec codec = new UserJsonCodec();
        RepositoryResult<String> encoded = codec.encode(users);
        if (!encoded.isSuccess()) return new ArrayList<>(users);
        RepositoryResult<List<User>> decoded = codec.decode(encoded.value);
        return decoded.isSuccess() ? new ArrayList<>(decoded.value) : new ArrayList<>(users);
    }

    private static User findUser(List<User> users, String uuid) {
        if (uuid == null) return null;
        for (User user : users) if (uuid.equals(user.uuid)) return user;
        return null;
    }

    private static void copyGarminAccessToken(User source, User target) {
        target.garminOauth2Token = source.garminOauth2Token;
        target.garminOauth2ExpiryTimestamp = source.garminOauth2ExpiryTimestamp;
    }

    private RepositoryResult<Void> await(Callable<RepositoryResult<Void>> operation) {
        try {
            return writeExecutor.submit(operation).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RepositoryResult.failure("Interrupted while saving application data", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Exception error = cause instanceof Exception ? (Exception) cause : e;
            return RepositoryResult.failure("Could not save application data", error);
        }
    }

    private static Future<RepositoryResult<Void>> completed(RepositoryResult<?> failed) {
        FutureTask<RepositoryResult<Void>> task = new FutureTask<>(
                () -> RepositoryResult.failure(failed.message, failed.error));
        task.run();
        return task;
    }
}
