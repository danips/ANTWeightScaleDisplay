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
        assertTrue(repository.saveWeights(weights.value).get().isSuccess());
        assertTrue(repository.saveGoals(goals.value).get().isSuccess());
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

        assertTrue(repository.saveGoals(callerList).get().isSuccess());

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

    private void writeFixture(String filename, String fixture) throws Exception {
        Files.write(new File(temporaryFolder.getRoot(), filename).toPath(),
                FixtureLoader.load(fixture).getBytes(StandardCharsets.UTF_8));
    }
}
