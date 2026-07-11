package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AtomicJsonFileTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writeReplacesFileAndRemovesWorkingFiles() throws Exception {
        File target = temporaryFolder.newFile("users");
        write(target, "[\"old\"]");
        AtomicJsonFile file = new AtomicJsonFile(target);

        assertTrue(file.write("[\"new\"]").isSuccess());
        assertEquals("[\"new\"]", file.read().value);
        assertFalse(new File(target.getPath() + ".tmp").exists());
        assertFalse(new File(target.getPath() + ".del").exists());
    }

    @Test
    public void readDiscardsStaleWorkingFilesWhenCommittedFileExists() throws Exception {
        File target = temporaryFolder.newFile("history");
        File temporary = new File(target.getPath() + ".tmp");
        File backup = new File(target.getPath() + ".del");
        write(target, "[\"committed\"]");
        write(temporary, "[\"stale-new\"]");
        write(backup, "[\"stale-old\"]");

        RepositoryResult<String> result = new AtomicJsonFile(target).read();

        assertTrue(result.isSuccess());
        assertEquals("[\"committed\"]", result.value);
        assertFalse(temporary.exists());
        assertFalse(backup.exists());
    }

    @Test
    public void readPromotesCompletedTemporaryFileAfterInterruptedReplacement() throws Exception {
        File target = new File(temporaryFolder.getRoot(), "goals");
        File temporary = new File(target.getPath() + ".tmp");
        File backup = new File(target.getPath() + ".del");
        write(temporary, "[\"new\"]");
        write(backup, "[\"old\"]");

        RepositoryResult<String> result = new AtomicJsonFile(target).read();

        assertTrue(result.isSuccess());
        assertEquals("[\"new\"]", result.value);
        assertTrue(target.exists());
        assertFalse(temporary.exists());
        assertFalse(backup.exists());
    }

    @Test
    public void readRestoresBackupWhenReplacementWasNotCreated() throws Exception {
        File target = new File(temporaryFolder.getRoot(), "users");
        File backup = new File(target.getPath() + ".del");
        write(backup, "[\"old\"]");

        RepositoryResult<String> result = new AtomicJsonFile(target).read();

        assertTrue(result.isSuccess());
        assertEquals("[\"old\"]", result.value);
        assertTrue(target.exists());
        assertFalse(backup.exists());
    }

    @Test
    public void readRejectsIncompleteTemporaryFileAndRestoresBackup() throws Exception {
        File target = new File(temporaryFolder.getRoot(), "history-incomplete");
        File temporary = new File(target.getPath() + ".tmp");
        File backup = new File(target.getPath() + ".del");
        write(temporary, "[{\"incomplete\"");
        write(backup, "[\"old\"]");

        RepositoryResult<String> result = new AtomicJsonFile(target).read();

        assertTrue(result.isSuccess());
        assertEquals("[\"old\"]", result.value);
        assertFalse(temporary.exists());
        assertFalse(backup.exists());
    }

    private static void write(File file, String value) throws Exception {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }
}
