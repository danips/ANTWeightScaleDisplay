package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        assertEquals(original.garminOauth1Token, stored.garminOauth1Token);
        assertEquals("renewed-oauth2", stored.garminOauth2Token);
        assertEquals(renewedTokens.garminOauth2ExpiryTimestamp, stored.garminOauth2ExpiryTimestamp);
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
        User renewedTokens = new User(fixture.getJSONObject(0));
        renewedTokens.garminOauth2Token = "newest-token";
        renewedTokens.garminOauth2ExpiryTimestamp += 20_000;
        assertTrue(repository.updateGarminTokensSynchronously(renewedTokens).isSuccess());

        assertTrue(repository.saveUsersSynchronously(Arrays.asList(staleProfile)).isSuccess());

        User stored = repository.loadUsers().value.get(0);
        assertEquals("Profile saved later", stored.name);
        assertEquals("newest-token", stored.garminOauth2Token);
        assertEquals(renewedTokens.garminOauth2ExpiryTimestamp, stored.garminOauth2ExpiryTimestamp);
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
    public void asynchronousMutationReportsSuccessAndPersistsBeforeCallback() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        Weight weight = new Weight();
        weight.uuid = "user";
        weight.date = 123;
        weight.weight = 75;
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.upsertWeight(weight, false, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(callbackResult[0].isSuccess());
        assertEquals(1, repository.loadWeights().value.size());
    }

    @Test
    public void asynchronousMutationReportsDiskFailureAndRetainsMemorySnapshot() throws Exception {
        assertTrue(repository.reloadState().isSuccess());
        File historyDirectory = new File(temporaryFolder.getRoot(), "history");
        assertTrue(historyDirectory.mkdir());
        Files.write(new File(historyDirectory, "blocker").toPath(), new byte[]{1});
        Weight weight = new Weight();
        weight.uuid = "user";
        weight.date = 456;
        weight.weight = 80;
        CountDownLatch completed = new CountDownLatch(1);
        RepositoryResult<?>[] callbackResult = new RepositoryResult<?>[1];

        repository.upsertWeight(weight, false, result -> {
            callbackResult[0] = result;
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(callbackResult[0].isSuccess());
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

        repository.upsertWeight(first, false, result -> {
            callbackOrder.add(1L);
            completed.countDown();
        });
        repository.upsertWeight(second, false, result -> {
            callbackOrder.add(2L);
            completed.countDown();
        });

        assertTrue(completed.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1L, 2L), callbackOrder);
        assertEquals(2, repository.loadWeights().value.size());
    }

    private void writeFixture(String filename, String fixture) throws Exception {
        Files.write(new File(temporaryFolder.getRoot(), filename).toPath(),
                FixtureLoader.load(fixture).getBytes(StandardCharsets.UTF_8));
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
