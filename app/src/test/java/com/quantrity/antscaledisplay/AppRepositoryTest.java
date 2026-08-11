package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AppRepositoryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AppRepository repository;

    @Before
    public void setUp() {
        repository = new AppRepository(temporaryFolder.getRoot());
    }

    @After
    public void tearDown() {
        repository.close();
    }

    @Test
    public void loadsAndSavesAllExistingFixtureFormats() throws Exception {
        writeFixture("users", "users.json");
        writeFixture("history", "history.json");
        writeFixture("goals", "goals.json");

        RepositoryResult<List<User>> users = repository.loadUsers();
        RepositoryResult<List<Weight>> weights = repository.loadWeights();
        RepositoryResult<List<Goal>> goals = repository.loadGoals();

        assertTrue(users.isSuccess());
        assertTrue(weights.isSuccess());
        assertTrue(goals.isSuccess());
        assertEquals(2, users.value.size());
        assertEquals(2, weights.value.size());
        assertEquals(2, goals.value.size());

        assertTrue(repository.saveUsersSynchronously(users.value).isSuccess());
        assertTrue(repository.saveWeightsSynchronously(weights.value).isSuccess());
        assertTrue(repository.saveGoalsSynchronously(goals.value).isSuccess());
        assertEquals(2, repository.loadUsers().value.size());
        assertEquals(2, repository.loadWeights().value.size());
        assertEquals(2, repository.loadGoals().value.size());
    }

    @Test
    public void restoreDecodesEverySchemaBeforeReplacingDataset() throws Exception {
        writeFixture("users", "users.json");
        writeFixture("history", "history.json");
        writeFixture("goals", "goals.json");
        assertTrue(repository.reloadState().isSuccess());
        String originalUsers = readFile("users");
        String originalHistory = readFile("history");
        String originalGoals = readFile("goals");
        Map<String, String> invalid = new java.util.LinkedHashMap<>();
        invalid.put("users", "[{}]");
        invalid.put("history", FixtureLoader.load("history.json"));
        invalid.put("goals", FixtureLoader.load("goals.json"));

        RepositoryResult<Integer> result = repository.restoreBackupSynchronously(
                new ByteArrayInputStream(archive(invalid)));

        assertFalse(result.isSuccess());
        assertEquals("Could not decode users", result.message);
        assertEquals(originalUsers, readFile("users"));
        assertEquals(originalHistory, readFile("history"));
        assertEquals(originalGoals, readFile("goals"));
        assertEquals(2, repository.usersSnapshot().size());
    }

    @Test
    public void restoreCommitsAndPublishesCompleteDataset() throws Exception {
        Map<String, String> restored = fixtureDataset();

        RepositoryResult<Integer> result = repository.restoreBackupSynchronously(
                new ByteArrayInputStream(archive(restored)));

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(3), result.value);
        assertEquals(2, repository.usersSnapshot().size());
        assertEquals(2, repository.weightsSnapshot().size());
        assertEquals(2, repository.goalsSnapshot().size());
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(2, repository.usersSnapshot().size());
    }

    @Test
    public void backupRunsAfterEarlierQueuedMutationAndCapturesItsGeneration() throws Exception {
        assertTrue(repository.saveUsersSynchronously(Collections.emptyList()).isSuccess());
        assertTrue(repository.saveWeightsSynchronously(Collections.emptyList()).isSuccess());
        assertTrue(repository.saveGoalsSynchronously(Collections.emptyList()).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight added = weight(456, 75);
        CountDownLatch mutationCompleted = new CountDownLatch(1);
        repository.upsertWeight(added, null, result -> mutationCompleted.countDown());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        RepositoryResult<Integer> result = repository.createBackupSynchronously(output);

        assertTrue(result.isSuccess());
        assertTrue(mutationCompleted.await(5, java.util.concurrent.TimeUnit.SECONDS));
        RepositoryResult<Map<String, String>> archived = BackupArchive.read(
                new ByteArrayInputStream(output.toByteArray()));
        assertTrue(archived.isSuccess());
        assertEquals(1, new WeightJsonCodec().decode(archived.value.get("history")).value.size());
    }

    @Test
    public void deleteUserRollsBackAtEveryDatasetCommitPosition() throws Exception {
        for (int failedIndex = 0; failedIndex < BackupArchive.DATA_FILES.size(); failedIndex++) {
            File directory = temporaryFolder.newFolder("delete-failure-" + failedIndex);
            writeFixture(directory, "users", "users.json");
            writeFixture(directory, "history", "history.json");
            writeFixture(directory, "goals", "goals.json");
            int position = failedIndex;
            AppRepository failingRepository = new AppRepository(directory, null, (name, index) -> {
                if (index == position) throw new Exception("injected failure at " + name);
            });
            try {
                assertTrue(failingRepository.reloadState().isSuccess());
                User removed = failingRepository.usersSnapshot().get(0);

                RepositoryResult<Void> result = deleteUserAndWait(failingRepository, removed);

                assertFalse(result.isSuccess());
                assertEquals(2, failingRepository.loadUsers().value.size());
                assertEquals(2, failingRepository.loadWeights().value.size());
                assertEquals(2, failingRepository.loadGoals().value.size());
                assertTrue(failingRepository.reloadState().isSuccess());
                assertEquals(2, failingRepository.usersSnapshot().size());
            } finally {
                failingRepository.close();
            }
        }
    }

    @Test
    public void savingGoalsSortsStoredCopyWithoutMutatingCallerList() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("goals.json"));
        Goal earlier = new Goal(fixture.getJSONObject(0));
        Goal later = new Goal(fixture.getJSONObject(1));
        if (earlier.end_date > later.end_date) {
            Goal swap = earlier;
            earlier = later;
            later = swap;
        }
        ArrayList<Goal> callerList = new ArrayList<>(Arrays.asList(earlier, later));

        assertTrue(repository.saveGoalsSynchronously(callerList).isSuccess());

        assertEquals(earlier, callerList.get(0));
        assertEquals(later, callerList.get(1));
        List<Goal> stored = repository.loadGoals().value;
        assertEquals(later.end_date, stored.get(0).end_date);
        assertEquals(earlier.end_date, stored.get(1).end_date);
    }

    @Test
    public void concurrentProfileSaveAndTokenRenewalPreserveBothChanges() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("users.json"));
        User original = new User(fixture.getJSONObject(0));
        assertTrue(repository.saveUsersSynchronously(Arrays.asList(original)).isSuccess());

        User editedProfile = new User(fixture.getJSONObject(0));
        editedProfile.name = "Edited profile";
        User renewedTokens = new User(fixture.getJSONObject(0));
        renewedTokens.garminOauth2Token = "renewed-oauth2";
        renewedTokens.garminOauth2ExpiryTimestamp += 10_000;
        renewedTokens.garminDiRefreshToken = "rotated-refresh";
        clearLegacyGarminTokens(renewedTokens);

        ExecutorService callers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<RepositoryResult<Void>> profileResult = callers.submit(() -> {
            start.await();
            return repository.saveUsersSynchronously(Arrays.asList(editedProfile));
        });
        Future<RepositoryResult<Void>> tokenResult = callers.submit(() -> {
            start.await();
            return repository.updateGarminTokensSynchronously(renewedTokens);
        });
        start.countDown();

        assertTrue(profileResult.get().isSuccess());
        assertTrue(tokenResult.get().isSuccess());
        callers.shutdownNow();

        User stored = repository.loadUsers().value.get(0);
        assertEquals("Edited profile", stored.name);
        assertNull(stored.garminOauth1Token);
        assertNull(stored.garminOauth1TokenSecret);
        assertEquals("renewed-oauth2", stored.garminOauth2Token);
        assertEquals(renewedTokens.garminOauth2ExpiryTimestamp, stored.garminOauth2ExpiryTimestamp);
        assertEquals("rotated-refresh", stored.garminDiRefreshToken);
    }

    @Test
    public void malformedJsonReturnsExplicitFailure() throws Exception {
        Files.write(new File(temporaryFolder.getRoot(), "users").toPath(),
                "not-json".getBytes(StandardCharsets.UTF_8));

        RepositoryResult<List<User>> result = repository.loadUsers();

        assertFalse(result.isSuccess());
        assertEquals("Could not decode users", result.message);
    }

    @Test
    public void staleProfileSaveCannotOverwriteNewerTokens() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("users.json"));
        User original = new User(fixture.getJSONObject(0));
        assertTrue(repository.saveUsersSynchronously(Arrays.asList(original)).isSuccess());

        User staleProfile = new User(fixture.getJSONObject(0));
        staleProfile.name = "Profile saved later";
        staleProfile.garminOauth2ExpiryTimestamp += 30_000;
        staleProfile.garminDiRefreshToken = null;
        staleProfile.garminDiClientId = null;
        User renewedTokens = new User(fixture.getJSONObject(0));
        renewedTokens.garminOauth2Token = "newest-token";
        renewedTokens.garminOauth2ExpiryTimestamp += 20_000;
        renewedTokens.garminDiRefreshToken = "newest-refresh";
        clearLegacyGarminTokens(renewedTokens);
        assertTrue(repository.updateGarminTokensSynchronously(renewedTokens).isSuccess());

        assertTrue(repository.saveUsersSynchronously(Arrays.asList(staleProfile)).isSuccess());

        User stored = repository.loadUsers().value.get(0);
        assertEquals("Profile saved later", stored.name);
        assertEquals("newest-token", stored.garminOauth2Token);
        assertEquals(renewedTokens.garminOauth2ExpiryTimestamp, stored.garminOauth2ExpiryTimestamp);
        assertEquals("newest-refresh", stored.garminDiRefreshToken);
        assertNull(stored.garminOauth1Token);
        assertNull(stored.garminOauth1TokenSecret);
    }

    @Test
    public void stateSurvivesRecreationAndMigratesLegacySelectedUserName() throws Exception {
        writeFixture("users", "users.json");
        writeFixture("history", "history.json");
        writeFixture("goals", "goals.json");
        FakeSelectionStore selection = new FakeSelectionStore();
        selection.legacyName = "Sample Legacy User";
        repository.close();
        repository = new AppRepository(temporaryFolder.getRoot(), selection);

        assertTrue(repository.reloadState().isSuccess());
        assertEquals("user-legacy-002", repository.selectedUser().uuid);
        assertEquals("user-legacy-002", selection.uuid);

        ArrayList<User> snapshot = repository.usersSnapshot();
        snapshot.clear();
        assertEquals(2, repository.usersSnapshot().size());

        repository.selectUser("user-current-001");
        repository.close();
        repository = new AppRepository(temporaryFolder.getRoot(), selection);
        assertTrue(repository.reloadState().isSuccess());

        assertEquals("user-current-001", repository.selectedUser().uuid);
        Weight weight = repository.weightsSnapshot().get(0);
        Goal goal = repository.goalsSnapshot().get(0);
        assertEquals(weight, repository.findWeight(weight.uuid, weight.date));
        assertEquals(goal, repository.findGoal(goal.uuid, goal.start_date, goal.type.toString()));
    }

    @Test
    public void selectedRecordIndexesTrackReplacementDeletionAndProfileChanges() throws Exception {
        writeFixture("users", "users.json");
        writeFixture("history", "history.json");
        writeFixture("goals", "goals.json");
        assertTrue(repository.reloadState().isSuccess());
        List<User> users = repository.usersSnapshot();
        String firstUuid = users.get(0).uuid;
        String secondUuid = users.get(1).uuid;
        for (User user : users) {
            repository.selectUser(user.uuid);
            ArrayList<Goal> expected = new ArrayList<>();
            for (Goal goal : repository.goalsSnapshot()) {
                if (user.uuid.equals(goal.uuid)) expected.add(goal);
            }
            assertEquals(expected, repository.selectedUserGoals());
        }
        repository.selectUser(firstUuid);
        if (!repository.selectedUserGoals().isEmpty()) {
            Goal removedGoal = repository.selectedUserGoals().get(0);
            assertTrue(deleteGoalAndWait(removedGoal).isSuccess());
            assertFalse(repository.selectedUserGoals().contains(removedGoal));
            assertNull(repository.findGoal(removedGoal.uuid, removedGoal.start_date,
                    removedGoal.type.toString()));
        }
        Weight firstNewest = weightFor(firstUuid, 30, 73);
        Weight firstOlder = weightFor(firstUuid, 20, 72);
        Weight second = weightFor(secondUuid, 10, 71);

        assertTrue(replaceWeightsAndWait(
                Arrays.asList(second, firstOlder, firstNewest)).isSuccess());
        repository.selectUser(firstUuid);
        assertEquals(Arrays.asList(firstNewest, firstOlder), repository.selectedUserWeights());
        assertEquals(firstNewest, repository.lastSelectedUserWeight());
        assertEquals(firstOlder, repository.findWeight(firstUuid, 20));

        assertTrue(deleteWeightAndWait(firstNewest).isSuccess());
        assertEquals(Collections.singletonList(firstOlder), repository.selectedUserWeights());
        assertEquals(firstOlder, repository.lastSelectedUserWeight());

        repository.selectUser(secondUuid);
        assertEquals(Collections.singletonList(second), repository.selectedUserWeights());
        assertEquals(second, repository.findWeight(secondUuid, 10));
        assertNull(repository.findWeight(firstUuid, 30));
    }

    @Test
    public void asynchronousMutationReportsSuccessAndPersistsBeforeCallback() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        Weight weight = new Weight();
        weight.uuid = "user";
        weight.date = 123;
        weight.weight = 75;
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.upsertWeight(weight, null, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(callbackResult[0].isSuccess());
        assertEquals(1, repository.loadWeights().value.size());
    }

    @Test
    public void failedAddLeavesMemoryAndDiskAtCommittedState() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        blockWrites("history");
        Weight weight = new Weight();
        weight.uuid = "user";
        weight.date = 456;
        weight.weight = 80;
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.upsertWeight(weight, null, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(callbackResult[0].isSuccess());
        assertTrue(repository.weightsSnapshot().isEmpty());
        unblockWrites("history");
        assertTrue(repository.loadWeights().value.isEmpty());
        assertTrue(repository.reloadState().isSuccess());
        assertTrue(repository.weightsSnapshot().isEmpty());
    }

    @Test
    public void failedEditLeavesMemoryAndDiskAtCommittedState() throws Exception {
        Weight original = weight(456, 75);
        assertTrue(repository.saveWeightsSynchronously(Collections.singletonList(original)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight edited = weight(456, 80);
        blockWrites("history");
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.upsertWeight(edited, original, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(callbackResult[0].isSuccess());
        assertEquals(75, repository.weightsSnapshot().get(0).weight, 0);
        unblockWrites("history");
        assertEquals(75, repository.loadWeights().value.get(0).weight, 0);
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(75, repository.weightsSnapshot().get(0).weight, 0);
    }

    @Test
    public void detachedEditCanChangeDateAndBeDiscardedWithoutMutatingState() throws Exception {
        Weight original = weight(456, 75);
        assertTrue(repository.saveWeightsSynchronously(Collections.singletonList(original)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());

        Weight draft = repository.findWeight("user", 456).copy();
        draft.date = 789;
        draft.weight = 80;

        Weight committed = repository.findWeight("user", 456);
        assertEquals(456, committed.date);
        assertEquals(75, committed.weight, 0);
        assertNull(repository.findWeight("user", 789));
        assertEquals(456, repository.loadWeights().value.get(0).date);
    }

    @Test
    public void editReplacesOriginalKeyAfterDateChange() throws Exception {
        Weight original = weight(456, 75);
        assertTrue(repository.saveWeightsSynchronously(Collections.singletonList(original)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight baseline = repository.findWeight("user", 456).copy();
        Weight draft = baseline.copy();
        draft.date = 789;
        draft.weight = 80;

        RepositoryResult<Void> result = upsertWeightAndWait(draft, baseline);

        assertTrue(result.isSuccess());
        assertNull(repository.findWeight("user", 456));
        assertEquals(80, repository.findWeight("user", 789).weight, 0);
        assertNull(findWeight(repository.loadWeights().value, "user", 456));
        assertEquals(80, findWeight(repository.loadWeights().value, "user", 789).weight, 0);
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(80, repository.findWeight("user", 789).weight, 0);
    }

    @Test
    public void editRejectsCollisionAtChangedKey() throws Exception {
        Weight original = weight(456, 75);
        Weight collision = weight(789, 70);
        assertTrue(repository.saveWeightsSynchronously(Arrays.asList(original, collision)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight baseline = repository.findWeight("user", 456).copy();
        Weight draft = baseline.copy();
        draft.date = 789;
        draft.weight = 80;

        RepositoryResult<Void> result = upsertWeightAndWait(draft, baseline);

        assertFalse(result.isSuccess());
        assertEquals("A weight already exists for the selected user and date", result.message);
        assertEquals(2, repository.weightsSnapshot().size());
        assertEquals(75, repository.findWeight("user", 456).weight, 0);
        assertEquals(70, repository.findWeight("user", 789).weight, 0);
        assertEquals(2, repository.loadWeights().value.size());
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(75, repository.findWeight("user", 456).weight, 0);
    }

    @Test
    public void editRejectsConcurrentChangeToOriginalRecord() throws Exception {
        Weight original = weight(456, 75);
        assertTrue(repository.saveWeightsSynchronously(Collections.singletonList(original)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight baseline = repository.findWeight("user", 456).copy();
        Weight concurrent = baseline.copy();
        concurrent.weight = 76;
        assertTrue(upsertWeightAndWait(concurrent, baseline).isSuccess());
        Weight staleDraft = baseline.copy();
        staleDraft.weight = 80;

        RepositoryResult<Void> result = upsertWeightAndWait(staleDraft, baseline);

        assertFalse(result.isSuccess());
        assertEquals("The weight changed while it was being edited", result.message);
        assertEquals(76, repository.findWeight("user", 456).weight, 0);
        assertEquals(76, repository.loadWeights().value.get(0).weight, 0);
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(76, repository.findWeight("user", 456).weight, 0);
    }

    @Test
    public void failedDeleteLeavesMemoryAndDiskAtCommittedState() throws Exception {
        Weight original = weight(456, 75);
        assertTrue(repository.saveWeightsSynchronously(Collections.singletonList(original)).isSuccess());
        assertTrue(repository.reloadState().isSuccess());
        Weight stored = repository.weightsSnapshot().get(0);
        blockWrites("history");
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.deleteWeight(stored, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(callbackResult[0].isSuccess());
        assertEquals(1, repository.weightsSnapshot().size());
        unblockWrites("history");
        assertEquals(1, repository.loadWeights().value.size());
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(1, repository.weightsSnapshot().size());
    }

    @Test
    public void encodingFailureIsReturnedExplicitly() {
        RepositoryResult<Void> result = repository.saveWeightsSynchronously(
                Collections.singletonList(null));

        assertFalse(result.isSuccess());
        assertEquals("Could not encode weight history", result.message);
    }

    @Test
    public void asynchronousMutationsAreSerializedInSubmissionOrder() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        Weight first = new Weight();
        first.uuid = "user";
        first.date = 1;
        first.weight = 70;
        Weight second = new Weight();
        second.uuid = "user";
        second.date = 2;
        second.weight = 71;
        CountDownLatch completed = new CountDownLatch(2);
        List<Long> callbackOrder = Collections.synchronizedList(new ArrayList<>());

        repository.upsertWeight(first, null, result -> {
            callbackOrder.add(1L);
            completed.countDown();
        });
        repository.upsertWeight(second, null, result -> {
            callbackOrder.add(2L);
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1L, 2L), callbackOrder);
        assertEquals(2, repository.loadWeights().value.size());
    }

    @Test
    public void queuedMutationExcludesEarlierFailedCandidate() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        Weight failed = weight(1, 70);
        Weight accepted = weight(2, 71);
        blockWrites("history");
        CountDownLatch completed = new CountDownLatch(2);
        RepositoryResult<?>[] callbackResults = new RepositoryResult<?>[2];
        int[] stateSizes = new int[2];
        int[] diskSizes = new int[2];

        repository.upsertWeight(failed, null, result -> {
            callbackResults[0] = result;
            stateSizes[0] = repository.weightsSnapshot().size();
            unblockWrites("history");
            diskSizes[0] = repository.loadWeights().value.size();
            completed.countDown();
        });
        repository.upsertWeight(accepted, null, result -> {
            callbackResults[1] = result;
            stateSizes[1] = repository.weightsSnapshot().size();
            diskSizes[1] = repository.loadWeights().value.size();
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(callbackResults[0].isSuccess());
        assertTrue(callbackResults[1].isSuccess());
        assertEquals(0, stateSizes[0]);
        assertEquals(0, diskSizes[0]);
        assertEquals(1, stateSizes[1]);
        assertEquals(1, diskSizes[1]);
        assertEquals(accepted.date, repository.weightsSnapshot().get(0).date);
        assertEquals(accepted.date, repository.loadWeights().value.get(0).date);
        assertTrue(repository.reloadState().isSuccess());
        assertEquals(accepted.date, repository.weightsSnapshot().get(0).date);
    }

    private void writeFixture(String filename, String fixture) throws Exception {
        writeFixture(temporaryFolder.getRoot(), filename, fixture);
    }

    private static void writeFixture(File directory, String filename, String fixture)
            throws Exception {
        Files.write(new File(directory, filename).toPath(),
                FixtureLoader.load(fixture).getBytes(StandardCharsets.UTF_8));
    }

    private String readFile(String filename) throws Exception {
        return new String(Files.readAllBytes(
                new File(temporaryFolder.getRoot(), filename).toPath()), StandardCharsets.UTF_8);
    }

    private static Map<String, String> fixtureDataset() throws Exception {
        Map<String, String> data = new java.util.LinkedHashMap<>();
        data.put("users", FixtureLoader.load("users.json"));
        data.put("history", FixtureLoader.load("history.json"));
        data.put("goals", FixtureLoader.load("goals.json"));
        return data;
    }

    private static byte[] archive(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static RepositoryResult<Void> deleteUserAndWait(
            AppRepository targetRepository, User user) throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> callbackResult = new AtomicReference<>();
        targetRepository.deleteUser(user, result -> {
            callbackResult.set(result);
            completed.countDown();
        });
        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return callbackResult.get();
    }

    private RepositoryResult<Void> upsertWeightAndWait(Weight weight, Weight original)
            throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> callbackResult = new AtomicReference<>();
        repository.upsertWeight(weight, original, result -> {
            callbackResult.set(result);
            completed.countDown();
        });
        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return callbackResult.get();
    }

    private RepositoryResult<Void> replaceWeightsAndWait(List<Weight> weights)
            throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> callbackResult = new AtomicReference<>();
        repository.replaceWeights(weights, result -> {
            callbackResult.set(result);
            completed.countDown();
        });
        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return callbackResult.get();
    }

    private RepositoryResult<Void> deleteWeightAndWait(Weight weight)
            throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> callbackResult = new AtomicReference<>();
        repository.deleteWeight(weight, result -> {
            callbackResult.set(result);
            completed.countDown();
        });
        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return callbackResult.get();
    }

    private RepositoryResult<Void> deleteGoalAndWait(Goal goal)
            throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> callbackResult = new AtomicReference<>();
        repository.deleteGoal(goal, result -> {
            callbackResult.set(result);
            completed.countDown();
        });
        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        return callbackResult.get();
    }

    private static Weight findWeight(List<Weight> weights, String uuid, long date) {
        for (Weight weight : weights) {
            if (date == weight.date && uuid.equals(weight.uuid)) return weight;
        }
        return null;
    }

    private static Weight weight(long date, double value) {
        return weightFor("user", date, value);
    }

    private static Weight weightFor(String uuid, long date, double value) {
        Weight weight = new Weight();
        weight.uuid = uuid;
        weight.date = date;
        weight.weight = value;
        return weight;
    }

    private void blockWrites(String filename) throws Exception {
        File temporaryDirectory = new File(temporaryFolder.getRoot(), filename + ".tmp");
        assertTrue(temporaryDirectory.mkdir());
        Files.write(new File(temporaryDirectory, "blocker").toPath(), new byte[]{1});
    }

    private void unblockWrites(String filename) {
        try {
            File temporaryDirectory = new File(temporaryFolder.getRoot(), filename + ".tmp");
            Files.delete(new File(temporaryDirectory, "blocker").toPath());
            Files.delete(temporaryDirectory.toPath());
        } catch (Exception e) {
            throw new AssertionError("Could not remove write blocker", e);
        }
    }

    private static void clearLegacyGarminTokens(User user) {
        user.garminOauth1Token = null;
        user.garminOauth1TokenSecret = null;
        user.garminOauth1MfaToken = null;
        user.garminOauth1MfaExpirationTimestamp = -1;
    }

    private static final class FakeSelectionStore implements AppRepository.SelectionStore {
        String uuid;
        String legacyName;

        @Override
        public String selectedUuid() {
            return uuid;
        }

        @Override
        public String legacySelectedName() {
            return legacyName;
        }

        @Override
        public void saveSelectedUuid(String uuid) {
            this.uuid = uuid;
            legacyName = null;
        }
    }
}
