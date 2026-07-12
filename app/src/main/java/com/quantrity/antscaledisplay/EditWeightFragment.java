package com.quantrity.antscaledisplay;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.quantrity.antscaledisplay.databinding.FragmentEditWeightBinding;

import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.EnumMap;

public class EditWeightFragment extends Fragment implements MenuProvider {
    private static final String TAG = "EditWeightFragment";
    private static final String ARG_WEIGHT_USER_UUID = "weight_user_uuid";
    private static final String ARG_WEIGHT_DATE = "weight_date";
    private static final String ARG_USER_UUID = "user_uuid";
    private static final String ARG_EDIT = "edit";
    private static final String STATE_WEIGHT_USER_UUID = "state_weight_user_uuid";
    private static final String STATE_WEIGHT_DATE = "state_weight_date";
    private static final String STATE_USER_UUID = "state_user_uuid";

    private Weight the_weight, old_weight;
    private User the_user;
    boolean edit;
    private AppStateViewModel state;
    private boolean preserveRestoredViews;
    private FragmentEditWeightBinding binding;
    private final EnumMap<EditableWeightMetric, MetricField> metricFields =
            new EnumMap<>(EditableWeightMetric.class);

    private static final class MetricField {
        final EditableWeightMetric definition;
        final EditText input;
        final TextView units;

        MetricField(EditableWeightMetric definition, EditText input, TextView units) {
            this.definition = definition;
            this.input = input;
            this.units = units;
        }
    }

    static EditWeightFragment newInstance(String weightUserUuid, long weightDate,
                                          String userUuid, boolean edit) {
        EditWeightFragment fragment = new EditWeightFragment();
        Bundle arguments = new Bundle();
        if (weightUserUuid != null) arguments.putString(ARG_WEIGHT_USER_UUID, weightUserUuid);
        arguments.putLong(ARG_WEIGHT_DATE, weightDate);
        if (userUuid != null) arguments.putString(ARG_USER_UUID, userUuid);
        arguments.putBoolean(ARG_EDIT, edit);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        if (!state.isLoaded()) state.reload();
        Bundle arguments = getArguments();
        String weightUuid = savedInstanceState == null
                ? (arguments == null ? null : arguments.getString(ARG_WEIGHT_USER_UUID))
                : savedInstanceState.getString(STATE_WEIGHT_USER_UUID);
        long weightDate = savedInstanceState == null
                ? (arguments == null ? -1 : arguments.getLong(ARG_WEIGHT_DATE, -1))
                : savedInstanceState.getLong(STATE_WEIGHT_DATE, -1);
        String userUuid = savedInstanceState == null
                ? (arguments == null ? null : arguments.getString(ARG_USER_UUID))
                : savedInstanceState.getString(STATE_USER_UUID);
        edit = arguments != null && arguments.getBoolean(ARG_EDIT, false);
        preserveRestoredViews = savedInstanceState != null;
        the_weight = weightUuid == null ? null : state.findWeight(weightUuid, weightDate);
        the_user = userUuid == null ? state.selectedUser() : state.findUser(userUuid);
        if (savedInstanceState != null && the_weight == null && !edit) {
            the_weight = new Weight();
            the_weight.date = weightDate;
        }
        if (the_weight != null) {
            try {
                old_weight = (Weight) the_weight.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Unable to copy the weight for editing", e);
            }
        }
        binding = FragmentEditWeightBinding.inflate(inflater, container, false);
        bindMetricFields();
        View rootView = binding.getRoot();

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

            for (MetricField field : metricFields.values()) {
                if (field.definition.acceptsDecimalInput()) {
                    field.input.addTextChangedListener(tw);
                }
            }
        }

