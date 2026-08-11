package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupArchiveTest {
    @Test
    public void readsCompleteRecognizedDataset() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "[{\"uuid\":\"user-one\"}]");
        entries.put("history", "[]");
        entries.put("goals", "[]");

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(archive(entries)));

        assertTrue(result.isSuccess());
        assertEquals(entries, result.value);
    }

    @Test
    public void partialDatasetIsRejected() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "[]");
        entries.put("history", "[]");

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(archive(entries)));

        assertFalse(result.isSuccess());
        assertEquals("Backup does not contain the complete application dataset", result.message);
    }

    @Test
    public void createsAllSupportedEntriesAndRoundTripsThem() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("users", "[{\"uuid\":\"user-one\"}]");
        expected.put("history", "[{\"date\":123}]");
        expected.put("goals", "[]");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        RepositoryResult<Integer> created = BackupArchive.create(output, expected);
        RepositoryResult<Map<String, String>> restoredResult = BackupArchive.read(
                new ByteArrayInputStream(output.toByteArray()));

        assertTrue(created.isSuccess());
        assertEquals(Integer.valueOf(3), created.value);
        assertTrue(restoredResult.isSuccess());
        assertEquals(expected, restoredResult.value);
    }

    @Test
    public void creationFailsWhenAnyRequiredSourceIsMissing() {
        Map<String, String> incomplete = new LinkedHashMap<>();
        incomplete.put("users", "[]");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        RepositoryResult<Integer> result = BackupArchive.create(output, incomplete);

        assertFalse(result.isSuccess());
        assertEquals("Backup source does not contain the complete application dataset", result.message);
    }

    @Test
    public void creationReportsOutputFailureAndClosesTransferredStream() throws Exception {
        Map<String, String> data = emptyDataset();
        FailingOutputStream output = new FailingOutputStream();

        RepositoryResult<Integer> result = BackupArchive.create(output, data);

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

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(duplicate));

        assertFalse(result.isSuccess());
    }

    @Test
    public void rejectsPathsOutsideKnownDataFiles() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("../users", "[]");

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(archive(entries)));

        assertFalse(result.isSuccess());
    }

    @Test
    public void invalidJsonIsRejected() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("users", "not-json");
        entries.put("history", "[]");
        entries.put("goals", "[]");

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(archive(entries)));

        assertFalse(result.isSuccess());
    }

    @Test
    public void aggregateUncompressedLimitAppliesAcrossEntries() throws Exception {
        Map<String, String> entries = emptyDataset();

        RepositoryResult<Map<String, String>> result = BackupArchive.read(
                new ByteArrayInputStream(archive(entries)), 100, 5);

        assertFalse(result.isSuccess());
        assertEquals("Backup application data is too large", result.message);
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

    private static Map<String, String> emptyDataset() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("users", "[]");
        data.put("history", "[]");
        data.put("goals", "[]");
        return data;
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
