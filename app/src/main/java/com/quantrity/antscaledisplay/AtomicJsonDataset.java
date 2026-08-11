package com.quantrity.antscaledisplay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Journals replacement of a fixed group of JSON files as one recoverable generation. */
final class AtomicJsonDataset {
    interface CommitObserver {
        void beforeWrite(String name, int index) throws Exception;
    }

    private static final String MARKER_NAME = ".dataset-transaction";
    private static final String BACKUP_SUFFIX = ".dataset-backup";
    private static final CommitObserver NO_OP_OBSERVER = (name, index) -> { };

    private final List<String> names;
    private final Map<String, AtomicJsonFile> targets = new LinkedHashMap<>();
    private final Map<String, AtomicJsonFile> backups = new LinkedHashMap<>();
    private final AtomicJsonFile marker;
    private final CommitObserver observer;

    AtomicJsonDataset(File directory, List<String> names) {
        this(directory, names, NO_OP_OBSERVER);
    }

    AtomicJsonDataset(File directory, List<String> names, CommitObserver observer) {
        this.names = new ArrayList<>(names);
        this.observer = observer == null ? NO_OP_OBSERVER : observer;
        for (String name : names) {
            targets.put(name, new AtomicJsonFile(new File(directory, name)));
            backups.put(name, new AtomicJsonFile(
                    new File(directory, "." + name + BACKUP_SUFFIX)));
        }
        marker = new AtomicJsonFile(new File(directory, MARKER_NAME));
    }

    synchronized RepositoryResult<Map<String, String>> snapshot() {
        RepositoryResult<Void> recovered = recover();
        if (!recovered.isSuccess()) {
            return RepositoryResult.failure(recovered.message, recovered.error);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : names) {
            RepositoryResult<String> read = targets.get(name).read();
            if (!read.isSuccess()) return RepositoryResult.failure(read.message, read.error);
            if (read.value == null) {
                String message = "Could not read backup source: " + name;
                return RepositoryResult.failure(message, new IllegalStateException(message));
            }
            values.put(name, read.value);
        }
        return RepositoryResult.success(values);
    }

    synchronized RepositoryResult<Void> recover() {
        RepositoryResult<String> markerRead = marker.read();
        if (!markerRead.isSuccess()) {
            return RepositoryResult.failure(markerRead.message, markerRead.error);
        }
        if (markerRead.value == null) {
            cleanupBackups();
            return RepositoryResult.success(null);
        }

        List<String> present;
        try {
            JSONObject manifest = new JSONObject(markerRead.value);
            JSONArray array = manifest.getJSONArray("present");
            present = new ArrayList<>(array.length());
            for (int index = 0; index < array.length(); index++) {
                String name = array.getString(index);
                if (!names.contains(name) || present.contains(name)) {
                    throw new IllegalArgumentException("Invalid dataset transaction entry " + name);
                }
                present.add(name);
            }
        } catch (Exception exception) {
            return RepositoryResult.failure("Could not read dataset transaction marker", exception);
        }

        for (String name : names) {
            RepositoryResult<Void> restored;
            if (present.contains(name)) {
                RepositoryResult<String> backup = backups.get(name).read();
                if (!backup.isSuccess() || backup.value == null) {
                    return RepositoryResult.failure("Could not read dataset backup: " + name,
                            backup.error == null
                                    ? new IllegalStateException("Missing dataset backup " + name)
                                    : backup.error);
                }
                restored = targets.get(name).write(backup.value);
            } else {
                restored = targets.get(name).delete();
            }
            if (!restored.isSuccess()) return restored;
        }

        RepositoryResult<Void> markerDeleted = marker.delete();
        if (!markerDeleted.isSuccess()) return markerDeleted;
        cleanupBackups();
        return RepositoryResult.success(null);
    }

    synchronized RepositoryResult<Void> commit(Map<String, String> replacement) {
        if (!replacement.keySet().equals(new java.util.LinkedHashSet<>(names))) {
            String message = "Dataset replacement must contain all application data files";
            return RepositoryResult.failure(message, new IllegalArgumentException(message));
        }
        RepositoryResult<Void> recovered = recover();
        if (!recovered.isSuccess()) return recovered;

        JSONArray present = new JSONArray();
        for (String name : names) {
            RepositoryResult<String> current = targets.get(name).read();
            if (!current.isSuccess()) return RepositoryResult.failure(current.message, current.error);
            RepositoryResult<Void> staged;
            if (current.value == null) {
                staged = backups.get(name).delete();
            } else {
                present.put(name);
                staged = backups.get(name).write(current.value);
            }
            if (!staged.isSuccess()) {
                cleanupBackups();
                return staged;
            }
        }

        JSONObject manifest = new JSONObject();
        try {
            manifest.put("present", present);
        } catch (Exception exception) {
            cleanupBackups();
            return RepositoryResult.failure("Could not create dataset transaction marker", exception);
        }
        RepositoryResult<Void> markerWritten = marker.write(manifest.toString());
        if (!markerWritten.isSuccess()) {
            cleanupBackups();
            return markerWritten;
        }

        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            RepositoryResult<Void> written;
            try {
                observer.beforeWrite(name, index);
                written = targets.get(name).write(replacement.get(name));
            } catch (Exception exception) {
                written = RepositoryResult.failure("Could not commit dataset file: " + name,
                        exception);
            }
            if (!written.isSuccess()) {
                RepositoryResult<Void> rollback = recover();
                if (!rollback.isSuccess()) {
                    return RepositoryResult.failure(
                            written.message + "; rollback failed: " + rollback.message,
                            rollback.error);
                }
                return written;
            }
        }

        RepositoryResult<Void> markerDeleted = marker.delete();
        if (!markerDeleted.isSuccess()) {
            RepositoryResult<Void> rollback = recover();
            return rollback.isSuccess() ? markerDeleted : rollback;
        }
        cleanupBackups();
        return RepositoryResult.success(null);
    }

    private void cleanupBackups() {
        for (AtomicJsonFile backup : backups.values()) backup.delete();
    }
}