        binding.dateTV.setOnClickListener(view -> {
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
                        binding.dateTV.setText(DateUtils.formatDateTime(getContext(), the_weight.date, DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
                    }, hour, minute, DateFormat.is24HourFormat(getActivity()));
                    tpd.show();
                }, year, month, day);
                dpd.show();
            }
        });

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return rootView;
    }

    private void bindMetricFields() {
        metricFields.clear();
        bind(EditableWeightMetric.WEIGHT, binding.weightTV, binding.weightUnits);
        bind(EditableWeightMetric.PERCENT_FAT, binding.percentFatTV, binding.percentFatUnits);
        bind(EditableWeightMetric.PERCENT_HYDRATION,
                binding.percentHydrationTV, binding.percentHydrationUnits);
        bind(EditableWeightMetric.BONE_MASS, binding.boneMassTV, binding.boneMassUnits);
        bind(EditableWeightMetric.MUSCLE_MASS, binding.muscleMassTV, binding.muscleMassUnits);
        bind(EditableWeightMetric.PHYSIQUE_RATING,
                binding.physiqueRatingTV, binding.physiqueRatingUnits);
        bind(EditableWeightMetric.VISCERAL_FAT,
                binding.visceralFatRatingTV, binding.visceralFatRatingUnits);
        bind(EditableWeightMetric.METABOLIC_AGE,
                binding.metabolicAgeTV, binding.metabolicAgeUnits);
        bind(EditableWeightMetric.ACTIVE_MET, binding.activeMetTV, binding.activeMetUnits);
        bind(EditableWeightMetric.BASAL_MET, binding.basalMetTV, binding.basalMetUnits);
        bind(EditableWeightMetric.TRUNK_PERCENT_FAT,
                binding.trunkPercentFatTV, binding.trunkPercentFatUnits);
        bind(EditableWeightMetric.TRUNK_MUSCLE_MASS,
                binding.trunkMuscleMassTV, binding.trunkMuscleMassUnits);
        bind(EditableWeightMetric.LEFT_ARM_PERCENT_FAT,
                binding.leftArmPercentFatTV, binding.leftArmPercentFatUnits);
        bind(EditableWeightMetric.LEFT_ARM_MUSCLE_MASS,
                binding.leftArmMuscleMassTV, binding.leftArmMuscleMassUnits);
        bind(EditableWeightMetric.RIGHT_ARM_PERCENT_FAT,
                binding.rightArmPercentFatTV, binding.rightArmPercentFatUnits);
        bind(EditableWeightMetric.RIGHT_ARM_MUSCLE_MASS,
                binding.rightArmMuscleMassTV, binding.rightArmMuscleMassUnits);
        bind(EditableWeightMetric.LEFT_LEG_PERCENT_FAT,
                binding.leftLegPercentFatTV, binding.leftLegPercentFatUnits);
        bind(EditableWeightMetric.LEFT_LEG_MUSCLE_MASS,
                binding.leftLegMuscleMassTV, binding.leftLegMuscleMassUnits);
        bind(EditableWeightMetric.RIGHT_LEG_PERCENT_FAT,
                binding.rightLegPercentFatTV, binding.rightLegPercentFatUnits);
        bind(EditableWeightMetric.RIGHT_LEG_MUSCLE_MASS,
                binding.rightLegMuscleMassTV, binding.rightLegMuscleMassUnits);
    }

    private void bind(EditableWeightMetric definition, EditText input, TextView units) {
        metricFields.put(definition, new MetricField(definition, input, units));
    }

    private MetricField field(EditableWeightMetric definition) {
        MetricField field = metricFields.get(definition);
        if (field == null) throw new IllegalStateException("Missing metric field " + definition);
        return field;
    }

    @Override public void onDestroyView() {
        metricFields.clear();
        binding = null;
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (preserveRestoredViews) preserveRestoredViews = false;
        else updateUi();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (the_weight != null) {
            outState.putString(STATE_WEIGHT_USER_UUID, the_weight.uuid);
            outState.putLong(STATE_WEIGHT_DATE, the_weight.date);
        }
        if (the_user != null) outState.putString(STATE_USER_UUID, the_user.uuid);
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
                if ((the_user = state.selectedUser()) == null)
                {
                    ((MainActivity)getActivity()).closeEditWeightFragment(null, null, edit, false);
                    return;
                }
            }
            field(EditableWeightMetric.WEIGHT).input.setError(null);
            if (the_weight == null)
            {
                the_weight = new Weight();
                the_weight.date = System.currentTimeMillis();
                for (MetricField field : metricFields.values()) field.input.getText().clear();
            }
            else {
                for (MetricField field : metricFields.values()) {
                    field.input.setText(field.definition.displayText(the_weight, the_user));
                }
            }
            binding.dateTV.setText(DateUtils.formatDateTime(getContext(), the_weight.date,
                    DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_SHOW_TIME));

            for (MetricField field : metricFields.values()) {
                field.input.setHint(field.definition.hint());
                int unitResource = field.definition.unitResource(the_user);
                field.units.setText(unitResource == 0 ? "" : getString(unitResource));
            }
        });
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (view != null) {
                if (getActivity() != null) {
                    state.selectUser((User) adapterView.getItemAtPosition(i));
                    the_user = (User)adapterView.getItemAtPosition(i);
                    updateUi();
                }
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_edit_weight_menu, menu);
        if (getActivity() != null) {
            ((MainActivity) getActivity()).addUsersSpinner(menu, oisListener);
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_editweight_cancel) {
            if (getActivity() != null)
                ((MainActivity) getActivity()).closeEditWeightFragment(null, null, edit, false);
            return true;
        } else if (itemId == R.id.action_editweight_done) {
            if (checkValues() && (getActivity() != null)) {
                ((MainActivity) getActivity()).closeEditWeightFragment(the_weight, the_user, edit,
                        !the_weight.equals(old_weight));
            }
            return true;
        }
        return false;
    }

    private boolean checkValues()
    {
        MetricField requiredWeight = field(EditableWeightMetric.WEIGHT);
        LocalizedNumberParser.Result parsedWeight =
                LocalizedNumberParser.parse(requiredWeight.input.getText().toString());
        if (!parsedWeight.isValid())
        {
            requiredWeight.input.setError(getString(R.string.weight_edit_empty_value_error));
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

        for (MetricField field : metricFields.values()) {
            String input = field.input.getText().toString().trim();
            LocalizedNumberParser.Result parsed = LocalizedNumberParser.parse(input);
            double displayed = input.isEmpty() ? -1
                    : parsed.isValid() ? parsed.value() : field.definition.invalidFallback();
            double canonical = field.definition.toCanonicalValue(
                    displayed, the_weight.weight, the_user);
            field.definition.set(the_weight, canonical);
        }

        return true;
    }
}
