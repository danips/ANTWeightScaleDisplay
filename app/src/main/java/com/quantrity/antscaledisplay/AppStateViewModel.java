package com.quantrity.antscaledisplay;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public final class AppStateViewModel extends AndroidViewModel {
    private final AppRepository repository;
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

    RepositoryResult<Void> saveUser(User user) {
        return repository.upsertUser(user);
    }

    RepositoryResult<Void> reloadGarminTokens(User user) {
        return repository.reloadGarminTokens(user);
    }

    Future<RepositoryResult<Void>> saveWeight(Weight weight, boolean editing) {
        return repository.upsertWeight(weight, editing);
    }

    Future<RepositoryResult<Void>> saveGoal(Goal goal) {
        return repository.upsertGoal(goal);
    }

    Future<RepositoryResult<Void>> replaceWeights(List<Weight> weights) {
        return repository.replaceWeights(weights);
    }

    Future<RepositoryResult<Void>> deleteWeight(Weight weight) {
        return repository.deleteWeight(weight);
    }

    Future<RepositoryResult<Void>> deleteGoal(Goal goal) {
        return repository.deleteGoal(goal);
    }

    RepositoryResult<Void> deleteUser(User user) {
        return repository.deleteUser(user);
    }

    AntWeightController antWeightController() { return antWeightController; }

    AntWeightController newAntWeightController(AntWeightListener listener) {
        if (antWeightController != null && antWeightController.isRunning()) {
            antWeightController.cancel();
        }
        antWeightController = new AntWeightController(getApplication(), listener);
        return antWeightController;
    }

    @Override
    protected void onCleared() {
        if (antWeightController != null && antWeightController.isRunning()) {
            antWeightController.cancel();
        }
        super.onCleared();
    }
}
