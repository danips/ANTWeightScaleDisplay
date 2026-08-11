package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Repeatable host characterization; device p95/heap thresholds remain release measurements. */
public class RepositoryScaleCharacterizationTest {
    private static final int[] SIZES = {100, 1_000, 10_000, 50_000};

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void weightAndGoalFilesRoundTripAtPlannedScales() throws Exception {
        for (int size : SIZES) {
            File directory = temporaryFolder.newFolder("records-" + size);
            AppRepository repository = new AppRepository(directory);
            try {
                List<Weight> weights = weights(size);
                List<Goal> goals = goals(Math.max(1, size / 10));
                long started = System.nanoTime();
                assertTrue(repository.saveWeightsSynchronously(weights).isSuccess());
                assertTrue(repository.saveGoalsSynchronously(goals).isSuccess());
                long written = System.nanoTime();
                RepositoryResult<List<Weight>> loadedWeights = repository.loadWeights();
                RepositoryResult<List<Goal>> loadedGoals = repository.loadGoals();
                long loaded = System.nanoTime();

                assertTrue(loadedWeights.isSuccess());
                assertTrue(loadedGoals.isSuccess());
                assertEquals(size, loadedWeights.value.size());
                assertEquals(goals.size(), loadedGoals.value.size());
                System.out.printf(java.util.Locale.US,
                        "repository-scale records=%d write_ms=%.3f read_ms=%.3f bytes=%d%n",
                        size, millis(written - started), millis(loaded - written),
                        new File(directory, "history").length()
                                + new File(directory, "goals").length());
            } finally {
                repository.close();
            }
        }
    }

    private static List<Weight> weights(int count) {
        ArrayList<Weight> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Weight weight = new Weight();
            weight.uuid = "user-" + (index % 4);
            weight.date = 1_700_000_000_000L + index * 86_400_000L;
            weight.age = 40;
            weight.height = 175;
            weight.weight = 70 + index % 20;
            weight.percentFat = 15 + index % 10;
            result.add(weight);
        }
        return result;
    }

    private static List<Goal> goals(int count) {
        ArrayList<Goal> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Goal goal = new Goal();
            goal.uuid = "user-" + (index % 4);
            goal.start_date = 1_700_000_000_000L + index * 86_400_000L;
            goal.end_date = goal.start_date + 30 * 86_400_000L;
            goal.start_value = 80;
            goal.end_value = 75;
            goal.type = Metric.WEIGHT;
            result.add(goal);
        }
        return result;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
