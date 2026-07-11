package com.quantrity.antscaledisplay;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

final class AtomicJsonFile {
    private final File target;
    private final File temporary;
    private final File backup;

    AtomicJsonFile(File target) {
        this.target = target;
        temporary = new File(target.getPath() + ".tmp");
        backup = new File(target.getPath() + ".del");
    }

    synchronized RepositoryResult<String> read() {
        RepositoryResult<Void> recovery = recover();
        if (!recovery.isSuccess()) return RepositoryResult.failure(recovery.message, recovery.error);
        if (!target.exists()) return RepositoryResult.success(null);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(target), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            return RepositoryResult.success(json.toString());
        } catch (Exception e) {
            return RepositoryResult.failure("Could not read " + target, e);
        }
    }

    synchronized RepositoryResult<Void> write(String json) {
        RepositoryResult<Void> recovery = recover();
        if (!recovery.isSuccess()) return recovery;

        try (FileOutputStream stream = new FileOutputStream(temporary);
             OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            writer.write(json);
            writer.flush();
            stream.getFD().sync();
        } catch (Exception e) {
            return RepositoryResult.failure("Could not write temporary file " + temporary, e);
        }

        if (backup.exists() && !backup.delete()) {
            return failure("Could not delete stale backup " + backup);
        }
        if (target.exists() && !target.renameTo(backup)) {
            return failure("Could not back up " + target);
        }
        if (!temporary.renameTo(target)) {
            if (backup.exists() && !backup.renameTo(target)) {
                return failure("Could not replace or restore " + target);
            }
            return failure("Could not replace " + target);
        }
        if (backup.exists() && !backup.delete()) {
            return failure("Could not delete backup " + backup);
        }
        return RepositoryResult.success(null);
    }

    synchronized RepositoryResult<Void> recover() {
        if (target.exists()) {
            if (temporary.exists() && !temporary.delete()) {
                return failure("Could not delete stale temporary file " + temporary);
            }
            if (backup.exists() && !backup.delete()) {
                return failure("Could not delete stale backup " + backup);
            }
            return RepositoryResult.success(null);
        }

        if (temporary.exists()) {
            if (!containsValidJson(temporary)) {
                if (!temporary.delete()) {
                    return failure("Could not delete incomplete temporary file " + temporary);
                }
                if (backup.exists() && !backup.renameTo(target)) {
                    return failure("Could not restore backup " + backup);
                }
                return RepositoryResult.success(null);
            }
            if (!temporary.renameTo(target)) {
                return failure("Could not recover temporary file " + temporary);
            }
            if (backup.exists() && !backup.delete()) {
                return failure("Could not delete backup after recovery " + backup);
            }
            return RepositoryResult.success(null);
        }

        if (backup.exists() && !backup.renameTo(target)) {
            return failure("Could not restore backup " + backup);
        }
        return RepositoryResult.success(null);
    }

    private static RepositoryResult<Void> failure(String message) {
        return RepositoryResult.failure(message, new IllegalStateException(message));
    }

    private static boolean containsValidJson(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            new JSONArray(json.toString());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
