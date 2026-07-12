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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.quantrity.antscaledisplay.databinding.FragmentEditGoalBinding;

import java.util.Calendar;
import java.util.Locale;

public class EditGoalFragment extends Fragment implements MenuProvider {
    private static final String ARG_GOAL_USER_UUID = "goal_user_uuid";
    private static final String ARG_GOAL_START_DATE = "goal_start_date";
    private static final String ARG_GOAL_TYPE = "goal_type";
    private static final String ARG_USER_UUID = "user_uuid";
    private static final String STATE_START_DATE = "start_date";
    private static final String STATE_END_DATE = "end_date";
    //private final static String TAG = "EditGoalFragment";

    enum Units {
        NO_UNITS,
        ONE_UNIT,
        ONE_UNIT_WEIGHT,
        TWO_UNITS_WEIGHT
    }

    //Data fields
    private Goal the_goal;
    private User the_user;
    private Weight last;
    private AppStateViewModel state;

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

    private boolean needs_to_sync;

    private Spinner sp_type;
    private EditText et_start_date;
    private long start_date_millis = -1;
    private EditText et_end_date;
    private long end_date_millis = -1;
    private LinearLayout ll_0unitStart;
    private LinearLayout ll_1unitStart;
    private LinearLayout ll_2unitStart;
    private LinearLayout ll_0unitEnd;
    private LinearLayout ll_1unitEnd;
    private LinearLayout ll_2unitEnd;
    private EditText et_startValue00;
    private EditText et_startValue10;
    private EditText et_startValue20;
    private EditText et_startValue21;
    private TextView tv_startValue10;
    private TextView tv_startValue20;
    private TextView tv_startValue21;
    private EditText et_endValue00;
    private EditText et_endValue10;
    private EditText et_endValue20;
    private EditText et_endValue21;
    private TextView tv_endValue10;
    private TextView tv_endValue20;
    private TextView tv_endValue21;
    private ColorPicker cp;
    private FragmentEditGoalBinding binding;

    private Goal checkValues() {
        Goal tmp = new Goal();

        if (getActivity() == null) return null;
        tmp.uuid = the_user.uuid;
        tmp.type = Metric.fromGoalPosition(sp_type.getSelectedItemPosition());
        Metric.Unit displayedUnit = tmp.type.displayedUnit(the_goal.show_fat_mass);
        Units units = displayedUnit == Metric.Unit.MASS
                ? (the_user.mass_unit == User.MassUnit.ST
                ? Units.TWO_UNITS_WEIGHT : Units.ONE_UNIT_WEIGHT)
                : displayedUnit == Metric.Unit.NONE ? Units.NO_UNITS : Units.ONE_UNIT;

        switch (units)
        {
            case NO_UNITS:
                tmp.start_value = readNumber(et_startValue00);
                tmp.end_value = readNumber(et_endValue00);
                break;
            case ONE_UNIT:
                tmp.start_value = readNumber(et_startValue10);
                tmp.end_value = readNumber(et_endValue10);
                break;
            case ONE_UNIT_WEIGHT:
                tmp.start_value = MassConverter.toKilograms(
                        readNumber(et_startValue10), the_user.mass_unit);
                tmp.end_value = MassConverter.toKilograms(
                        readNumber(et_endValue10), the_user.mass_unit);
                break;
            case TWO_UNITS_WEIGHT:
                tmp.start_value = MassConverter.stonePoundsToKilograms(
                        readNumber(et_startValue20), readNumber(et_startValue21));
                tmp.end_value = MassConverter.stonePoundsToKilograms(
                        readNumber(et_endValue20), readNumber(et_endValue21));
                break;

            default:
        }

        tmp.start_date = start_date_millis;
        tmp.end_date = end_date_millis;
        tmp.color = cp.getColor();

        if (the_goal == null)
        {
            return tmp;
        }

        the_goal.uuid = tmp.uuid;
        the_goal.type = tmp.type;
        the_goal.start_date = tmp.start_date;
        the_goal.end_date = tmp.end_date;
        the_goal.start_value = tmp.start_value;
        the_goal.end_value = tmp.end_value;
        the_goal.color = tmp.color;
        return the_goal;
    }

