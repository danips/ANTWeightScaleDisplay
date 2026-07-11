package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupArchiveTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void restoresRecognizedJsonFiles() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "[{\"uuid\":\"user-one\"}]");
        entries.put("history", "[]");

        RepositoryResult<Integer> result = BackupArchive.restore(
                new ByteArrayInputStream(archive(entries)), temporaryFolder.getRoot());

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(2), result.value);
        assertEquals(entries.get("users"), read(new File(temporaryFolder.getRoot(), "users")));
        assertEquals(entries.get("history"), read(new File(temporaryFolder.getRoot(), "history")));
    }

    @Test
    public void rejectsPathsOutsideKnownDataFiles() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("../users", "[]");

        RepositoryResult<Integer> result = BackupArchive.restore(
                new ByteArrayInputStream(archive(entries)), temporaryFolder.getRoot());

        assertFalse(result.isSuccess());
        assertFalse(new File(temporaryFolder.getRoot().getParentFile(), "users").exists());
    }

    @Test
    public void invalidJsonDoesNotReplaceExistingData() throws Exception {
        File users = new File(temporaryFolder.getRoot(), "users");
        Files.write(users.toPath(), "[{\"existing\":true}]".getBytes(StandardCharsets.UTF_8));
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "not-json");

        RepositoryResult<Integer> result = BackupArchive.restore(
                new ByteArrayInputStream(archive(entries)), temporaryFolder.getRoot());

        assertFalse(result.isSuccess());
        assertEquals("[{\"existing\":true}]", read(users));
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

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
