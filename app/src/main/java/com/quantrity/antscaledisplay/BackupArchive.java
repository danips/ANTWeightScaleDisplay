package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;

/** Creates and validates app data archives without trusting ZIP entry paths. */
final class BackupArchive {
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_ENTRY_BYTES = 50 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 50 * 1024 * 1024;
    static final List<String> DATA_FILES =
            Arrays.asList("users", "history", "goals");

    private BackupArchive() {}

    /** Creates an archive and closes the supplied output stream. */
    static RepositoryResult<Integer> create(OutputStream output, Map<String, String> data) {
        if (output == null) return failure("Could not open backup destination", null);
        if (data == null || !data.keySet().equals(
                new java.util.LinkedHashSet<>(DATA_FILES))) {
            closeQuietly(output);
            return failure("Backup source does not contain the complete application dataset", null);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipOutputStream archive = new ZipOutputStream(new BufferedOutputStream(output))) {
            archive.setLevel(Deflater.BEST_COMPRESSION);
            for (String name : DATA_FILES) {
                archive.putNextEntry(new ZipEntry(name));
                byte[] contents = data.get(name).getBytes(StandardCharsets.UTF_8);
                int offset = 0;
                while (offset < contents.length) {
                    int count = Math.min(buffer.length, contents.length - offset);
                    archive.write(contents, offset, count);
                    offset += count;
                }
                archive.closeEntry();
            }
            archive.finish();
            return RepositoryResult.success(DATA_FILES.size());
        } catch (Exception exception) {
            return failure("Could not create backup archive", exception);
        }
    }

    /** Reads and validates an archive and closes the supplied input stream. */
    static RepositoryResult<Map<String, String>> read(InputStream input) {
        return read(input, MAX_ENTRY_BYTES, MAX_TOTAL_BYTES);
    }

    static RepositoryResult<Map<String, String>> read(
            InputStream input, int maxEntryBytes, int maxTotalBytes) {
        if (input == null) {
            return dataFailure("Could not open backup archive", null);
        }

        Map<String, String> decoded = new LinkedHashMap<>();
        int aggregateTotal = 0;
        try (ZipInputStream archive = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((entry = archive.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !DATA_FILES.contains(name) || decoded.containsKey(name)) {
                    return dataFailure("Backup contains an unsupported entry: " + name, null);
                }

                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                int total = 0;
                int count;
                while ((count = archive.read(buffer)) != -1) {
                    total += count;
                    aggregateTotal += count;
                    if (total > maxEntryBytes) {
                        return dataFailure("Backup entry is too large: " + name, null);
                    }
                    if (aggregateTotal > maxTotalBytes) {
                        return dataFailure("Backup application data is too large", null);
                    }
                    contents.write(buffer, 0, count);
                }
                String json = new String(contents.toByteArray(), StandardCharsets.UTF_8);
                new JSONArray(json);
                decoded.put(name, json);
                archive.closeEntry();
            }
        } catch (Exception exception) {
            return dataFailure("Could not read backup archive", exception);
        }

        if (!decoded.keySet().equals(new java.util.LinkedHashSet<>(DATA_FILES))) {
            return dataFailure("Backup does not contain the complete application dataset", null);
        }
        return RepositoryResult.success(decoded);
    }

    private static RepositoryResult<Integer> failure(String message, Exception error) {
        return RepositoryResult.failure(message,
                error == null ? new IllegalArgumentException(message) : error);
    }

    private static RepositoryResult<Map<String, String>> dataFailure(
            String message, Exception error) {
        return RepositoryResult.failure(message,
                error == null ? new IllegalArgumentException(message) : error);
    }

    private static void closeQuietly(OutputStream output) {
        try {
            output.close();
        } catch (Exception ignored) { }
    }
}
