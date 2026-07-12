package com.quantrity.antscaledisplay;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;

public final class AppStateViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AntWeightController antWeightController;

    public AppStateViewModel(@NonNull Application application) {
        super(application);
        repository = AppRepository.get(application);
    }

    RepositoryResult<Void> reload() {
        return repository.reloadState();
    }

    boolean isLoaded() {
        return repository.isStateLoaded();
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

    void saveWeight(Weight weight, boolean editing, AppRepository.MutationCallback callback) {
        repository.upsertWeight(weight, editing, onMainThread(callback));
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
        super.onCleared();
    }
}
