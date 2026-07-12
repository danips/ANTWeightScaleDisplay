package com.quantrity.antscaledisplay;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.quantrity.antscaledisplay.databinding.FragmentEditGoalBinding;

import java.util.Calendar;

public class EditGoalFragment extends Fragment implements MenuProvider {
    private static final String ARG_GOAL_USER_UUID = "goal_user_uuid";
    private static final String ARG_GOAL_START_DATE = "goal_start_date";
    private static final String ARG_GOAL_TYPE = "goal_type";
    private static final String ARG_USER_UUID = "user_uuid";
    private static final String STATE_START_DATE = "start_date";
    private static final String STATE_END_DATE = "end_date";
    private static final int DATE_FORMAT = DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR;

    private Goal goal;
    private User user;
    private Weight last;
    private AppStateViewModel state;
    private boolean needsSync;

    private Spinner typeSpinner;
    private EditText startDate;
    private long startDateMillis = -1;
    private EditText endDate;
    private long endDateMillis = -1;
    private GoalValueInput startValue;
    private GoalValueInput endValue;
    private ColorPicker colorPicker;
    private FragmentEditGoalBinding binding;

    static EditGoalFragment newInstance(String goalUserUuid, long startDate, String goalType,
                                        String userUuid) {
        EditGoalFragment fragment = new EditGoalFragment();
        Bundle arguments = new Bundle();
        if (goalUserUuid != null) arguments.putString(ARG_GOAL_USER_UUID, goalUserUuid);
        arguments.putLong(ARG_GOAL_START_DATE, startDate);
        if (goalType != null) arguments.putString(ARG_GOAL_TYPE, goalType);
        arguments.putString(ARG_USER_UUID, userUuid);
        fragment.setArguments(arguments);
        return fragment;
    }

    private Goal validatedGoal() {
        if (getActivity() == null || !datesAreValid()) return null;
        LocalizedNumberParser.Result parsedStart = startValue.readCanonicalValue();
        LocalizedNumberParser.Result parsedEnd = endValue.readCanonicalValue();
        if (!parsedStart.isValid() || !parsedEnd.isValid()) return null;

        Goal result = goal == null ? new Goal() : goal;
        result.uuid = user.uuid;
        result.type = selectedMetric();
        result.start_date = startDateMillis;
        result.end_date = endDateMillis;
        result.start_value = parsedStart.value();
        result.end_value = parsedEnd.value();
        result.color = colorPicker.getColor();
        return result;
    }

    private boolean datesAreValid() {
        startDate.setError(null);
        endDate.setError(null);
        if (startDateMillis < 0) {
            startDate.setError(getString(R.string.weight_edit_empty_value_error));
            return false;
        }
        if (endDateMillis < 0 || endDateMillis <= startDateMillis) {
            endDate.setError(getString(R.string.edit_goal_date_range_error));
            return false;
        }
        return true;
    }

    private Metric selectedMetric() {
        return Metric.fromGoalPosition(typeSpinner.getSelectedItemPosition());
    }

    private void showGoal() {
        if (goal == null) {
            goal = new Goal();
            resetGoal();
            return;
        }
        startDateMillis = goal.start_date;
        endDateMillis = goal.end_date;
        showDate(startDate, startDateMillis);
        showDate(endDate, endDateMillis);
        colorPicker.setColor(goal.color);
        configureValues(goal.type);
        startValue.setCanonicalValue(goal.start_value);
        endValue.setCanonicalValue(goal.end_value);
    }

    private void configureValues(Metric metric) {
        startValue.configure(metric, user, goal.show_fat_mass);
        endValue.configure(metric, user, goal.show_fat_mass);
    }

    private void resetGoal() {
        goal.color = Color.RED;
        goal.type = Metric.WEIGHT;
        goal.show_fat_mass = user.show_fat_mass;
        colorPicker.setColor(goal.color);
        typeSpinner.setSelection(0);

        Calendar calendar = Calendar.getInstance();
        goal.start_date = startDateMillis = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 3);
        goal.end_date = endDateMillis = calendar.getTimeInMillis();
        showDate(startDate, startDateMillis);
        showDate(endDate, endDateMillis);

