package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppRepository {
    interface MutationCallback {
        void onComplete(RepositoryResult<Void> result);
    }
    private static final String SELECTED_USER_KEY = "selected_user";
    private static final String SELECTED_USER_UUID_KEY = "selected_user_uuid";
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
    private final SelectionStore selectionStore;
    private final File filesDirectory;
    private final Object stateLock = new Object();
    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<Weight> weights = new ArrayList<>();
    private final ArrayList<Goal> goals = new ArrayList<>();
    private String selectedUserUuid;
    private boolean stateLoaded;

    static AppRepository get(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    Context application = context.getApplicationContext();
                    SharedPreferences preferences = application.getSharedPreferences(
                            application.getPackageName() + "_preferences", Context.MODE_PRIVATE);
                    instance = new AppRepository(application.getFilesDir(),
                            new SharedPreferencesSelectionStore(preferences));
                }
            }
        }
        return instance;
    }

    AppRepository(File filesDirectory) {
        this(filesDirectory, null);
    }

    AppRepository(File filesDirectory, SelectionStore selectionStore) {
        this.filesDirectory = filesDirectory;
        usersFile = new AtomicJsonFile(new File(filesDirectory, USERS_FILE_NAME));
        historyFile = new AtomicJsonFile(new File(filesDirectory, HISTORY_FILE_NAME));
        goalsFile = new AtomicJsonFile(new File(filesDirectory, GOALS_FILE_NAME));
        this.selectionStore = selectionStore;
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

    RepositoryResult<Void> reloadState() {
        RepositoryResult<List<User>> loadedUsers = loadUsers();
        if (!loadedUsers.isSuccess()) return RepositoryResult.failure(loadedUsers.message, loadedUsers.error);
        RepositoryResult<List<Weight>> loadedWeights = loadWeights();
        if (!loadedWeights.isSuccess()) return RepositoryResult.failure(loadedWeights.message, loadedWeights.error);
        RepositoryResult<List<Goal>> loadedGoals = loadGoals();
        if (!loadedGoals.isSuccess()) return RepositoryResult.failure(loadedGoals.message, loadedGoals.error);

        synchronized (stateLock) {
            users.clear();
            users.addAll(loadedUsers.value);
            weights.clear();
            weights.addAll(loadedWeights.value);
            goals.clear();
            goals.addAll(loadedGoals.value);
            sortUsers(users);
            Collections.sort(weights, new Weight.DateComparator());
            selectedUserUuid = resolveSelectedUserUuid(users);
            stateLoaded = true;
        }
        return RepositoryResult.success(null);
    }

    boolean isStateLoaded() {
        synchronized (stateLock) {
            return stateLoaded;
        }
    }

    ArrayList<User> usersSnapshot() {
        synchronized (stateLock) {
            return new ArrayList<>(users);
        }
    }

    ArrayList<Weight> weightsSnapshot() {
        synchronized (stateLock) {
            return new ArrayList<>(weights);
        }
    }

    ArrayList<Goal> goalsSnapshot() {
        synchronized (stateLock) {
            return new ArrayList<>(goals);
        }
    }

    User selectedUser() {
        synchronized (stateLock) {
            return findUser(users, selectedUserUuid);
        }
    }

    void selectUser(String uuid) {
        synchronized (stateLock) {
            selectedUserUuid = findUser(users, uuid) == null ? null : uuid;
            persistSelectedUserUuid(selectedUserUuid);
        }
    }

    User findUser(String uuid) {
        synchronized (stateLock) {
            return findUser(users, uuid);
        }
    }

    Weight findWeight(String userUuid, long date) {
        synchronized (stateLock) {
            for (Weight weight : weights) {
                if (date == weight.date && userUuid.equals(weight.uuid)) return weight;
            }
            return null;
        }
    }

    Goal findGoal(String userUuid, long startDate, String type) {
        synchronized (stateLock) {
            for (Goal goal : goals) {
                if (startDate == goal.start_date && userUuid.equals(goal.uuid)
                        && goal.type.toString().equals(type)) return goal;
            }
            return null;
        }
    }

    ArrayList<Weight> selectedUserWeights() {
        synchronized (stateLock) {
            ArrayList<Weight> selected = new ArrayList<>();
            if (selectedUserUuid == null) return selected;
            for (Weight weight : weights) if (selectedUserUuid.equals(weight.uuid)) selected.add(weight);
            return selected;
        }
    }

    ArrayList<Goal> selectedUserGoals() {
        synchronized (stateLock) {
            ArrayList<Goal> selected = new ArrayList<>();
            if (selectedUserUuid == null) return selected;
            for (Goal goal : goals) if (selectedUserUuid.equals(goal.uuid)) selected.add(goal);
            return selected;
        }
    }

    Weight lastSelectedUserWeight() {
        ArrayList<Weight> selected = selectedUserWeights();
        return selected.isEmpty() ? null : selected.get(0);
    }

    void upsertUser(User user, MutationCallback callback) {
        ArrayList<User> snapshot;
        synchronized (stateLock) {
            User existing = findUser(users, user.uuid);
            if (existing == null) users.add(user);
            else if (existing != user) users.set(users.indexOf(existing), user);
            sortUsers(users);
            selectedUserUuid = user.uuid;
            persistSelectedUserUuid(selectedUserUuid);
            snapshot = new ArrayList<>(users);
        }
        execute(() -> saveUsersPreservingNewerTokens(copyUsers(snapshot)), callback);
    }

    void upsertWeight(Weight weight, boolean editing, MutationCallback callback) {
        ArrayList<Weight> snapshot;
        synchronized (stateLock) {
            Weight existing = findWeight(weight.uuid, weight.date);
            if (!editing && existing == null) weights.add(weight);
            else if (existing != null && existing != weight) weights.set(weights.indexOf(existing), weight);
            Collections.sort(weights, new Weight.DateComparator());
            snapshot = new ArrayList<>(weights);
        }
        execute(() -> writeWeights(snapshot), callback);
    }

    void upsertGoal(Goal goal, MutationCallback callback) {
        ArrayList<Goal> snapshot;
        synchronized (stateLock) {
            if (!goals.contains(goal)) goals.add(goal);
            snapshot = new ArrayList<>(goals);
        }
        execute(() -> writeGoals(snapshot), callback);
    }

    void replaceWeights(List<Weight> replacement, MutationCallback callback) {
        ArrayList<Weight> snapshot;
        synchronized (stateLock) {
            weights.clear();
            weights.addAll(replacement);
            Collections.sort(weights, new Weight.DateComparator());
            snapshot = new ArrayList<>(weights);
        }
        execute(() -> writeWeights(snapshot), callback);
    }

    void deleteWeight(Weight weight, MutationCallback callback) {
        ArrayList<Weight> snapshot;
        synchronized (stateLock) {
            weights.remove(weight);
            snapshot = new ArrayList<>(weights);
        }
        execute(() -> writeWeights(snapshot), callback);
    }

    void deleteGoal(Goal goal, MutationCallback callback) {
        ArrayList<Goal> snapshot;
        synchronized (stateLock) {
            goals.remove(goal);
            snapshot = new ArrayList<>(goals);
        }
        execute(() -> writeGoals(snapshot), callback);
    }

    void deleteUser(User user, MutationCallback callback) {
        ArrayList<User> userSnapshot;
        ArrayList<Weight> weightSnapshot;
        ArrayList<Goal> goalSnapshot;
        synchronized (stateLock) {
            users.remove(user);
            for (Iterator<Weight> iterator = weights.iterator(); iterator.hasNext();) {
                if (user.uuid.equals(iterator.next().uuid)) iterator.remove();
            }
            for (Iterator<Goal> iterator = goals.iterator(); iterator.hasNext();) {
                if (user.uuid.equals(iterator.next().uuid)) iterator.remove();
            }
            if (user.uuid.equals(selectedUserUuid)) {
                selectedUserUuid = users.isEmpty() ? null : users.get(0).uuid;
                persistSelectedUserUuid(selectedUserUuid);
            }
            userSnapshot = new ArrayList<>(users);
            weightSnapshot = new ArrayList<>(weights);
            goalSnapshot = new ArrayList<>(goals);
        }
        execute(() -> {
            RepositoryResult<Void> result = saveUsersPreservingNewerTokens(copyUsers(userSnapshot));
            if (!result.isSuccess()) return result;
            result = writeWeights(weightSnapshot);
            return result.isSuccess() ? writeGoals(goalSnapshot) : result;
        }, callback);
    }

    RepositoryResult<Void> saveUsersSynchronously(List<User> users) {
        ArrayList<User> snapshot = copyUsers(users);
        return await(() -> saveUsersPreservingNewerTokens(snapshot));
    }

    RepositoryResult<Void> saveWeightsSynchronously(List<Weight> weights) {
        ArrayList<Weight> snapshot = new ArrayList<>(weights);
        return await(() -> writeWeights(snapshot));
    }

    RepositoryResult<Void> saveGoalsSynchronously(List<Goal> goals) {
        ArrayList<Goal> snapshot = new ArrayList<>(goals);
        return await(() -> writeGoals(snapshot));
    }

    private RepositoryResult<Void> writeWeights(List<Weight> weights) {
        RepositoryResult<String> encoded = weightCodec.encode(new ArrayList<>(weights));
        if (!encoded.isSuccess()) return RepositoryResult.failure(encoded.message, encoded.error);
        return historyFile.write(encoded.value);
    }

    private RepositoryResult<Void> writeGoals(List<Goal> goals) {
        RepositoryResult<String> encoded = goalCodec.encode(new ArrayList<>(goals));
        if (!encoded.isSuccess()) return RepositoryResult.failure(encoded.message, encoded.error);
        return goalsFile.write(encoded.value);
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
            RepositoryResult<Void> result = writeUsers(loaded.value);
            if (result.isSuccess()) {
                synchronized (stateLock) {
                    User stateUser = findUser(users, tokenSource.uuid);
                    if (stateUser != null) copyGarminAccessToken(tokenSource, stateUser);
                }
            }
            return result;
        });
    }

    void reloadGarminTokens(User target, MutationCallback callback) {
        execute(() -> {
            if (target == null || target.uuid == null) return RepositoryResult.success(null);
            RepositoryResult<List<User>> loaded = loadUsers();
            if (!loaded.isSuccess()) {
                return RepositoryResult.failure(loaded.message, loaded.error);
            }
            User latest = findUser(loaded.value, target.uuid);
            if (latest != null) copyGarminAccessToken(latest, target);
            return RepositoryResult.success(null);
        }, callback);
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

    private static void sortUsers(List<User> users) {
        Collator collator = Collator.getInstance();
        Collections.sort(users, (first, second) -> collator.compare(first.name, second.name));
    }

    private String resolveSelectedUserUuid(List<User> loadedUsers) {
        if (loadedUsers.isEmpty()) {
            persistSelectedUserUuid(null);
            return null;
        }
        String uuid = selectionStore == null ? null : selectionStore.selectedUuid();
        if (findUser(loadedUsers, uuid) != null) return uuid;

        String legacyName = selectionStore == null ? null : selectionStore.legacySelectedName();
        if (legacyName != null) {
            for (User user : loadedUsers) {
                if (legacyName.equals(user.name)) {
                    persistSelectedUserUuid(user.uuid);
                    return user.uuid;
                }
            }
        }
        uuid = loadedUsers.get(0).uuid;
        persistSelectedUserUuid(uuid);
        return uuid;
    }

    private void persistSelectedUserUuid(String uuid) {
        if (selectionStore != null) selectionStore.saveSelectedUuid(uuid);
    }

    interface SelectionStore {
        String selectedUuid();
        String legacySelectedName();
        void saveSelectedUuid(String uuid);
    }

    private static final class SharedPreferencesSelectionStore implements SelectionStore {
        private final SharedPreferences preferences;

        SharedPreferencesSelectionStore(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String selectedUuid() {
            return preferences.getString(SELECTED_USER_UUID_KEY, null);
        }

        @Override
        public String legacySelectedName() {
            return preferences.getString(SELECTED_USER_KEY, null);
        }

        @Override
        public void saveSelectedUuid(String uuid) {
            SharedPreferences.Editor editor = preferences.edit().remove(SELECTED_USER_KEY);
            if (uuid == null) editor.remove(SELECTED_USER_UUID_KEY);
            else editor.putString(SELECTED_USER_UUID_KEY, uuid);
            editor.apply();
        }
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

    private void execute(Callable<RepositoryResult<Void>> operation, MutationCallback callback) {
        writeExecutor.submit(() -> {
            RepositoryResult<Void> result;
            try {
                result = operation.call();
            } catch (Exception e) {
                result = RepositoryResult.failure("Could not save application data", e);
            }
            callback.onComplete(result);
        });
    }
}
