package com.quantrity.antscaledisplay;

import android.view.Menu;
import android.widget.AdapterView;
import android.widget.Spinner;

import java.util.ArrayList;

interface CoreInterface {
    ArrayList<User> getUsersArray();
    ArrayList<Weight> getHistoryArraySelectedUser();
    Weight getLastHistorySelectedUser();
    ArrayList<Weight> getHistoryArray();
    ArrayList<Goal> getGoalsArraySelectedUser();
    ArrayList<Goal> getGoalsArray();
    void deleteHistoryAndUser(User user);
    void reloadDB();

    void openEditUserFragment(User user);
    void closeEditUserFragment(User user);

    void openEditWeightFragment(Weight weight, User user, boolean edit);
    void closeEditWeightFragment(MainActivity activity, Weight weight, User user, boolean edit, boolean change);

    void openEditGoalFragment(Goal goal);
    void closeEditGoalFragment(Goal goal);

    RequestWeight getRequestWeight();
    RequestWeight newRequestWeight(WeightFragment fragment);

    void saveWeight(Weight weight);
    void deleteWeight(Weight weight);

    void saveGoal(/*User user,*/ Goal goal);
    void deleteGoal(Goal goal);

    User getSelectedUser();
    void setSelectedUser(User user);
    Spinner addUsersSpinner(Menu menu, AdapterView.OnItemSelectedListener oisListener);
}
