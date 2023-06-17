package com.quantrity.antscaledisplay;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

public class EditWeightFragment extends Fragment {
    private static final String TAG = "EditWeightFragment";

    private TextView dateTV = null;
    private EditText weightTV = null;
    private EditText trunkPercentFatTV = null;
    private EditText trunkMuscleMassTV = null;
    private EditText leftArmPercentFatTV = null;
    private EditText leftArmMuscleMassTV = null;
    private EditText rightArmPercentFatTV = null;
    private EditText rightArmMuscleMassTV = null;
    private EditText leftLegPercentFatTV = null;
    private EditText leftLegMuscleMassTV = null;
    private EditText rightLegPercentFatTV = null;
    private EditText rightLegMuscleMassTV = null;
    private EditText percentFatTV = null;
    private EditText percentHydrationTV = null;
    private EditText boneMassTV = null;
    private EditText muscleMassTV = null;
    private EditText physiqueRatingTV = null;
    private EditText visceralFatRatingTV = null;
    private EditText metabolicAgeTV = null;
    private EditText activeMetTV = null;
    private EditText basalMetTV = null;
    private TextView weightUnits = null;
    private TextView trunkPercentFatUnits = null;
    private TextView trunkMuscleMassUnits = null;
    private TextView leftArmPercentFatUnits = null;
    private TextView leftArmMuscleMassUnits = null;
    private TextView rightArmPercentFatUnits = null;
    private TextView rightArmMuscleMassUnits = null;
    private TextView leftLegPercentFatUnits = null;
    private TextView leftLegMuscleMassUnits = null;
    private TextView rightLegPercentFatUnits = null;
    private TextView rightLegMuscleMassUnits = null;
    private TextView percentFatUnits = null;
    private TextView boneMassUnits = null;
    private TextView muscleMassUnits = null;

    Weight the_weight, old_weight;
    User the_user;
    boolean edit;

