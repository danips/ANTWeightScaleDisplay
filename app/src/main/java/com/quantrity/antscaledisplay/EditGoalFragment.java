package com.quantrity.antscaledisplay;

import android.app.Activity;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.larswerkman.holocolorpicker.ColorPicker;

import java.util.Calendar;

public class EditGoalFragment extends Fragment {
    private final static String TAG = "EditGoalFragment";

    enum Units {
        NO_UNITS,
        ONE_UNIT,
        ONE_UNIT_WEIGHT,
        TWO_UNITS_WEIGHT
    }

    //Data fields
    Goal the_goal;
    User the_user;
    private Weight last;

    private boolean needs_to_sync;

    private Spinner sp_type;
    private EditText et_startdate;
    private long startdate_millis = -1;
    private EditText et_enddate;
    private long enddate_millis = -1;
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


    public EditGoalFragment() {}

    private static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if ((activity.getCurrentFocus() != null) && (inputMethodManager != null)) inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    private void setupUI(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if(!(view instanceof EditText) && (getActivity() != null)) {
            view.setOnTouchListener((v, event) -> {
                if (getActivity() != null) hideSoftKeyboard(getActivity());
                return false;
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    private Goal checkValues() {
        Goal tmp = new Goal();

        tmp.uuid = ((MainActivity) getActivity()).getSelectedUser().uuid;
        Units units;
        switch (sp_type.getSelectedItemPosition())
        {
            case 1:
                tmp.type = Metric.BMI;
                units = Units.NO_UNITS;
                break;
            case 2:
                tmp.type = Metric.PERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 3:
                tmp.type = Metric.PERCENTHYDRATION;
                units = Units.ONE_UNIT;
                break;
            case 4:
                tmp.type = Metric.BONEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 5:
                tmp.type = Metric.MUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 6:
                tmp.type = Metric.PHYSIQUERATING;
                units = Units.NO_UNITS;
                break;
            case 7:
                tmp.type = Metric.VISCERALFATRATING;
                units = Units.NO_UNITS;
                break;
            case 8:
                tmp.type = Metric.METABOLICAGE;
                units = Units.ONE_UNIT;
                break;
            case 9:
                tmp.type = Metric.ACTIVEMET;
                units = Units.ONE_UNIT;
                break;
            case 10:
                tmp.type = Metric.BASALMET;
                units = Units.ONE_UNIT;
                break;
            case 11:
                tmp.type = Metric.TRUNKPERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 12:
                tmp.type = Metric.TRUNKMUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 13:
                tmp.type = Metric.LEFTARMPERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 14:
                tmp.type = Metric.LEFTARMMUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 15:
                tmp.type = Metric.RIGHTARMPERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 16:
                tmp.type = Metric.RIGHTARMMUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 17:
                tmp.type = Metric.LEFTLEGPERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 18:
                tmp.type = Metric.LEFTLEGMUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 19:
                tmp.type = Metric.RIGHTLEGPERCENTFAT;
                units = (the_goal.show_fat_mass) ? Units.ONE_UNIT_WEIGHT : Units.ONE_UNIT;
                break;
            case 20:
                tmp.type = Metric.RIGHTLEGMUSCLEMASS;
                units = Units.ONE_UNIT_WEIGHT;
                break;
            case 0:
            default:
                tmp.type = Metric.WEIGHT;
                units = Units.ONE_UNIT_WEIGHT;
        }

        switch (units)
        {
            case NO_UNITS:
                tmp.start_value = MainActivity.parseNumber(et_startValue00);
                tmp.end_value = MainActivity.parseNumber(et_endValue00);
                break;
            case ONE_UNIT:
                tmp.start_value = MainActivity.parseNumber(et_startValue10);
                tmp.end_value = MainActivity.parseNumber(et_endValue10);
                break;
            case ONE_UNIT_WEIGHT:
                tmp.start_value = the_user.calc_mass(et_startValue10, 1, false);
                tmp.end_value = the_user.calc_mass(et_endValue10, 1, false);
                break;
            case TWO_UNITS_WEIGHT:
                tmp.end_value = MainActivity.parseNumber(et_endValue20) * 14 + MainActivity.parseNumber(et_endValue21);
                tmp.end_value = the_user.calc_mass(tmp.end_value, 1, false);
                tmp.end_value = MainActivity.parseNumber(et_endValue20) * 14 + MainActivity.parseNumber(et_endValue21);
                tmp.end_value = the_user.calc_mass(tmp.end_value, 1, false);
                break;

            default:
        }

        tmp.start_date = startdate_millis;
        tmp.end_date = enddate_millis;
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

        et_startdate.setText(DateUtils.formatDateTime(getActivity(), the_goal.start_date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        startdate_millis = the_goal.start_date;
        et_enddate.setText(DateUtils.formatDateTime(getActivity(), the_goal.end_date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        enddate_millis = the_goal.end_date;
        cp.setColor(the_goal.color);

        Units units;
        switch (the_goal.type)
        {
            case BMI:
            case VISCERALFATRATING:
                et_startValue00.setText(String.format("%.01f", the_goal.start_value));
                et_endValue00.setText(String.format("%.01f", the_goal.end_value));
                units = Units.NO_UNITS;
                break;

            case PHYSIQUERATING:
                et_startValue00.setText(String.format("%.00f", the_goal.start_value));
                et_endValue00.setText(String.format("%.00f", the_goal.end_value));
                units = Units.NO_UNITS;
                break;

            case METABOLICAGE:
                et_startValue10.setText(String.format("%.0f", the_goal.start_value));
                et_endValue10.setText(String.format("%.0f", the_goal.end_value));
                tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_years_tag));
                tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_years_tag));
                units = Units.ONE_UNIT;
                break;

            case ACTIVEMET:
            case BASALMET:
                et_startValue10.setText(String.format("%.0f", the_goal.start_value));
                et_endValue10.setText(String.format("%.0f", the_goal.end_value));
                tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_kcal_tag));
                tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_kcal_tag));
                units = Units.ONE_UNIT;
                break;

            case PERCENTHYDRATION:
                et_startValue10.setText(String.format("%.1f", the_goal.start_value));
                et_endValue10.setText(String.format("%.1f", the_goal.end_value));
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
                        et_startValue10.setText(String.format("%.1f", the_goal.start_value * 2.20462262));
                        et_endValue10.setText(String.format("%.1f", the_goal.end_value * 2.20462262));
                        tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        units = Units.ONE_UNIT_WEIGHT;
                    } else if (the_user.mass_unit == User.MassUnit.ST) {
                        double lbs = the_goal.start_value * 2.20462262;
                        double divisor = (float)Math.floor(lbs / 14);
                        double remainder = lbs % 14;
                        et_startValue20.setText(String.format("%.0f", divisor));
                        et_startValue21.setText(String.format("%.1f", remainder));
                        tv_startValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                        tv_startValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));

