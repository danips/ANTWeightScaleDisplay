package com.quantrity.antscaledisplay;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

/** The single setup and searchable-selection implementation for action-bar user spinners. */
final class UserSpinnerController {
    private static final int SEARCH_THRESHOLD = 10;
    private final MainActivity activity;
    private final AppStateViewModel state;

    UserSpinnerController(MainActivity activity, AppStateViewModel state) {
        this.activity = activity;
        this.state = state;
    }

    Spinner attach(Menu menu, AdapterView.OnItemSelectedListener listener) {
        MenuItem item = menu.findItem(R.id.action_select_user);
        ArrayList<User> users = state.users();
        if (users.isEmpty()) {
            item.setVisible(false);
            return null;
        }
        Spinner spinner = (Spinner) item.getActionView();
        if (spinner == null) return null;
        ArrayAdapter<User> adapter = new ArrayAdapter<>(activity,
                R.layout.fragment_weight_user_spinner_item, users);
        adapter.setDropDownViewResource(R.layout.fragment_weight_user_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (users.size() == 1) item.setVisible(false);
        else {
            spinner.setSelection(users.indexOf(state.selectedUser()), false);
            if (users.size() > SEARCH_THRESHOLD) enableSearch(spinner);
        }
        spinner.setOnItemSelectedListener(listener);
        return spinner;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enableSearch(Spinner spinner) {
        spinner.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            showSearch(spinner);
            return true;
        });
        spinner.setOnKeyListener((view, keyCode, event) -> {
            if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) return false;
            showSearch(spinner);
            return true;
        });
    }

    private void showSearch(Spinner spinner) {
        ArrayList<User> users = state.users();
        ArrayList<String> names = new ArrayList<>();
        for (User user : users) names.add(user.toString());
        SpinnerDialog dialog = new SpinnerDialog(activity, names,
                activity.getString(R.string.edit_user_fragment_user));
        dialog.setCancellable(true);
        dialog.setShowKeyboard(true);
        dialog.bindOnSpinnerListener((item, position) -> {
            User selected = users.get(position);
            state.selectUser(selected);
            spinner.setSelection(position);
            dialog.closeSpinnerDialog();
        });
        dialog.showSpinnerDialog();
    }
}