        double initial = last == null ? -1 : last.weight;
        goal.start_value = goal.end_value = initial;
        configureValues(goal.type);
        startValue.setCanonicalValue(initial);
        endValue.setCanonicalValue(initial);
    }

    private void changeMetric(int position) {
        Metric metric = Metric.fromGoalPosition(position);
        goal.type = metric;
        double initial = last == null ? 0 : metric.goalValue(last, goal.show_fat_mass);
        if (initial == -1) initial = 0;
        goal.start_value = goal.end_value = initial;
        configureValues(metric);
        startValue.setCanonicalValue(initial);
        endValue.setCanonicalValue(initial);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        if (!state.isLoaded()) state.reload();
        Bundle arguments = getArguments();
        String goalUuid = arguments == null ? null : arguments.getString(ARG_GOAL_USER_UUID);
        long goalStart = arguments == null ? -1 : arguments.getLong(ARG_GOAL_START_DATE, -1);
        String goalType = arguments == null ? null : arguments.getString(ARG_GOAL_TYPE);
        String userUuid = arguments == null ? null : arguments.getString(ARG_USER_UUID);
        goal = goalUuid == null ? null : state.findGoal(goalUuid, goalStart, goalType);
        user = state.findUser(userUuid);
        last = state.lastSelectedWeight();
        if (savedInstanceState != null) {
            startDateMillis = savedInstanceState.getLong(STATE_START_DATE, -1);
            endDateMillis = savedInstanceState.getLong(STATE_END_DATE, -1);
        }

        binding = FragmentEditGoalBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        KeyboardUtils.dismissOnTouchOutsideInputs(root, requireActivity());
        bindViews();
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        needsSync = savedInstanceState == null;

        Metric initialMetric = goal == null ? Metric.WEIGHT : goal.type;
        boolean showFatMass = goal == null ? user.show_fat_mass : goal.show_fat_mass;
        startValue.configure(initialMetric, user, showFatMass);
        endValue.configure(initialMetric, user, showFatMass);
        bindDatePicker(startDate, true);
        bindDatePicker(endDate, false);
        bindTypeSelection();
        return root;
    }

    private void bindViews() {
        typeSpinner = binding.spType;
        typeSpinner.setEnabled(goal == null);
        startDate = binding.etStartDate;
        endDate = binding.etEndDate;
        colorPicker = binding.picker;
        startValue = new GoalValueInput(binding.ll0unitStart, binding.ll1unitStart,
                binding.ll2unitStart, binding.etStartValue00, binding.etStartValue10,
                binding.etStartValue20, binding.etStartValue21, binding.tvStartValue10,
                binding.tvStartValue20, binding.tvStartValue21);
        endValue = new GoalValueInput(binding.ll0unitEnd, binding.ll1unitEnd,
                binding.ll2unitEnd, binding.etEndValue00, binding.etEndValue10,
                binding.etEndValue20, binding.etEndValue21, binding.tvEndValue10,
                binding.tvEndValue20, binding.tvEndValue21);
    }

    private void bindDatePicker(EditText input, boolean isStart) {
        input.setOnFocusChangeListener((view, focused) -> {
            if (!focused || getActivity() == null) return;
            long value = isStart ? startDateMillis : endDateMillis;
            Calendar calendar = Calendar.getInstance();
            if (value >= 0) calendar.setTimeInMillis(value);
            DatePickerDialog dialog = new DatePickerDialog(getActivity(),
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, day, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        long millis = selected.getTimeInMillis();
                        if (isStart) startDateMillis = millis;
                        else endDateMillis = millis;
                        input.setError(null);
                        showDate(input, millis);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.setTitle(isStart ? R.string.edit_goal_fragment_start
                    : R.string.edit_goal_fragment_end);
            dialog.show();
        });
    }

    private void bindTypeSelection() {
        if (!typeSpinner.isEnabled()) return;
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view,
                                                  int position, long id) {
                if (goal != null && !needsSync) changeMetric(position);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showDate(EditText input, long millis) {
        input.setText(DateUtils.formatDateTime(requireContext(), millis, DATE_FORMAT));
    }

    @Override public void onResume() {
        super.onResume();
        if (!needsSync) {
            if (goal == null) {
                goal = new Goal();
                goal.type = selectedMetric();
                goal.show_fat_mass = user.show_fat_mass;
                configureValues(goal.type);
            }
            return;
        }
        if (goal != null) typeSpinner.setSelection(goal.type.getMetricCode() - 1);
        showGoal();
        needsSync = false;
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_START_DATE, startDateMillis);
        outState.putLong(STATE_END_DATE, endDateMillis);
    }

    @Override public void onDestroyView() {
        typeSpinner = null;
        startDate = endDate = null;
        startValue = endValue = null;
        colorPicker = null;
        binding = null;
        super.onDestroyView();
    }

    @Override public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_edit_goal_menu, menu);
    }

    @Override public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_editgoal_cancel) {
            if (getActivity() != null) AppHost.from(this).closeEditGoalFragment(null);
            return true;
        }
        if (item.getItemId() == R.id.action_editgoal_done) {
            Goal result = validatedGoal();
            if (result != null && getActivity() != null) {
                AppHost.from(this).closeEditGoalFragment(result);
            }
            return true;
        }
        return false;
    }
}