                        lbs = the_goal.end_value * 2.20462262;
                        divisor = (float)Math.floor(lbs / 14);
                        remainder = lbs % 14;
                        et_endValue20.setText(String.format("%.0f", divisor));
                        et_endValue21.setText(String.format("%.1f", remainder));
                        tv_endValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                        tv_endValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                        units = Units.TWO_UNITS_WEIGHT;
                    } else {
                        et_startValue10.setText(String.format("%.1f", the_goal.start_value));
                        et_endValue10.setText(String.format("%.1f", the_goal.end_value));
                        tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                        tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_kg_tag));
                        units = Units.ONE_UNIT_WEIGHT;
                    }
                }
                else
                {
                    et_startValue10.setText(String.format("%.1f", the_goal.start_value));
                    et_endValue10.setText(String.format("%.1f", the_goal.end_value));
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
                    et_startValue10.setText(String.format("%.1f", the_goal.start_value * 2.20462262));
                    et_endValue10.setText(String.format("%.1f", the_goal.end_value * 2.20462262));
                    tv_startValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    tv_endValue10.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    units = Units.ONE_UNIT_WEIGHT;
                } else if (the_user.mass_unit == User.MassUnit.ST) {
                    double lbs = the_goal.start_value * 2.20462262;
                    double divisor = (float)Math.floor(lbs / 14);
                    double remainder = lbs % 14;
                    et_startValue20.setText(String.format("%.0f", divisor));
                    et_startValue21.setText(String.format("%.1f", remainder));
                    tv_startValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                    tv_startValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));

                    lbs = the_goal.end_value * 2.20462262;
                    divisor = (float)Math.floor(lbs / 14);
                    remainder = lbs % 14;
                    et_endValue20.setText(String.format("%.0f", divisor));
                    et_endValue21.setText(String.format("%.1f", remainder));
                    tv_endValue20.setText(getResources().getString(R.string.weight_edit_fragment_st_tag));
                    tv_endValue21.setText(getResources().getString(R.string.weight_edit_fragment_lb_tag));
                    units = Units.TWO_UNITS_WEIGHT;
                } else {
                    et_startValue10.setText(String.format("%.1f", the_goal.start_value));
                    et_endValue10.setText(String.format("%.1f", the_goal.end_value));
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
            case ONE_UNIT:
            case ONE_UNIT_WEIGHT:
            default:
                ll_1unitStart.setVisibility(View.VISIBLE);
                ll_1unitEnd.setVisibility(View.VISIBLE);
                break;
            case TWO_UNITS_WEIGHT:
                ll_2unitStart.setVisibility(View.VISIBLE);
                ll_2unitEnd.setVisibility(View.VISIBLE);
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
                        value *= 2.20462262;
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
                    value *= 2.20462262;
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
                et_Value00.setText(String.format("%.1f", value));
                break;
            case 2:
                ll_0unit.setVisibility(View.GONE);
                ll_1unit.setVisibility(View.GONE);
                ll_2unit.setVisibility(View.VISIBLE);
                double lbs = value * 2.20462262;
                double divisor = (float)Math.floor(lbs / 14);
                double remainder = lbs % 14;
                et_Value20.setText(String.format("%.0f", divisor));
                et_Value21.setText(String.format("%.1f", remainder));
                break;
            case 1:
            default:
                ll_0unit.setVisibility(View.GONE);
                ll_1unit.setVisibility(View.VISIBLE);
                ll_2unit.setVisibility(View.GONE);
                et_Value10.setText(String.format("%.1f", value));
                break;
        }


    }

    private void resetValues() {
        last = ((MainActivity) getActivity()).getLastHistorySelectedUser();
        cp.setColor(Color.RED);
        the_goal.color = Color.RED;
        the_goal.type = Metric.WEIGHT;
        the_goal.show_fat_mass = the_user.show_fat_mass;
        sp_type.setSelection(0);
        Calendar c = Calendar.getInstance();
        startdate_millis = c.getTimeInMillis();
        the_goal.start_date = startdate_millis;
        c.add(Calendar.MONTH, 3);
        enddate_millis = c.getTimeInMillis();
        the_goal.end_date = enddate_millis;
        et_startdate.setText(DateUtils.formatDateTime(getActivity(), startdate_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        et_enddate.setText(DateUtils.formatDateTime(getActivity(), enddate_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));

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
        View rootView = inflater.inflate(R.layout.fragment_edit_goal, container, false);

        //Close keyboard when clicking any other item on screen
        setupUI(rootView);

        sp_type = rootView.findViewById(R.id.sp_type);
        if (the_goal != null)
        {
            sp_type.setEnabled(false);
        }
        et_startdate = rootView.findViewById(R.id.et_startdate);
        et_enddate = rootView.findViewById(R.id.et_enddate);

        ll_0unitStart = rootView.findViewById(R.id.ll_0unitStart);
        ll_1unitStart = rootView.findViewById(R.id.ll_1unitStart);
        ll_2unitStart = rootView.findViewById(R.id.ll_2unitStart);
        ll_0unitEnd = rootView.findViewById(R.id.ll_0unitEnd);
        ll_1unitEnd = rootView.findViewById(R.id.ll_1unitEnd);
        ll_2unitEnd = rootView.findViewById(R.id.ll_2unitEnd);
        et_startValue00 = rootView.findViewById(R.id.et_startValue00);
        et_startValue10 = rootView.findViewById(R.id.et_startValue10);
        et_startValue20 = rootView.findViewById(R.id.et_startValue20);
        et_startValue21 = rootView.findViewById(R.id.et_startValue21);
        tv_startValue10 = rootView.findViewById(R.id.tv_startValue10);
        tv_startValue20 = rootView.findViewById(R.id.tv_startValue20);
        tv_startValue21 = rootView.findViewById(R.id.tv_startValue21);
        et_endValue00 = rootView.findViewById(R.id.et_endValue00);
        et_endValue10 = rootView.findViewById(R.id.et_endValue10);
        et_endValue20 = rootView.findViewById(R.id.et_endValue20);
        et_endValue21 = rootView.findViewById(R.id.et_endValue21);
        tv_endValue10 = rootView.findViewById(R.id.tv_endValue10);
        tv_endValue20 = rootView.findViewById(R.id.tv_endValue20);
        tv_endValue21 = rootView.findViewById(R.id.tv_endValue21);
        cp = rootView.findViewById(R.id.picker);

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        this.needs_to_sync = true;

        et_startdate.setOnFocusChangeListener((view, b) -> {
            if ((b) && (getActivity() != null)) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(startdate_millis);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    Calendar c1 = Calendar.getInstance();
                    c1.set(i, i2, i3, 0, 0, 0);
                    c1.set(Calendar.MILLISECOND, 0);
                    startdate_millis = c1.getTimeInMillis();
                    et_startdate.setText(DateUtils.formatDateTime(getActivity(), startdate_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
                }, year, month, day);
                dpd.setTitle(R.string.edit_goal_fragment_start);
                dpd.show();
            }
        });

        et_enddate.setOnFocusChangeListener((view, b) -> {
            if ((b) && (getActivity() != null)) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(enddate_millis);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    Calendar c12 = Calendar.getInstance();
                    c12.set(i, i2, i3, 0, 0, 0);
                    c12.set(Calendar.MILLISECOND, 0);
                    enddate_millis = c12.getTimeInMillis();
                    et_enddate.setText(DateUtils.formatDateTime(getActivity(), enddate_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
                }, year, month, day);
                dpd.setTitle(R.string.edit_goal_fragment_end);
                dpd.show();
            }
        });

        if (sp_type.isEnabled()) {
            sp_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    the_goal.type = Metric.values()[position + 2];
                    switch (position) {
                        case 1:
                            the_goal.type = Metric.BMI;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : (last.weight / Math.pow(last.height / 100, 2));
                            break;
                        case 2:
                            the_goal.type = Metric.PERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.percentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.percentFat;
                            }
                            break;
                        case 3:
                            the_goal.type = Metric.PERCENTHYDRATION;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.percentHydration;
                            break;
                        case 4:
                            the_goal.type = Metric.BONEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.boneMass;
                            break;
                        case 5:
                            the_goal.type = Metric.MUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.muscleMass;
                            break;
                        case 6:
                            the_goal.type = Metric.PHYSIQUERATING;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.physiqueRating;
                            break;
                        case 7:
                            the_goal.type = Metric.VISCERALFATRATING;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.visceralFatRating;
                            break;
                        case 8:
                            the_goal.type = Metric.METABOLICAGE;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.metabolicAge;
                            break;
                        case 9:
                            the_goal.type = Metric.ACTIVEMET;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.activeMet;
                            break;
                        case 10:
                            the_goal.type = Metric.BASALMET;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.basalMet;
                            break;
                        case 11:
                            the_goal.type = Metric.TRUNKPERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.trunkPercentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.trunkPercentFat;
                            }
                            break;
                        case 12:
                            the_goal.type = Metric.TRUNKMUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.trunkMuscleMass;
                            break;
                        case 13:
                            the_goal.type = Metric.LEFTARMPERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftArmPercentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftArmPercentFat;
                            }
                            break;
                        case 14:
                            the_goal.type = Metric.LEFTARMMUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftArmMuscleMass;
                            break;
                        case 15:
                            the_goal.type = Metric.RIGHTARMPERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightArmPercentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightArmPercentFat;
                            }
                            break;
                        case 16:
                            the_goal.type = Metric.RIGHTARMMUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightLegMuscleMass;
                            break;
                        case 17:
                            the_goal.type = Metric.LEFTLEGPERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftLegPercentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftLegPercentFat;
                            }
                            break;
                        case 18:
                            the_goal.type = Metric.LEFTLEGMUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.leftLegMuscleMass;
                            break;
                        case 19:
                            the_goal.type = Metric.RIGHTLEGPERCENTFAT;
                            if (the_goal.show_fat_mass)
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightLegPercentFat * last.weight / 100;
                            }
                            else
                            {
                                the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightLegPercentFat;
                            }
                            break;
                        case 20:
                            the_goal.type = Metric.RIGHTLEGMUSCLEMASS;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.rightLegMuscleMass;
                            break;
                        case 0:
                        default:
                            the_goal.type = Metric.WEIGHT;
                            the_goal.start_value = the_goal.end_value = (last == null) ? 0 : last.weight;
                    }
                    setValues();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {}
            });
        }

        return rootView;
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
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_edit_goal_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
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
        return super.onOptionsItemSelected(item);
    }
}
