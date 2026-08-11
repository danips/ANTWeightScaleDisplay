package com.quantrity.antscaledisplay;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppStateViewModel extends AndroidViewModel {
    enum OperationKind { BACKUP, RESTORE, CSV_EXPORT }

    static final class OperationResult {
        final OperationKind kind;
        final String displayName;
        final RepositoryResult<Integer> result;

        OperationResult(OperationKind kind, String displayName,
                        RepositoryResult<Integer> result) {
            this.kind = kind;
            this.displayName = displayName;
            this.result = result;
        }
    }

    private final AppRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "app-view-model-io");
        thread.setDaemon(true);
        return thread;
    });
    private final MutableLiveData<RepositoryResult<Void>> loadResult = new MutableLiveData<>();
    private final MutableLiveData<OperationEvent<OperationResult>> backupResult =
            new MutableLiveData<>();
    private final MutableLiveData<OperationEvent<OperationResult>> restoreResult =
            new MutableLiveData<>();
    private final MutableLiveData<OperationEvent<OperationResult>> csvResult =
            new MutableLiveData<>();
    private final Object loadLock = new Object();
    private boolean loading;
    private AntWeightController antWeightController;

    public AppStateViewModel(@NonNull Application application) {
        super(application);
        repository = AppRepository.get(application);
    }

    LiveData<RepositoryResult<Void>> loadResult() {
        return loadResult;
    }

    LiveData<OperationEvent<OperationResult>> operationResult(OperationKind kind) {
        switch (kind) {
            case BACKUP: return backupResult;
            case RESTORE: return restoreResult;
            default: return csvResult;
        }
    }

    void ensureLoaded() {
        synchronized (loadLock) {
            if (repository.isStateLoaded()) {
                loadResult.setValue(RepositoryResult.success(null));
                return;
            }
            if (loading) return;
            loading = true;
        }
        ioExecutor.execute(() -> {
            RepositoryResult<Void> result = repository.reloadState();
            synchronized (loadLock) {
                loading = false;
            }
            loadResult.postValue(result);
        });
    }

    ArrayList<User> users() {
        return repository.usersSnapshot();
    }

    ArrayList<Weight> weights() {
        return repository.weightsSnapshot();
    }

    ArrayList<Goal> goals() {
        return repository.goalsSnapshot();
    }

    ArrayList<Weight> selectedWeights() {
        return repository.selectedUserWeights();
    }

    ArrayList<Goal> selectedGoals() {
        return repository.selectedUserGoals();
    }

    User selectedUser() {
        return repository.selectedUser();
    }

    Weight lastSelectedWeight() {
        return repository.lastSelectedUserWeight();
    }

    User findUser(String uuid) {
        return repository.findUser(uuid);
    }

    Weight findWeight(String uuid, long date) {
        return repository.findWeight(uuid, date);
    }

    Goal findGoal(String uuid, long startDate, String type) {
        return repository.findGoal(uuid, startDate, type);
    }

    void selectUser(User user) {
        repository.selectUser(user == null ? null : user.uuid);
    }

    void saveUser(User user, AppRepository.MutationCallback callback) {
        repository.upsertUser(user, onMainThread(callback));
    }

    void reloadGarminTokens(User user, AppRepository.MutationCallback callback) {
        repository.reloadGarminTokens(user, onMainThread(callback));
    }

    void saveWeight(Weight weight, Weight original, AppRepository.MutationCallback callback) {
        repository.upsertWeight(weight, original, onMainThread(callback));
    }

    void saveGoal(Goal goal, AppRepository.MutationCallback callback) {
        repository.upsertGoal(goal, onMainThread(callback));
    }

    void replaceWeights(List<Weight> weights, AppRepository.MutationCallback callback) {
        repository.replaceWeights(weights, onMainThread(callback));
    }

    void deleteWeight(Weight weight, AppRepository.MutationCallback callback) {
        repository.deleteWeight(weight, onMainThread(callback));
    }

    void deleteGoal(Goal goal, AppRepository.MutationCallback callback) {
        repository.deleteGoal(goal, onMainThread(callback));
    }

    void deleteUser(User user, AppRepository.MutationCallback callback) {
        repository.deleteUser(user, onMainThread(callback));
    }

    void createBackup(ContentResolver resolver, Uri treeUri, String displayName) {
        ioExecutor.execute(() -> {
            RepositoryResult<Integer> result;
            try {
                Uri outputUri = createDocument(resolver, treeUri,
                        "application/octet-stream", displayName);
                if (outputUri == null) throw new IllegalStateException("Provider did not create a file");
                ParcelFileDescriptor descriptor = resolver.openFileDescriptor(outputUri, "w", null);
                if (descriptor == null) throw new IllegalStateException("Provider did not open the file");
                result = repository.createBackupSynchronously(
                        new ParcelFileDescriptor.AutoCloseOutputStream(descriptor));
            } catch (Exception exception) {
                result = RepositoryResult.failure("Unable to create the backup file", exception);
            }
            postOperation(OperationKind.BACKUP, displayName, result);
        });
    }

    void restoreBackup(ContentResolver resolver, Uri uri) {
        ioExecutor.execute(() -> {
            RepositoryResult<Integer> result;
            try {
                InputStream input = resolver.openInputStream(uri);
                result = repository.restoreBackupSynchronously(input);
                if (result.isSuccess()) {
                    GarminTokenRefreshScheduler.scheduleAll(
                            getApplication(), repository.usersSnapshot());
                }
            } catch (Exception exception) {
                result = RepositoryResult.failure("Unable to open the backup archive", exception);
            }
            postOperation(OperationKind.RESTORE, null, result);
        });
    }

    void exportCsv(ContentResolver resolver, Uri treeUri, String displayName,
                   User user, List<Weight> weights) {
        ArrayList<Weight> snapshot = new ArrayList<>(weights);
        ioExecutor.execute(() -> {
            RepositoryResult<Integer> result;
            try {
                Uri outputUri = createDocument(resolver, treeUri, "text/csv", displayName);
                if (outputUri == null) throw new IllegalStateException("Provider did not create a file");
                ParcelFileDescriptor descriptor = resolver.openFileDescriptor(outputUri, "w", null);
                if (descriptor == null) {
                    throw new IllegalStateException("Provider did not open the file");
                }
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new ParcelFileDescriptor.AutoCloseOutputStream(descriptor),
                        StandardCharsets.UTF_8)) {
                    result = CsvExporter.write(writer, getApplication(), user, snapshot);
                }
            } catch (Exception exception) {
                result = RepositoryResult.failure("Unable to create the CSV export", exception);
            }
            postOperation(OperationKind.CSV_EXPORT, displayName, result);
        });
    }

    private static Uri createDocument(ContentResolver resolver, Uri treeUri,
                                      String mimeType, String displayName) throws Exception {
        String treeId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri directory = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
        return DocumentsContract.createDocument(resolver, directory, mimeType, displayName);
    }

    private void postOperation(OperationKind kind, String displayName,
                               RepositoryResult<Integer> result) {
        OperationEvent<OperationResult> event = new OperationEvent<>(
                new OperationResult(kind, displayName, result));
        switch (kind) {
            case BACKUP:
                backupResult.postValue(event);
                break;
            case RESTORE:
                restoreResult.postValue(event);
                break;
            default:
                csvResult.postValue(event);
                break;
        }
    }

    AntWeightController antWeightController() { return antWeightController; }

    AntWeightController newAntWeightController(AntWeightListener listener) {
        if (antWeightController != null && antWeightController.isRunning()) {
            antWeightController.cancel();
        }
        antWeightController = new AntWeightController(getApplication(), listener);
        return antWeightController;
    }

    private AppRepository.MutationCallback onMainThread(AppRepository.MutationCallback callback) {
        return result -> mainHandler.post(() -> callback.onComplete(result));
    }

    @Override
    protected void onCleared() {
        if (antWeightController != null && antWeightController.isRunning()) {
            antWeightController.cancel();
        }
        ioExecutor.shutdownNow();
        super.onCleared();
    }
}
