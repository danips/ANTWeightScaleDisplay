package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AtomicJsonDatasetTest {
    private static final List<String> NAMES = Arrays.asList("users", "history", "goals");

    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void failureAtEveryCommitPositionRollsBackWholeGeneration() throws Exception {
        for (int failedIndex = 0; failedIndex < NAMES.size(); failedIndex++) {
            File directory = temporaryFolder.newFolder("failure-" + failedIndex);
            Map<String, String> original = dataset("old");
            write(directory, original);
            int position = failedIndex;
            AtomicJsonDataset store = new AtomicJsonDataset(directory, NAMES, (name, index) -> {
                if (index == position) throw new Exception("injected failure at " + name);
            });

            RepositoryResult<Void> result = store.commit(dataset("new"));

            assertFalse(result.isSuccess());
            assertEquals(original, new AtomicJsonDataset(directory, NAMES).snapshot().value);
        }
    }

    @Test
    public void restartRecoversJournalLeftByInterruptedCommit() throws Exception {
        for (int interruptedIndex = 0; interruptedIndex < NAMES.size(); interruptedIndex++) {
            File directory = temporaryFolder.newFolder("interrupted-" + interruptedIndex);
            Map<String, String> original = dataset("old");
            write(directory, original);
            int position = interruptedIndex;
            AtomicJsonDataset interrupted = new AtomicJsonDataset(directory, NAMES, (name, index) -> {
                if (index == position) throw new SimulatedProcessDeath();
            });

            try {
                interrupted.commit(dataset("new"));
                fail("Expected simulated process death");
            } catch (SimulatedProcessDeath expected) {
                assertTrue(new File(directory, ".dataset-transaction").isFile());
            }

            AtomicJsonDataset restarted = new AtomicJsonDataset(directory, NAMES);
            assertEquals(original, restarted.snapshot().value);
            assertFalse(new File(directory, ".dataset-transaction").exists());
        }
    }

    private static Map<String, String> dataset(String generation) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String name : NAMES) data.put(name, "[\"" + generation + "-" + name + "\"]");
        return data;
    }

    private static void write(File directory, Map<String, String> data) throws Exception {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Files.write(new File(directory, entry.getKey()).toPath(),
                    entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class SimulatedProcessDeath extends Error { }
}
