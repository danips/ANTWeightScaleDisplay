package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Validates and restores app-created data archives without trusting ZIP entry paths. */
final class BackupArchive {
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_ENTRY_BYTES = 50 * 1024 * 1024;
    private static final Set<String> DATA_FILES = new HashSet<>();

    static {
        DATA_FILES.add("users");
        DATA_FILES.add("history");
        DATA_FILES.add("goals");
    }

    private BackupArchive() {}

    static RepositoryResult<Integer> restore(InputStream input, File directory) {
        if (input == null) {
            return failure("Could not open backup archive", null);
        }

        Map<String, String> decoded = new LinkedHashMap<>();
        try (ZipInputStream archive = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((entry = archive.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !DATA_FILES.contains(name) || decoded.containsKey(name)) {
                    return failure("Backup contains an unsupported entry: " + name, null);
                }

                ByteArrayOutputStream contents = new ByteArrayOutputStream();
                int total = 0;
                int count;
                while ((count = archive.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_ENTRY_BYTES) {
                        return failure("Backup entry is too large: " + name, null);
                    }
                    contents.write(buffer, 0, count);
                }
                String json = new String(contents.toByteArray(), StandardCharsets.UTF_8);
                new JSONArray(json);
                decoded.put(name, json);
                archive.closeEntry();
            }
        } catch (Exception exception) {
            return failure("Could not read backup archive", exception);
        }

        if (decoded.isEmpty()) return failure("Backup does not contain application data", null);
        for (Map.Entry<String, String> entry : decoded.entrySet()) {
            RepositoryResult<Void> write = new AtomicJsonFile(
                    new File(directory, entry.getKey())).write(entry.getValue());
            if (!write.isSuccess()) {
                return RepositoryResult.failure(write.message, write.error);
            }
        }
        return RepositoryResult.success(decoded.size());
    }

    private static RepositoryResult<Integer> failure(String message, Exception error) {
        return RepositoryResult.failure(message,
                error == null ? new IllegalArgumentException(message) : error);
    }
}