    private void setValues() {
        if (the_goal == null) {
            the_goal = new Goal();
            resetValues();
            return;
        }

        //sp_type.setSelection(the_goal.type.getMetricCode() - 1);

        et_start_date.setText(DateUtils.formatDateTime(getActivity(), the_goal.start_date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        start_date_millis = the_goal.start_date;
        et_end_date.setText(DateUtils.formatDateTime(getActivity(), the_goal.end_date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        end_date_millis = the_goal.end_date;
        cp.setColor(the_goal.color);

        Units units;
        switch (the_goal.type)
        {
            case BMI:
            case VISCERALFATRATING:
                et_startValue00.setText(String.format(Locale.getDefault(),"%.01f", the_goal.start_value));
                et_endValue00.setText(String.format(Locale.getDefault(),"%.01f", the_goal.end_value));
                units = Units.NO_UNITS;
                break;

            case PHYSIQUERATING:
                et_startValue00.setText(String.format(Locale.getDefault(),"%.00f", the_goal.start_value));
                et_endValue00.setText(String.format(Locale.getDefault(),"%.00f", the_goal.end_value));
                units = Units.NO_UNITS;
                break;

            case METABOLICAGE:
                et_startValue10.setText(String.format(Locale.getDefault(),"%.0f", the_goal.start_value));
                et_endValue10.setText(String.format(Locale.getDefault(),"%.0f", the_goal.end_value));
                tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_years_tag));
                tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_years_tag));
                units = Units.ONE_UNIT;
                break;

            case ACTIVEMET:
            case BASALMET:
                et_startValue10.setText(String.format(Locale.getDefault(),"%.0f", the_goal.start_value));
                et_endValue10.setText(String.format(Locale.getDefault(),"%.0f", the_goal.end_value));
                tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_kcal_tag));
                tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_kcal_tag));
                units = Units.ONE_UNIT;
                break;

            case PERCENTHYDRATION:
                et_startValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.start_value));
                et_endValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.end_value));
                tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_percent_tag));
                tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_percent_tag));
                units = Units.ONE_UNIT;
                break;

            case PERCENTFAT:
            case TRUNKPERCENTFAT:
            case LEFTARMPERCENTFAT:
            case RIGHTARMPERCENTFAT:
            case LEFTLEGPERCENTFAT:
            case RIGHTLEGPERCENTFAT:
                if (the_goal.show_fat_mass) {
                    if (the_user.mass_unit == User.MassUnit.LB) {
                        et_startValue10.setText(String.format(Locale.getDefault(),"%.1f",
                                MassConverter.kilogramsToPounds(the_goal.start_value)));
                        et_endValue10.setText(String.format(Locale.getDefault(),"%.1f",
                                MassConverter.kilogramsToPounds(the_goal.end_value)));
                        tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        units = Units.ONE_UNIT_WEIGHT;
                    } else if (the_user.mass_unit == User.MassUnit.ST) {
                        MassConverter.StonePounds start =
                                MassConverter.toStonePounds(the_goal.start_value);
                        et_startValue20.setText(String.format(Locale.getDefault(),"%.0f", start.stones));
                        et_startValue21.setText(String.format(Locale.getDefault(),"%.1f", start.pounds));
                        tv_startValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                        tv_startValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));

                        MassConverter.StonePounds end =
                                MassConverter.toStonePounds(the_goal.end_value);
                        et_endValue20.setText(String.format(Locale.getDefault(),"%.0f", end.stones));
                        et_endValue21.setText(String.format(Locale.getDefault(),"%.1f", end.pounds));
                        tv_endValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                        tv_endValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        units = Units.TWO_UNITS_WEIGHT;
                    } else {
                        et_startValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.start_value));
                        et_endValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.end_value));
                        tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                        tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                        units = Units.ONE_UNIT_WEIGHT;
                    }
                }
                else
                {
                    et_startValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.start_value));
                    et_endValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.end_value));
                    tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_percent_tag));
                    tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_percent_tag));
                    units = Units.ONE_UNIT;
                }
                break;

            case BONEMASS:
            case MUSCLEMASS:
            case TRUNKMUSCLEMASS:
            case LEFTARMMUSCLEMASS:
            case RIGHTARMMUSCLEMASS:
            case LEFTLEGMUSCLEMASS:
            case RIGHTLEGMUSCLEMASS:
            case WEIGHT:
            default:
                if (the_user.mass_unit == User.MassUnit.LB) {
                    et_startValue10.setText(String.format(Locale.getDefault(),"%.1f",
                            MassConverter.kilogramsToPounds(the_goal.start_value)));
                    et_endValue10.setText(String.format(Locale.getDefault(),"%.1f",
                            MassConverter.kilogramsToPounds(the_goal.end_value)));
                    tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    units = Units.ONE_UNIT_WEIGHT;
                } else if (the_user.mass_unit == User.MassUnit.ST) {
                    MassConverter.StonePounds start =
                            MassConverter.toStonePounds(the_goal.start_value);
                    et_startValue20.setText(String.format(Locale.getDefault(),"%.0f", start.stones));
                    et_startValue21.setText(String.format(Locale.getDefault(),"%.1f", start.pounds));
                    tv_startValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                    tv_startValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));

                    MassConverter.StonePounds end =
                            MassConverter.toStonePounds(the_goal.end_value);
                    et_endValue20.setText(String.format(Locale.getDefault(),"%.0f", end.stones));
                    et_endValue21.setText(String.format(Locale.getDefault(),"%.1f", end.pounds));
                    tv_endValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                    tv_endValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    units = Units.TWO_UNITS_WEIGHT;
                } else {
                    et_startValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.start_value));
                    et_endValue10.setText(String.format(Locale.getDefault(),"%.1f", the_goal.end_value));
                    tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                    tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                    units = Units.ONE_UNIT_WEIGHT;
                }
                break;
        }


        ll_0unitStart.setVisibility(View.GONE);
        ll_0unitEnd.setVisibility(View.GONE);
        ll_1unitStart.setVisibility(View.GONE);
        ll_1unitEnd.setVisibility(View.GONE);
        ll_2unitStart.setVisibility(View.GONE);
        ll_2unitEnd.setVisibility(View.GONE);
        switch (units)
        {
            case NO_UNITS:
                ll_0unitStart.setVisibility(View.VISIBLE);
                ll_0unitEnd.setVisibility(View.VISIBLE);
                break;
            case TWO_UNITS_WEIGHT:
                ll_2unitStart.setVisibility(View.VISIBLE);
                ll_2unitEnd.setVisibility(View.VISIBLE);
                break;
            case ONE_UNIT:
            case ONE_UNIT_WEIGHT:
            default:
                ll_1unitStart.setVisibility(View.VISIBLE);
                ll_1unitEnd.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateValue(LinearLayout ll_0unit, LinearLayout ll_1unit, LinearLayout ll_2unit,
                             EditText et_Value00, EditText et_Value10, EditText et_Value20, EditText et_Value21,
                             TextView tv_Value10, TextView tv_Value20, TextView tv_Value21, double value)
    {
        int units;
        switch (the_goal.type)
        {
            case BMI:
            case PHYSIQUERATING:
            case VISCERALFATRATING:
                units = 0;
                break;

            case METABOLICAGE:
                units = 1;
                tv_Value10.setText(R.string.weight_edit_fragment_years_tag);
                break;

            case ACTIVEMET:
            case BASALMET:
                units = 1;
                tv_Value10.setText(R.string.weight_edit_fragment_kcal_tag);
                break;

            case PERCENTHYDRATION:
                units = 1;
                tv_Value10.setText(R.string.weight_edit_fragment_percent_tag);
                break;

            case PERCENTFAT:
            case TRUNKPERCENTFAT:
            case LEFTARMPERCENTFAT:
            case RIGHTARMPERCENTFAT:
            case LEFTLEGPERCENTFAT:
            case RIGHTLEGPERCENTFAT:
                if (the_goal.show_fat_mass) {
                    if (the_user.mass_unit == User.MassUnit.ST) {
                        units = 2;
                        tv_Value20.setText(R.string.weight_edit_fragment_st_tag);
                        tv_Value21.setText(R.string.weight_edit_fragment_lb_tag);
                    } else if (the_user.mass_unit == User.MassUnit.LB) {
                        units = 1;
                        tv_Value10.setText(R.string.weight_edit_fragment_lb_tag);
                        value = MassConverter.kilogramsToPounds(value);
                    } else {
                        units = 1;
                        tv_Value10.setText(R.string.weight_edit_fragment_kg_tag);
                    }
                }
                else
                {
                    units = 1;
                    tv_Value10.setText(R.string.weight_edit_fragment_percent_tag);
                }
                break;

            case BONEMASS:
            case MUSCLEMASS:
            case TRUNKMUSCLEMASS:
            case LEFTARMMUSCLEMASS:
            case RIGHTARMMUSCLEMASS:
            case LEFTLEGMUSCLEMASS:
            case RIGHTLEGMUSCLEMASS:
            case WEIGHT:
            default:
                if (the_user.mass_unit == User.MassUnit.ST) {
                    units = 2;
                    tv_Value20.setText(R.string.weight_edit_fragment_st_tag);
                    tv_Value21.setText(R.string.weight_edit_fragment_lb_tag);
                } else if (the_user.mass_unit == User.MassUnit.LB) {
                    units = 1;
                    tv_Value10.setText(R.string.weight_edit_fragment_lb_tag);
                    value = MassConverter.kilogramsToPounds(value);
                } else {
                    units = 1;
                    tv_Value10.setText(R.string.weight_edit_fragment_kg_tag);
                }
                break;
        }
        switch (units)
        {
            case 0:
                ll_0unit.setVisibility(View.VISIBLE);
                ll_1unit.setVisibility(View.GONE);
                ll_2unit.setVisibility(View.GONE);
                et_Value00.setText(String.format(Locale.getDefault(),"%.1f", value));
                break;
            case 2:
                ll_0unit.setVisibility(View.GONE);
                ll_1unit.setVisibility(View.GONE);
                ll_2unit.setVisibility(View.VISIBLE);
                MassConverter.StonePounds stonePounds = MassConverter.toStonePounds(value);
                et_Value20.setText(String.format(Locale.getDefault(),"%.0f", stonePounds.stones));
                et_Value21.setText(String.format(Locale.getDefault(),"%.1f", stonePounds.pounds));
                break;
            case 1:
            default:
                ll_0unit.setVisibility(View.GONE);
                ll_1unit.setVisibility(View.VISIBLE);
                ll_2unit.setVisibility(View.GONE);
                et_Value10.setText(String.format(Locale.getDefault(),"%.1f", value));
                break;
        }


    }

    private static double readNumber(EditText input) {
        return LocalizedNumberParser.parseOrDefault(input.getText(), 0);
    }

    private void resetValues() {
        cp.setColor(Color.RED);
        the_goal.color = Color.RED;
        the_goal.type = Metric.WEIGHT;
        the_goal.show_fat_mass = the_user.show_fat_mass;
        sp_type.setSelection(0);
        Calendar c = Calendar.getInstance();
        start_date_millis = c.getTimeInMillis();
        the_goal.start_date = start_date_millis;
        c.add(Calendar.MONTH, 3);
        end_date_millis = c.getTimeInMillis();
        the_goal.end_date = end_date_millis;
        et_start_date.setText(DateUtils.formatDateTime(getActivity(), start_date_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        et_end_date.setText(DateUtils.formatDateTime(getActivity(), end_date_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));

        last = state.lastSelectedWeight();
        if (last != null) {
            the_goal.start_value = last.weight;
            the_goal.end_value = last.weight;
        }
        else
        {
            the_goal.start_value = -1;
            the_goal.end_value = -1;
        }
        updateValue(ll_0unitStart, ll_1unitStart, ll_2unitStart, et_startValue00, et_startValue10, et_startValue20, et_startValue21, tv_startValue10, tv_startValue20, tv_startValue21, the_goal.start_value);
        updateValue(ll_0unitEnd, ll_1unitEnd, ll_2unitEnd, et_endValue00, et_endValue10, et_endValue20, et_endValue21, tv_endValue10, tv_endValue20, tv_endValue21, the_goal.end_value);
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
        the_goal = goalUuid == null ? null : state.findGoal(goalUuid, goalStart, goalType);
        the_user = state.findUser(userUuid);
        last = state.lastSelectedWeight();
        if (savedInstanceState != null) {
            start_date_millis = savedInstanceState.getLong(STATE_START_DATE, -1);
            end_date_millis = savedInstanceState.getLong(STATE_END_DATE, -1);
        }
        binding = FragmentEditGoalBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        //Close keyboard when clicking any other item on screen
        KeyboardUtils.dismissOnTouchOutsideInputs(rootView, requireActivity());

        sp_type = binding.spType;
        if (the_goal != null)
        {
            sp_type.setEnabled(false);
        }
        et_start_date = binding.etStartDate;
        et_end_date = binding.etEndDate;

        ll_0unitStart = binding.ll0unitStart;
        ll_1unitStart = binding.ll1unitStart;
        ll_2unitStart = binding.ll2unitStart;
        ll_0unitEnd = binding.ll0unitEnd;
        ll_1unitEnd = binding.ll1unitEnd;
        ll_2unitEnd = binding.ll2unitEnd;
        et_startValue00 = binding.etStartValue00;
        et_startValue10 = binding.etStartValue10;
        et_startValue20 = binding.etStartValue20;
        et_startValue21 = binding.etStartValue21;
        tv_startValue10 = binding.tvStartValue10;
        tv_startValue20 = binding.tvStartValue20;
        tv_startValue21 = binding.tvStartValue21;
        et_endValue00 = binding.etEndValue00;
        et_endValue10 = binding.etEndValue10;
        et_endValue20 = binding.etEndValue20;
        et_endValue21 = binding.etEndValue21;
        tv_endValue10 = binding.tvEndValue10;
        tv_endValue20 = binding.tvEndValue20;
        tv_endValue21 = binding.tvEndValue21;
        cp = binding.picker;

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        this.needs_to_sync = savedInstanceState == null;

        et_start_date.setOnFocusChangeListener((view, b) -> {
            if ((b) && (getActivity() != null)) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(start_date_millis);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    Calendar c1 = Calendar.getInstance();
                    c1.set(i, i2, i3, 0, 0, 0);
                    c1.set(Calendar.MILLISECOND, 0);
                    start_date_millis = c1.getTimeInMillis();
                    et_start_date.setText(DateUtils.formatDateTime(getActivity(), start_date_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
                }, year, month, day);
                dpd.setTitle(R.string.edit_goal_fragment_start);
                dpd.show();
            }
        });

        et_end_date.setOnFocusChangeListener((view, b) -> {
            if ((b) && (getActivity() != null)) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(end_date_millis);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    Calendar c12 = Calendar.getInstance();
                    c12.set(i, i2, i3, 0, 0, 0);
                    c12.set(Calendar.MILLISECOND, 0);
                    end_date_millis = c12.getTimeInMillis();
                    et_end_date.setText(DateUtils.formatDateTime(getActivity(), end_date_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
                }, year, month, day);
                dpd.setTitle(R.string.edit_goal_fragment_end);
                dpd.show();
            }
        });

        if (sp_type.isEnabled()) {
            sp_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    the_goal.type = Metric.fromGoalPosition(position);
                    double value = last == null ? 0
                            : the_goal.type.goalValue(last, the_goal.show_fat_mass);
                    the_goal.start_value = the_goal.end_value = value == -1 ? 0 : value;
                    setValues();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {}
            });
        }

        return rootView;
    }

    @Override public void onDestroyView() {
        sp_type = null;
        et_start_date = et_end_date = null;
        ll_0unitStart = ll_1unitStart = ll_2unitStart = null;
        ll_0unitEnd = ll_1unitEnd = ll_2unitEnd = null;
        et_startValue00 = et_startValue10 = et_startValue20 = et_startValue21 = null;
        tv_startValue10 = tv_startValue20 = tv_startValue21 = null;
        et_endValue00 = et_endValue10 = et_endValue20 = et_endValue21 = null;
        tv_endValue10 = tv_endValue20 = tv_endValue21 = null;
        cp = null;
        binding = null;
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        if (needs_to_sync) {
            if (the_goal != null)
            {
                sp_type.setSelection(the_goal.type.getMetricCode() - 1);
            }
            setValues();
            needs_to_sync = false;
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_START_DATE, start_date_millis);
        outState.putLong(STATE_END_DATE, end_date_millis);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_edit_goal_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_editgoal_cancel) {
            if (getActivity() != null)
                ((MainActivity) getActivity()).closeEditGoalFragment(null);
            return true;
        } else if (itemId == R.id.action_editgoal_done) {
            Goal goal;
            if (((goal = checkValues()) != null) && (getActivity() != null)) {
                ((MainActivity) getActivity()).closeEditGoalFragment(goal);
            }
            return true;
        }
        return false;
    }
}