    public EditWeightFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_weight, container, false);

        dateTV = rootView.findViewById(R.id.dateTV);
        weightTV = rootView.findViewById(R.id.weightTV);
        trunkPercentFatTV = rootView.findViewById(R.id.trunkPercentFatTV);
        trunkMuscleMassTV = rootView.findViewById(R.id.trunkMuscleMassTV);
        leftArmPercentFatTV = rootView.findViewById(R.id.leftArmPercentFatTV);
        leftArmMuscleMassTV = rootView.findViewById(R.id.leftArmMuscleMassTV);
        rightArmPercentFatTV = rootView.findViewById(R.id.rightArmPercentFatTV);
        rightArmMuscleMassTV = rootView.findViewById(R.id.rightArmMuscleMassTV);
        leftLegPercentFatTV = rootView.findViewById(R.id.leftLegPercentFatTV);
        leftLegMuscleMassTV = rootView.findViewById(R.id.leftLegMuscleMassTV);
        rightLegPercentFatTV = rootView.findViewById(R.id.rightLegPercentFatTV);
        rightLegMuscleMassTV = rootView.findViewById(R.id.rightLegMuscleMassTV);
        percentFatTV = rootView.findViewById(R.id.percentFatTV);
        percentHydrationTV = rootView.findViewById(R.id.percentHydrationTV);
        boneMassTV = rootView.findViewById(R.id.boneMassTV);
        muscleMassTV = rootView.findViewById(R.id.muscleMassTV);
        physiqueRatingTV = rootView.findViewById(R.id.physiqueRatingTV);
        visceralFatRatingTV = rootView.findViewById(R.id.visceralFatRatingTV);
        metabolicAgeTV = rootView.findViewById(R.id.metabolicAgeTV);
        activeMetTV = rootView.findViewById(R.id.activeMetTV);
        basalMetTV = rootView.findViewById(R.id.basalMetTV);
        weightUnits = rootView.findViewById(R.id.weightUnits);
        trunkPercentFatUnits = rootView.findViewById(R.id.trunkPercentFatUnits);
        trunkMuscleMassUnits = rootView.findViewById(R.id.trunkMuscleMassUnits);
        leftArmPercentFatUnits = rootView.findViewById(R.id.leftArmPercentFatUnits);
        leftArmMuscleMassUnits = rootView.findViewById(R.id.leftArmMuscleMassUnits);
        rightArmPercentFatUnits = rootView.findViewById(R.id.rightArmPercentFatUnits);
        rightArmMuscleMassUnits = rootView.findViewById(R.id.rightArmMuscleMassUnits);
        leftLegPercentFatUnits = rootView.findViewById(R.id.leftLegPercentFatUnits);
        leftLegMuscleMassUnits = rootView.findViewById(R.id.leftLegMuscleMassUnits);
        rightLegPercentFatUnits = rootView.findViewById(R.id.rightLegPercentFatUnits);
        rightLegMuscleMassUnits = rootView.findViewById(R.id.rightLegMuscleMassUnits);
        percentFatUnits = rootView.findViewById(R.id.percentFatUnits);
        boneMassUnits = rootView.findViewById(R.id.boneMassUnits);
        muscleMassUnits = rootView.findViewById(R.id.muscleMassUnits);

        if (DecimalFormatSymbols.getInstance().getDecimalSeparator() == ',')
        {
            TextWatcher tw = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (editable.toString().contains(".")) {
                        Editable ab = new SpannableStringBuilder(editable.toString().replace(".", ","));
                        editable.replace(0, editable.length(), ab);
                    }
                }
            };

            weightTV.addTextChangedListener(tw);
            trunkPercentFatTV.addTextChangedListener(tw);
            trunkMuscleMassTV.addTextChangedListener(tw);
            leftArmPercentFatTV.addTextChangedListener(tw);
            leftArmMuscleMassTV.addTextChangedListener(tw);
            rightArmPercentFatTV.addTextChangedListener(tw);
            rightArmMuscleMassTV.addTextChangedListener(tw);
            leftLegPercentFatTV.addTextChangedListener(tw);
            leftLegMuscleMassTV.addTextChangedListener(tw);
            rightLegPercentFatTV.addTextChangedListener(tw);
            rightLegMuscleMassTV.addTextChangedListener(tw);
            percentFatTV.addTextChangedListener(tw);
            percentHydrationTV.addTextChangedListener(tw);
            boneMassTV.addTextChangedListener(tw);
            muscleMassTV.addTextChangedListener(tw);
            visceralFatRatingTV.addTextChangedListener(tw);
        }

        dateTV.setOnClickListener(view -> {
            if (getActivity() != null) {
                Calendar c = Calendar.getInstance();
                if (the_weight.date != -1) c.setTimeInMillis(the_weight.date);
                else c.add(Calendar.YEAR, -18);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                final int hour = c.get(Calendar.HOUR_OF_DAY);
                final int minute = c.get(Calendar.MINUTE);
                final int second = c.get(Calendar.SECOND);
                final int ms = c.get(Calendar.MILLISECOND);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    final int year1 = i;
                    final int month1 = i2;
                    final int day1 = i3;
                    TimePickerDialog tpd = new TimePickerDialog(getActivity(), (timePicker, i12, i1) -> {
                        Calendar c1 = Calendar.getInstance();
                        c1.set(year1, month1, day1, i12, i1, second);
                        c1.set(Calendar.MILLISECOND, ms);
                        the_weight.date = c1.getTimeInMillis();
                        dateTV.setText(DateUtils.formatDateTime(getContext(), the_weight.date, DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
                    }, hour, minute, DateFormat.is24HourFormat(getActivity()));
                    tpd.show();
                }, year, month, day);
                dpd.show();
            }
        });

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        updateUi();
    }

    // updates UI to reflect model
    private void updateUi() {
        if (getActivity() == null) {
            Log.v(TAG, "********** updateUi with getActivity NULL **********");
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (the_user == null)
            {
                if (getActivity() == null)
                {
                    return;
                }
                if ((the_user = ((MainActivity)getActivity()).getSelectedUser()) == null)
                {
                    ((MainActivity)getActivity()).closeEditWeightFragment((MainActivity)getActivity(),null, null, edit, false);
                    return;
                }
            }
            weightTV.setError(null);
            if (the_weight == null)
            {
                the_weight = new Weight();
                the_weight.date = System.currentTimeMillis();

                weightTV.getText().clear();
                trunkPercentFatTV.getText().clear();
                trunkMuscleMassTV.getText().clear();
                leftArmPercentFatTV.getText().clear();
                leftArmMuscleMassTV.getText().clear();
                rightArmPercentFatTV.getText().clear();
                rightArmMuscleMassTV.getText().clear();
                leftLegPercentFatTV.getText().clear();
                leftLegMuscleMassTV.getText().clear();
                rightLegPercentFatTV.getText().clear();
                rightLegMuscleMassTV.getText().clear();
                percentFatTV.getText().clear();
                percentHydrationTV.getText().clear();
                boneMassTV.getText().clear();
                muscleMassTV.getText().clear();
                physiqueRatingTV.getText().clear();
                visceralFatRatingTV.getText().clear();
                metabolicAgeTV.getText().clear();
                basalMetTV.getText().clear();
            }
            else {
                if (the_weight.weight != -1) weightTV.setText(the_user.printMass(getContext(), the_weight.weight, false, true));
                if (the_weight.trunkMuscleMass != -1) trunkMuscleMassTV.setText(the_user.printMass(getContext(), the_weight.trunkMuscleMass, false, true));
                if (the_weight.leftArmMuscleMass != -1) leftArmMuscleMassTV.setText(the_user.printMass(getContext(), the_weight.leftArmMuscleMass, false, true));
                if (the_weight.rightArmMuscleMass != -1) rightArmMuscleMassTV.setText(the_user.printMass(getContext(), the_weight.rightArmMuscleMass, false, true));
                if (the_weight.leftLegMuscleMass != -1) leftLegMuscleMassTV.setText(the_user.printMass(getContext(), the_weight.leftLegMuscleMass, false, true));
                if (the_weight.rightLegMuscleMass != -1) rightLegMuscleMassTV.setText(the_user.printMass(getContext(), the_weight.rightLegMuscleMass, false, true));
                if (the_weight.percentHydration != -1) percentHydrationTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.percentHydration));
                if (the_weight.boneMass != -1) boneMassTV.setText(the_user.printMass(getContext(), the_weight.boneMass, false, true));
                if (the_weight.muscleMass != -1) muscleMassTV.setText(the_user.printMass(getContext(), the_weight.muscleMass, false, true));
                if (the_weight.physiqueRating != -1) physiqueRatingTV.setText(String.format(Locale.getDefault(),"%d", the_weight.physiqueRating));
                if (the_weight.visceralFatRating != -1) visceralFatRatingTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.visceralFatRating));
                if (the_weight.metabolicAge != -1) metabolicAgeTV.setText(String.format(Locale.getDefault(),"%d", the_weight.metabolicAge));
                if (the_weight.basalMet != -1) basalMetTV.setText(String.format(Locale.getDefault(),"%1$.0f", the_weight.basalMet));

                if (the_user.show_fat_mass)
                {
                    if (the_weight.percentFat != -1) percentFatTV.setText(the_user.printMass(getContext(), the_weight.percentFat * the_weight.weight / 100, false, true));
                    if (the_weight.trunkPercentFat != -1) trunkPercentFatTV.setText(the_user.printMass(getContext(), the_weight.trunkPercentFat * the_weight.weight / 100, false, true));
                    if (the_weight.rightArmPercentFat != -1) rightArmPercentFatTV.setText(the_user.printMass(getContext(), the_weight.rightArmPercentFat * the_weight.weight / 100, false, true));
                    if (the_weight.leftArmPercentFat != -1) leftArmPercentFatTV.setText(the_user.printMass(getContext(), the_weight.leftArmPercentFat * the_weight.weight / 100, false, true));
                    if (the_weight.leftLegPercentFat != -1) leftLegPercentFatTV.setText(the_user.printMass(getContext(), the_weight.leftLegPercentFat * the_weight.weight / 100, false, true));
                    if (the_weight.rightLegPercentFat != -1) rightLegPercentFatTV.setText(the_user.printMass(getContext(), the_weight.rightLegPercentFat * the_weight.weight / 100, false, true));
                }
                else
                {
                    if (the_weight.percentFat != -1) percentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.percentFat));
                    if (the_weight.trunkPercentFat != -1) trunkPercentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.trunkPercentFat));
                    if (the_weight.rightArmPercentFat != -1) rightArmPercentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.rightArmPercentFat));
                    if (the_weight.leftArmPercentFat != -1) leftArmPercentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.leftArmPercentFat));
                    if (the_weight.leftLegPercentFat != -1) leftLegPercentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.leftLegPercentFat));
                    if (the_weight.rightLegPercentFat != -1) rightLegPercentFatTV.setText(String.format(Locale.getDefault(),"%1$.2f", the_weight.rightLegPercentFat));
                }
            }
            dateTV.setText(DateUtils.formatDateTime(getContext(), the_weight.date, DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

            weightTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            trunkMuscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            leftArmMuscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            rightArmMuscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            leftLegMuscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            rightLegMuscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            trunkPercentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            leftArmPercentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            rightArmPercentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            leftLegPercentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            rightLegPercentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            percentFatTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            if (!the_user.show_fat_mass) {
                trunkPercentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
                leftArmPercentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
                rightArmPercentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
                leftLegPercentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
                rightLegPercentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
                percentFatUnits.setText(R.string.weight_edit_fragment_percent_tag);
            }

            if (the_user.mass_unit == User.MassUnit.KG)
            {
                weightUnits.setText(R.string.weight_edit_fragment_kg_tag);
                trunkMuscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                leftArmMuscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                rightArmMuscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                leftLegMuscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                rightLegMuscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                boneMassUnits.setText(R.string.weight_edit_fragment_kg_tag);
                muscleMassUnits.setText(R.string.weight_edit_fragment_kg_tag);

                if (the_user.show_fat_mass) {
                    trunkPercentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                    leftArmPercentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                    rightArmPercentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                    leftLegPercentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                    rightLegPercentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                    percentFatUnits.setText(R.string.weight_edit_fragment_kg_tag);
                }
            }
            else if ((the_user.mass_unit == User.MassUnit.LB) || (the_user.mass_unit == User.MassUnit.ST))
            {
                weightUnits.setText(R.string.weight_edit_fragment_lb_tag);
                trunkMuscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                leftArmMuscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                rightArmMuscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                leftLegMuscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                rightLegMuscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                boneMassUnits.setText(R.string.weight_edit_fragment_lb_tag);
                muscleMassUnits.setText(R.string.weight_edit_fragment_lb_tag);

                if (the_user.show_fat_mass) {
                    trunkPercentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                    leftArmPercentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                    rightArmPercentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                    leftLegPercentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                    rightLegPercentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                    percentFatUnits.setText(R.string.weight_edit_fragment_lb_tag);
                }
            }
            percentHydrationTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            boneMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            muscleMassTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            physiqueRatingTV.setHint("0");
            visceralFatRatingTV.setHint(String.format(Locale.getDefault(), "%.2f", 0f));
            metabolicAgeTV.setHint("0");
            activeMetTV.setHint("0");
            basalMetTV.setHint("0");
        });
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (view != null) {
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).setSelectedUser((User) adapterView.getItemAtPosition(i));
                    the_user = (User)adapterView.getItemAtPosition(i);
                    updateUi();
                }
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_edit_weight_menu, menu);
        if (getActivity() != null) {
            ((MainActivity) getActivity()).addUsersSpinner(menu, oisListener);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == R.id.action_editweight_cancel) {
            if (getActivity() != null)
                ((MainActivity) getActivity()).closeEditWeightFragment((MainActivity) getActivity(), null, null, edit, false);
            return true;
        } else if (itemId == R.id.action_editweight_done) {
            if (checkValues() && (getActivity() != null)) {
                ((MainActivity) getActivity()).closeEditWeightFragment((MainActivity) getActivity(), the_weight, the_user, edit, !the_weight.equals(old_weight));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkValues()
    {
        if (TextUtils.isEmpty(weightTV.getText()))
        {
            weightTV.setError(getString(R.string.weight_edit_empty_value_error));
            return false;
        }

        if (!the_user.uuid.equals(the_weight.uuid))
        {
            Log.v(TAG, "The user has changed, updating activity level");
            the_weight.activityLevel = the_user.activity_level;
        }

        the_weight.uuid = the_user.uuid;
        the_weight.height = the_user.height_cm;
        the_weight.age = User.calcAge(the_user.birthdate, the_weight.date);
        the_weight.isMale = the_user.isMale;

        the_weight.weight = the_user.calc_mass(weightTV, the_weight.weight,false);
        if (TextUtils.isEmpty(trunkPercentFatTV.getText()))
        {
            the_weight.trunkPercentFat = -1;
        }
        else
        {
            the_weight.trunkPercentFat = the_user.calc_mass(trunkPercentFatTV, the_weight.weight, true);
        }

        if (TextUtils.isEmpty(trunkMuscleMassTV.getText()))
        {
            the_weight.trunkMuscleMass = -1;
        }
        else
        {
            the_weight.trunkMuscleMass = the_user.calc_mass(trunkMuscleMassTV, the_weight.weight, false);
        }
        if (TextUtils.isEmpty(leftArmPercentFatTV.getText()))
        {
            the_weight.leftArmPercentFat = -1;
        }
        else
        {
            the_weight.leftArmPercentFat = the_user.calc_mass(leftArmPercentFatTV, the_weight.weight, true);
        }
        if (TextUtils.isEmpty(leftArmPercentFatTV.getText()))
        {
            the_weight.leftArmMuscleMass = -1;
        }
        else
        {
            the_weight.leftArmMuscleMass = the_user.calc_mass(leftArmMuscleMassTV, the_weight.weight,false);
        }
        if (TextUtils.isEmpty(rightArmPercentFatTV.getText()))
        {
            the_weight.rightArmPercentFat = -1;
        }
        else
        {
            the_weight.rightArmPercentFat = the_user.calc_mass(rightArmPercentFatTV, the_weight.weight,true);
        }
        if (TextUtils.isEmpty(rightArmMuscleMassTV.getText()))
        {
            the_weight.rightArmMuscleMass = -1;
        }
        else
        {
            the_weight.rightArmMuscleMass = the_user.calc_mass(rightArmMuscleMassTV, the_weight.weight,false);
        }
        if (TextUtils.isEmpty(leftLegPercentFatTV.getText()))
        {
            the_weight.leftLegPercentFat = -1;
        }
        else
        {
            the_weight.leftLegPercentFat = the_user.calc_mass(leftLegPercentFatTV, the_weight.weight,true);
        }
        if (TextUtils.isEmpty(leftLegMuscleMassTV.getText()))
        {
            the_weight.leftLegMuscleMass = -1;
        }
        else
        {
            the_weight.leftLegMuscleMass = the_user.calc_mass(leftLegMuscleMassTV, the_weight.weight,false);
        }
        if (TextUtils.isEmpty(rightLegPercentFatTV.getText()))
        {
            the_weight.rightLegPercentFat = -1;
        }
        else
        {
            the_weight.rightLegPercentFat = the_user.calc_mass(rightLegPercentFatTV, the_weight.weight,true);
        }
        if (TextUtils.isEmpty(rightLegMuscleMassTV.getText()))
        {
            the_weight.rightLegMuscleMass = -1;
        }
        else
        {
            the_weight.rightLegMuscleMass = the_user.calc_mass(rightLegMuscleMassTV, the_weight.weight,false);
        }
        if (TextUtils.isEmpty(percentFatTV.getText()))
        {
            the_weight.percentFat = -1;
        }
        else
        {
            the_weight.percentFat = the_user.calc_mass(percentFatTV, the_weight.weight,true);
        }


        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        Number number;

        if (TextUtils.isEmpty(percentHydrationTV.getText()))
        {
            the_weight.percentHydration = -1;
        }
        else
        {
            try {
                number = format.parse(percentHydrationTV.getText().toString());
                the_weight.percentHydration = number.doubleValue();
            } catch (ParseException e) {
                e.printStackTrace();
                the_weight.percentHydration = -1;
            }
        }
        if (TextUtils.isEmpty(boneMassTV.getText()))
        {
            the_weight.boneMass = -1;
        }
        else
        {
            the_weight.boneMass = the_user.calc_mass(boneMassTV, the_weight.weight, false);
        }
        if (TextUtils.isEmpty(muscleMassTV.getText()))
        {
            the_weight.muscleMass = -1;
        }
        else
        {
            the_weight.muscleMass = the_user.calc_mass(muscleMassTV, the_weight.weight, false);
        }
        if (TextUtils.isEmpty(physiqueRatingTV.getText()))
        {
            the_weight.physiqueRating = -1;
        }
        else
        {
            the_weight.physiqueRating = Integer.parseInt(physiqueRatingTV.getText().toString());
        }
        if (TextUtils.isEmpty(visceralFatRatingTV.getText()))
        {
            the_weight.visceralFatRating = -1;
        }
        else
        {

            try {
                number = format.parse(visceralFatRatingTV.getText().toString());
                the_weight.visceralFatRating = number.doubleValue();
            } catch (ParseException e) {
                e.printStackTrace();
                the_weight.visceralFatRating = -1;
            }
        }
        if (TextUtils.isEmpty(metabolicAgeTV.getText()))
        {
            the_weight.metabolicAge = -1;
        }
        else
        {
            the_weight.metabolicAge = Integer.parseInt(metabolicAgeTV.getText().toString());
        }
        if (TextUtils.isEmpty(activeMetTV.getText()))
        {
            the_weight.activeMet = -1;
        }
        else
        {
            the_weight.activeMet = MainActivity.parseNumber(activeMetTV);
        }
        if (TextUtils.isEmpty(basalMetTV.getText()))
        {
            the_weight.basalMet = -1;
        }
        else
        {
            the_weight.basalMet = MainActivity.parseNumber(basalMetTV);
        }


        return true;
    }
}
