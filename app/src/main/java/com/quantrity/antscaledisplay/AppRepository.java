package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final AtomicJsonDataset dataset;
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
        this(filesDirectory, null, null);
    }

    AppRepository(File filesDirectory, SelectionStore selectionStore) {
        this(filesDirectory, selectionStore, null);
    }

    AppRepository(File filesDirectory, SelectionStore selectionStore,
                  AtomicJsonDataset.CommitObserver commitObserver) {
        this.filesDirectory = filesDirectory;
        usersFile = new AtomicJsonFile(new File(filesDirectory, USERS_FILE_NAME));
        historyFile = new AtomicJsonFile(new File(filesDirectory, HISTORY_FILE_NAME));
        goalsFile = new AtomicJsonFile(new File(filesDirectory, GOALS_FILE_NAME));
        dataset = new AtomicJsonDataset(filesDirectory, BackupArchive.DATA_FILES, commitObserver);
        this.selectionStore = selectionStore;
        writeExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "app-repository-writes");
            thread.setDaemon(true);
            return thread;
        });
    }

    RepositoryResult<List<User>> loadUsers() {
        RepositoryResult<Void> recovered = dataset.recover();
        if (!recovered.isSuccess()) {
            return RepositoryResult.failure(recovered.message, recovered.error);
        }
        RepositoryResult<String> read = usersFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return userCodec.decode(read.value);
    }

    RepositoryResult<List<Weight>> loadWeights() {
        RepositoryResult<Void> recovered = dataset.recover();
        if (!recovered.isSuccess()) {
            return RepositoryResult.failure(recovered.message, recovered.error);
        }
        RepositoryResult<String> read = historyFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return weightCodec.decode(read.value);
    }

    RepositoryResult<List<Goal>> loadGoals() {
        RepositoryResult<Void> recovered = dataset.recover();
        if (!recovered.isSuccess()) {
            return RepositoryResult.failure(recovered.message, recovered.error);
        }
        RepositoryResult<String> read = goalsFile.read();
        if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
        if (read.value == null || read.value.isEmpty()) return RepositoryResult.success(new ArrayList<>());
        return goalCodec.decode(read.value);
    }

    RepositoryResult<Void> reloadState() {
        return await(this::reloadStateOnExecutor);
    }

    private RepositoryResult<Void> reloadStateOnExecutor() {
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
        execute(() -> {
            ArrayList<User> candidate;
            synchronized (stateLock) {
                candidate = new ArrayList<>(users);
            }
            User existing = findUser(candidate, user.uuid);
            if (existing == null) candidate.add(user);
            else if (existing != user) candidate.set(candidate.indexOf(existing), user);
            sortUsers(candidate);

            ArrayList<User> persistedCandidate = copyUsers(candidate);
            RepositoryResult<Void> result = saveUsersPreservingNewerTokens(persistedCandidate);
            if (!result.isSuccess()) return result;

            synchronized (stateLock) {
                users.clear();
                users.addAll(persistedCandidate);
                selectedUserUuid = user.uuid;
                persistSelectedUserUuid(selectedUserUuid);
            }
            return result;
        }, callback);
    }

    void upsertWeight(Weight weight, Weight original, MutationCallback callback) {
        Weight requested = weight.copy();
        Weight expected = original == null ? null : original.copy();
        execute(() -> {
            ArrayList<Weight> candidate;
            synchronized (stateLock) {
                candidate = new ArrayList<>(weights);
            }

            if (expected == null) {
                if (findWeight(candidate, requested.uuid, requested.date) != null) {
                    return mutationConflict("A weight already exists for the selected user and date");
                }
                candidate.add(requested);
            } else {
                Weight current = findWeight(candidate, expected.uuid, expected.date);
                if (current == null || !current.equals(expected)) {
                    return mutationConflict("The weight changed while it was being edited");
                }
                Weight collision = findWeight(candidate, requested.uuid, requested.date);
                if (collision != null && collision != current) {
                    return mutationConflict("A weight already exists for the selected user and date");
                }
                candidate.set(candidate.indexOf(current), requested);
            }
            Collections.sort(candidate, new Weight.DateComparator());

            RepositoryResult<Void> result = writeWeights(candidate);
            if (!result.isSuccess()) return result;
            synchronized (stateLock) {
                weights.clear();
                weights.addAll(candidate);
            }
            return result;
        }, callback);
    }

    void upsertGoal(Goal goal, MutationCallback callback) {
        execute(() -> {
            ArrayList<Goal> candidate;
            synchronized (stateLock) {
                candidate = new ArrayList<>(goals);
            }
            if (!candidate.contains(goal)) candidate.add(goal);

            RepositoryResult<Void> result = writeGoals(candidate);
            if (!result.isSuccess()) return result;
            synchronized (stateLock) {
                goals.clear();
                goals.addAll(candidate);
            }
            return result;
        }, callback);
    }

    void replaceWeights(List<Weight> replacement, MutationCallback callback) {
        ArrayList<Weight> requested = new ArrayList<>(replacement);
        execute(() -> {
            ArrayList<Weight> candidate = new ArrayList<>(requested);
            Collections.sort(candidate, new Weight.DateComparator());

            RepositoryResult<Void> result = writeWeights(candidate);
            if (!result.isSuccess()) return result;
            synchronized (stateLock) {
                weights.clear();
                weights.addAll(candidate);
            }
            return result;
        }, callback);
    }

    void deleteWeight(Weight weight, MutationCallback callback) {
        execute(() -> {
            ArrayList<Weight> candidate;
            synchronized (stateLock) {
                candidate = new ArrayList<>(weights);
            }
            candidate.remove(weight);

            RepositoryResult<Void> result = writeWeights(candidate);
            if (!result.isSuccess()) return result;
            synchronized (stateLock) {
                weights.clear();
                weights.addAll(candidate);
            }
            return result;
        }, callback);
    }

    void deleteGoal(Goal goal, MutationCallback callback) {
        execute(() -> {
            ArrayList<Goal> candidate;
            synchronized (stateLock) {
                candidate = new ArrayList<>(goals);
            }
            candidate.remove(goal);

            RepositoryResult<Void> result = writeGoals(candidate);
            if (!result.isSuccess()) return result;
            synchronized (stateLock) {
                goals.clear();
                goals.addAll(candidate);
            }
            return result;
        }, callback);
    }

    void deleteUser(User user, MutationCallback callback) {
        execute(() -> {
            ArrayList<User> userCandidate;
            ArrayList<Weight> weightCandidate;
            ArrayList<Goal> goalCandidate;
            String previousSelection;
            synchronized (stateLock) {
                userCandidate = new ArrayList<>(users);
                weightCandidate = new ArrayList<>(weights);
                goalCandidate = new ArrayList<>(goals);
                previousSelection = selectedUserUuid;
            }
            User candidateUser = findUser(userCandidate, user.uuid);
            if (candidateUser != null) userCandidate.remove(candidateUser);
            for (Iterator<Weight> iterator = weightCandidate.iterator(); iterator.hasNext();) {
                if (user.uuid.equals(iterator.next().uuid)) iterator.remove();
            }
            for (Iterator<Goal> iterator = goalCandidate.iterator(); iterator.hasNext();) {
                if (user.uuid.equals(iterator.next().uuid)) iterator.remove();
            }
            String candidateSelection = user.uuid.equals(previousSelection)
                    ? (userCandidate.isEmpty() ? null : userCandidate.get(0).uuid)
                    : previousSelection;
            ArrayList<User> persistedUsers = copyUsers(userCandidate);

            RepositoryResult<Void> tokenMerge = preserveNewerTokens(persistedUsers);
            if (!tokenMerge.isSuccess()) return tokenMerge;
            RepositoryResult<Map<String, String>> encoded = encodeDataset(
                    persistedUsers, weightCandidate, goalCandidate);
            if (!encoded.isSuccess()) {
                return RepositoryResult.failure(encoded.message, encoded.error);
            }
            RepositoryResult<Void> result = dataset.commit(encoded.value);
            if (!result.isSuccess()) return result;

            synchronized (stateLock) {
                users.clear();
                users.addAll(persistedUsers);
                weights.clear();
                weights.addAll(weightCandidate);
                goals.clear();
                goals.addAll(goalCandidate);
                selectedUserUuid = candidateSelection;
                persistSelectedUserUuid(selectedUserUuid);
            }
            return result;
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

    RepositoryResult<Integer> createBackupSynchronously(OutputStream output) {
        return await(() -> {
            RepositoryResult<Map<String, String>> snapshot = dataset.snapshot();
            if (!snapshot.isSuccess()) {
                closeQuietly(output);
                return RepositoryResult.failure(snapshot.message, snapshot.error);
            }
            return BackupArchive.create(output, snapshot.value);
        });
    }

    RepositoryResult<Integer> restoreBackupSynchronously(InputStream input) {
        RepositoryResult<Map<String, String>> archive = BackupArchive.read(input);
        if (!archive.isSuccess()) {
            return RepositoryResult.failure(archive.message, archive.error);
        }
        return await(() -> restoreDataset(archive.value));
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
            copyGarminTokens(tokenSource, target);
            RepositoryResult<Void> result = writeUsers(loaded.value);
            if (result.isSuccess()) {
                synchronized (stateLock) {
                    User stateUser = findUser(users, tokenSource.uuid);
                    if (stateUser != null) copyGarminTokens(tokenSource, stateUser);
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
            if (latest != null) copyGarminTokens(latest, target);
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
        RepositoryResult<Void> preserved = preserveNewerTokens(users);
        return preserved.isSuccess() ? writeUsers(users) : preserved;
    }

    private RepositoryResult<Void> preserveNewerTokens(List<User> users) {
        RepositoryResult<List<User>> loaded = loadUsers();
        if (!loaded.isSuccess()) return RepositoryResult.failure(loaded.message, loaded.error);
        for (User outgoing : users) {
            User latest = findUser(loaded.value, outgoing.uuid);
            if (latest == null) continue;
            boolean latestHasDi = hasDiCredentials(latest);
            boolean outgoingHasDi = hasDiCredentials(outgoing);
            if ((latestHasDi && !outgoingHasDi)
                    || latest.garminOauth2ExpiryTimestamp
                    > outgoing.garminOauth2ExpiryTimestamp) {
                outgoing.garminOauth2Token = latest.garminOauth2Token;
                outgoing.garminOauth2ExpiryTimestamp = latest.garminOauth2ExpiryTimestamp;
                outgoing.garminDiRefreshToken = latest.garminDiRefreshToken;
                outgoing.garminDiClientId = latest.garminDiClientId;
                outgoingHasDi = latestHasDi;
            }
            if (latestHasDi || outgoingHasDi) {
                clearLegacyGarminTokens(outgoing);
            }
        }
        return RepositoryResult.success(null);
    }

    private RepositoryResult<Void> writeUsers(List<User> users) {
        RepositoryResult<String> encoded = userCodec.encode(users);
        if (!encoded.isSuccess()) return RepositoryResult.failure(encoded.message, encoded.error);
        return usersFile.write(encoded.value);
    }

    private RepositoryResult<Map<String, String>> encodeDataset(
            List<User> users, List<Weight> weights, List<Goal> goals) {
        RepositoryResult<String> encodedUsers = userCodec.encode(users);
        if (!encodedUsers.isSuccess()) {
            return RepositoryResult.failure(encodedUsers.message, encodedUsers.error);
        }
        RepositoryResult<String> encodedWeights = weightCodec.encode(weights);
        if (!encodedWeights.isSuccess()) {
            return RepositoryResult.failure(encodedWeights.message, encodedWeights.error);
        }
        RepositoryResult<String> encodedGoals = goalCodec.encode(goals);
        if (!encodedGoals.isSuccess()) {
            return RepositoryResult.failure(encodedGoals.message, encodedGoals.error);
        }
        Map<String, String> encoded = new LinkedHashMap<>();
        encoded.put(USERS_FILE_NAME, encodedUsers.value);
        encoded.put(HISTORY_FILE_NAME, encodedWeights.value);
        encoded.put(GOALS_FILE_NAME, encodedGoals.value);
        return RepositoryResult.success(encoded);
    }

    private RepositoryResult<Integer> restoreDataset(Map<String, String> archive) {
        RepositoryResult<List<User>> restoredUsers = userCodec.decode(archive.get(USERS_FILE_NAME));
        if (!restoredUsers.isSuccess()) {
            return RepositoryResult.failure(restoredUsers.message, restoredUsers.error);
        }
        RepositoryResult<List<Weight>> restoredWeights =
                weightCodec.decode(archive.get(HISTORY_FILE_NAME));
        if (!restoredWeights.isSuccess()) {
            return RepositoryResult.failure(restoredWeights.message, restoredWeights.error);
        }
        RepositoryResult<List<Goal>> restoredGoals = goalCodec.decode(archive.get(GOALS_FILE_NAME));
        if (!restoredGoals.isSuccess()) {
            return RepositoryResult.failure(restoredGoals.message, restoredGoals.error);
        }

        RepositoryResult<Void> committed = dataset.commit(archive);
        if (!committed.isSuccess()) {
            return RepositoryResult.failure(committed.message, committed.error);
        }

        ArrayList<User> newUsers = new ArrayList<>(restoredUsers.value);
        ArrayList<Weight> newWeights = new ArrayList<>(restoredWeights.value);
        ArrayList<Goal> newGoals = new ArrayList<>(restoredGoals.value);
        sortUsers(newUsers);
        Collections.sort(newWeights, new Weight.DateComparator());
        synchronized (stateLock) {
            users.clear();
            users.addAll(newUsers);
            weights.clear();
            weights.addAll(newWeights);
            goals.clear();
            goals.addAll(newGoals);
            selectedUserUuid = resolveSelectedUserUuid(users);
            stateLoaded = true;
        }
        return RepositoryResult.success(BackupArchive.DATA_FILES.size());
    }

    private static ArrayList<User> copyUsers(List<User> users) {
        UserJsonCodec codec = new UserJsonCodec();
        RepositoryResult<String> encoded = codec.encode(users);
        if (!encoded.isSuccess()) return new ArrayList<>(users);
        RepositoryResult<List<User>> decoded = codec.decode(encoded.value);
        return decoded.isSuccess() ? new ArrayList<>(decoded.value) : new ArrayList<>(users);
    }

    private static RepositoryResult<Void> mutationConflict(String message) {
        return RepositoryResult.failure(message, new IllegalStateException(message));
    }

    private static User findUser(List<User> users, String uuid) {
        if (uuid == null) return null;
        for (User user : users) if (uuid.equals(user.uuid)) return user;
        return null;
    }

    private static Weight findWeight(List<Weight> weights, String userUuid, long date) {
        if (userUuid == null) return null;
        for (Weight weight : weights) {
            if (date == weight.date && userUuid.equals(weight.uuid)) return weight;
        }
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

    private static void copyGarminTokens(User source, User target) {
        target.garminOauth1Token = source.garminOauth1Token;
        target.garminOauth1TokenSecret = source.garminOauth1TokenSecret;
        target.garminOauth1MfaToken = source.garminOauth1MfaToken;
        target.garminOauth1MfaExpirationTimestamp =
                source.garminOauth1MfaExpirationTimestamp;
        target.garminOauth2Token = source.garminOauth2Token;
        target.garminOauth2ExpiryTimestamp = source.garminOauth2ExpiryTimestamp;
        target.garminDiRefreshToken = source.garminDiRefreshToken;
        target.garminDiClientId = source.garminDiClientId;
    }

    private static boolean hasDiCredentials(User user) {
        return user.garminDiRefreshToken != null && !user.garminDiRefreshToken.isEmpty()
                && user.garminDiClientId != null && !user.garminDiClientId.isEmpty();
    }

    private static void clearLegacyGarminTokens(User user) {
        user.garminOauth1Token = null;
        user.garminOauth1TokenSecret = null;
        user.garminOauth1MfaToken = null;
        user.garminOauth1MfaExpirationTimestamp = -1;
    }

    private <T> RepositoryResult<T> await(Callable<RepositoryResult<T>> operation) {
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

    private static void closeQuietly(OutputStream output) {
        if (output == null) return;
        try {
            output.close();
        } catch (Exception ignored) { }
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
