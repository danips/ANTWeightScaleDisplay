package com.quantrity.antscaledisplay;

import android.view.Menu;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

/** Narrow Activity contract used by fragments for navigation and shared action-bar UI. */
interface AppHost {
    static AppHost from(Fragment fragment) {
        return (AppHost) fragment.requireActivity();
    }

    void reloadDB();
    void openEditUserFragment(User user);
    void closeEditUserFragment(User user);
    void openEditWeightFragment(Weight weight, User user, boolean edit);
    void closeEditWeightFragment(Weight weight, User user, boolean edit, boolean change);
    void openEditGoalFragment(Goal goal);
    void closeEditGoalFragment(Goal goal);
    Spinner addUsersSpinner(Menu menu, AdapterView.OnItemSelectedListener listener);
    boolean handleMutationFailure(RepositoryResult<Void> result);
    void showMessage(String message);
}
