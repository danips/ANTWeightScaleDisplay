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
import java.io.IOException;
import java.io.OutputStream;
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
    public void createsAllSupportedEntriesAndRoundTripsThem() throws Exception {
        File source = temporaryFolder.newFolder("source");
        File restored = temporaryFolder.newFolder("restored");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("users", "[{\"uuid\":\"user-one\"}]");
        expected.put("history", "[{\"date\":123}]");
        expected.put("goals", "[]");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            Files.write(new File(source, entry.getKey()).toPath(),
                    entry.getValue().getBytes(StandardCharsets.UTF_8));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        RepositoryResult<Integer> created = BackupArchive.create(output, source);
        RepositoryResult<Integer> restoredResult = BackupArchive.restore(
                new ByteArrayInputStream(output.toByteArray()), restored);

        assertTrue(created.isSuccess());
        assertEquals(Integer.valueOf(3), created.value);
        assertTrue(restoredResult.isSuccess());
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), read(new File(restored, entry.getKey())));
        }
    }

    @Test
    public void creationFailsWhenAnyRequiredSourceIsMissing() throws Exception {
        File source = temporaryFolder.newFolder("incomplete");
        Files.write(new File(source, "users").toPath(), "[]".getBytes(StandardCharsets.UTF_8));

        RepositoryResult<Integer> result = BackupArchive.create(
                new ByteArrayOutputStream(), source);

        assertFalse(result.isSuccess());
        assertEquals("Could not read backup source: history", result.message);
    }

    @Test
    public void creationReportsOutputFailureAndClosesTransferredStream() throws Exception {
        File source = temporaryFolder.newFolder("output-failure");
        for (String name : new String[]{"users", "history", "goals"}) {
            Files.write(new File(source, name).toPath(), "[]".getBytes(StandardCharsets.UTF_8));
        }
        FailingOutputStream output = new FailingOutputStream();

        RepositoryResult<Integer> result = BackupArchive.create(output, source);

        assertFalse(result.isSuccess());
        assertTrue(output.closed);
    }

    @Test
    public void duplicateEntriesAreRejected() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "[]");
        entries.put("goals", "[]");
        byte[] duplicate = archive(entries);
        replaceAscii(duplicate, "goals", "users");

        RepositoryResult<Integer> result = BackupArchive.restore(
                new ByteArrayInputStream(duplicate), temporaryFolder.getRoot());

        assertFalse(result.isSuccess());
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

    private static void replaceAscii(byte[] bytes, String from, String to) {
        byte[] needle = from.getBytes(StandardCharsets.US_ASCII);
        byte[] replacement = to.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= bytes.length - needle.length; i++) {
            boolean matches = true;
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) matches = false;
            }
            if (matches) System.arraycopy(replacement, 0, bytes, i, replacement.length);
        }
    }

    private static final class FailingOutputStream extends OutputStream {
        boolean closed;

        @Override public void write(int value) throws IOException {
            throw new IOException("expected output failure");
        }

        @Override public void close() {
            closed = true;
        }
    }
}
