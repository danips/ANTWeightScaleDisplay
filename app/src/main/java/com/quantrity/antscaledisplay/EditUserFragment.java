package com.quantrity.antscaledisplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EditUserFragment extends Fragment implements MenuProvider {
    private final static String TAG = "EditUserFragment";
    private static final String ARG_USER_UUID = "user_uuid";
    private static final String STATE_BIRTHDATE = "birthdate";

    private LinearLayout cm_ll;
    private LinearLayout ft_ll;

    //Data fields
    private User the_user;
    private AppStateViewModel state;
    private boolean needs_to_sync;
    private EditText et_name;
    private RadioGroup rg_gender;
    private EditText et_birthdate;
    private long birthdate_millis = -1;
    private Spinner sp_units;
    private EditText et_height_cm;
    private EditText et_height_ft;
    private EditText et_height_in;
    private Spinner sp_activity;
    private CheckBox cb_lifetime_athlete;
    private EditText et_gc_user;
    private EditText et_gc_pass;
    private EditText et_email_to;
    private CheckBox cb_auto_upload;
    private CheckBox cb_show_fat_mass;
    private TextView tv_garmin_token_information;
    private List<WorkInfo> garmin_refresh_work = Collections.emptyList();

    static EditUserFragment newInstance(String userUuid) {
        EditUserFragment fragment = new EditUserFragment();
        Bundle arguments = new Bundle();
        if (userUuid != null) arguments.putString(ARG_USER_UUID, userUuid);
        fragment.setArguments(arguments);
        return fragment;
    }

    // Replacement for startActivityForResult
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    boolean ok = false;
                    if (getActivity() != null && uri != null) {
                        ok = UsersFragment.unzip(uri, getActivity().getFilesDir().toString(), getActivity().getContentResolver());
                    }

                    if (ok && getActivity() != null) {
                        Toast.makeText(getActivity(), getString(R.string.history_fragment_action_database_restore_ok), Toast.LENGTH_LONG).show();
                        ((MainActivity) getActivity()).reloadDB();
                        getActivity().invalidateOptionsMenu();
                        ((MainActivity) getActivity()).closeEditUserFragment(null);
                    }
                }
            }
    );

    private static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if ((activity.getCurrentFocus() != null) && (inputMethodManager != null)) inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @SuppressLint("ClickableViewAccessibility")
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

    void showMessage(int id) {
        showMessage(getString(id));
    }

    void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(msg)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.cancel()).create().show();
    }

    private User checkValues() {
        User tmp = new User();

        if (et_name.getText().length() == 0) {
            showMessage(R.string.edit_user_fragment_msg_name_missing);
            return null;
        }

        if ((the_user == null) && (getActivity() != null)) {
            ArrayList<User> users = state.users();
            for (User user : users) {
                if (user.name.equals(et_name.getText().toString().trim().replaceAll("[\n\r]", ""))) {
                    showMessage(R.string.edit_user_fragment_msg_name_dup);
                    return null;
                }
            }
        }

        tmp.name = et_name.getText().toString().trim().replaceAll("[\n\r]", "");
        tmp.isMale = (rg_gender.getCheckedRadioButtonId() == R.id.rb_male);
        if (birthdate_millis == -1) {
            showMessage(R.string.edit_user_fragment_msg_birthdate_missing);
            return null;
        }
        tmp.birthdate = birthdate_millis;
        tmp.age = User.calcAgeNow(birthdate_millis);
        if ((tmp.age < 1) || (tmp.age > 127)) {
            showMessage(R.string.edit_user_fragment_msg_age_invalid);
            return null;
        }
        switch (sp_units.getSelectedItemPosition()) {
            case 0:
                tmp.mass_unit = User.MassUnit.KG;
                tmp.usesCm = true;
                break;
            case 1:
                tmp.mass_unit = User.MassUnit.LB;
                tmp.usesCm = true;
                break;
            case 2:
                tmp.mass_unit = User.MassUnit.ST;
                tmp.usesCm = true;
                break;
            case 3:
                tmp.mass_unit = User.MassUnit.KG;
                tmp.usesCm = false;
                break;
            case 4:
                tmp.mass_unit = User.MassUnit.LB;
                tmp.usesCm = false;
                break;
            case 5:
                tmp.mass_unit = User.MassUnit.ST;
                tmp.usesCm = false;
                break;
        }

        if (tmp.usesCm) {
            if (et_height_cm.getText().length() == 0) {
                showMessage(R.string.edit_user_fragment_msg_height_missing);
                return null;
            }
            tmp.height_cm = Integer.parseInt(et_height_cm.getText().toString().trim());
        } else {
            if ((et_height_ft.getText().length() == 0) || (et_height_in.getText().length() == 0)) {

                showMessage(R.string.edit_user_fragment_msg_height_missing);
                return null;
            }
            tmp.height_ft = Integer.parseInt(et_height_ft.getText().toString().trim());
            tmp.height_in = Integer.parseInt(et_height_in.getText().toString().trim());
            tmp.height_cm = (int)(tmp.height_ft * 30.48 + tmp.height_in * 2.54);
        }
        if ((tmp.height_cm > 255) || ((tmp.height_ft >= 8) && (tmp.height_in >= 4))) {
            showMessage(R.string.edit_user_fragment_msg_height_invalid);
            return null;
        }
        tmp.activity_level = sp_activity.getSelectedItemPosition();
        tmp.isLifetimeAthlete = cb_lifetime_athlete.isChecked();

        tmp.gc_user = (et_gc_user.getText().length() == 0) ? null : et_gc_user.getText().toString().trim().replaceAll("[\n\r]", "");
        tmp.gc_pass = (et_gc_pass.getText().length() == 0) ? null : et_gc_pass.getText().toString().trim().replaceAll("[\n\r]", "");
        tmp.email_to = (et_email_to.getText().length() == 0) ? null : et_email_to.getText().toString().trim().replaceAll("[\n\r]", "");

        tmp.autoupload = cb_auto_upload.isChecked();
        tmp.show_fat_mass = cb_show_fat_mass.isChecked();

        if (the_user == null) {
            tmp.uuid = UUID.randomUUID().toString();
            return tmp;
        }

        the_user.name = tmp.name;
        the_user.isMale = tmp.isMale;
        the_user.birthdate = tmp.birthdate;
        the_user.age = tmp.age;
        the_user.usesCm = tmp.usesCm;
        the_user.mass_unit = tmp.mass_unit;
        the_user.height_cm = tmp.height_cm;
        the_user.height_ft = tmp.height_ft;
        the_user.height_in = tmp.height_in;
        the_user.activity_level = tmp.activity_level;
        the_user.isLifetimeAthlete = tmp.isLifetimeAthlete;
        the_user.gc_user = tmp.gc_user;
        the_user.gc_pass = tmp.gc_pass;
        the_user.email_to = tmp.email_to;
        the_user.autoupload = tmp.autoupload;
        the_user.show_fat_mass = tmp.show_fat_mass;
        return the_user;
    }


    public void setValues(User user) {
        if (user == null) {
            resetValues();
            return;
        }

        et_name.setText(user.name);
        rg_gender.check((user.isMale) ? R.id.rb_male : R.id.rb_female );

        et_birthdate.setText(DateUtils.formatDateTime(getActivity(), user.birthdate, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
        birthdate_millis = user.birthdate;
        if (user.mass_unit == User.MassUnit.LB)
        {
            sp_units.setSelection((user.usesCm) ? 1 : 4);
        }
        else if (user.mass_unit == User.MassUnit.ST)
        {
            sp_units.setSelection((user.usesCm) ? 2 : 5);
        }
        else
        {
            sp_units.setSelection((user.usesCm) ? 0 : 3);
        }
        if (user.usesCm) {
            et_height_cm.setText(String.format(Locale.getDefault(),"%d",user.height_cm));
        } else {
            et_height_ft.setText(String.format(Locale.getDefault(),"%d",user.height_ft));
            et_height_in.setText(String.format(Locale.getDefault(),"%d",user.height_in));
        }
        sp_activity.setSelection(user.activity_level);
        cb_lifetime_athlete.setChecked(user.isLifetimeAthlete);
        et_gc_user.setText(user.gc_user);
        et_gc_pass.setText(user.gc_pass);

        et_email_to.setText(user.email_to);

        cb_auto_upload.setChecked(user.autoupload);
        cb_show_fat_mass.setChecked(user.show_fat_mass);
    }

    private void resetValues() {
        et_name.setText("");
        //rg_gender.set;
        et_birthdate.setText("");
        birthdate_millis = -1;
        //sp_units.set;
        et_height_cm.setText("");
        et_height_ft.setText("");
        et_height_in.setText("");
        sp_activity.setSelection(0);
        cb_lifetime_athlete.setChecked(false);
        et_gc_user.setText("");
        et_gc_pass.setText("");
        et_email_to.setText("");
        cb_auto_upload.setChecked(true);
        cb_show_fat_mass.setChecked(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        if (!state.isLoaded()) state.reload();
        String userUuid = getArguments() == null ? null : getArguments().getString(ARG_USER_UUID);
        the_user = userUuid == null ? null : state.findUser(userUuid);
        if (savedInstanceState != null) {
            birthdate_millis = savedInstanceState.getLong(STATE_BIRTHDATE, -1);
        }
        View rootView = inflater.inflate(R.layout.fragment_edit_user, container, false);

        //Close keyboard when clicking any other item on screen
        setupUI(rootView);

        cm_ll = rootView.findViewById(R.id.ll_height_metric);
        ft_ll = rootView.findViewById(R.id.ll_height_imperial);
        et_name =  rootView.findViewById(R.id.et_name);
        rg_gender = rootView.findViewById(R.id.rg_gender);
        et_birthdate =  rootView.findViewById(R.id.et_birthdate);
        et_height_cm =  rootView.findViewById(R.id.et_height_cm);
        et_height_ft =  rootView.findViewById(R.id.et_height_ft);
        et_height_in =  rootView.findViewById(R.id.et_height_in);
        sp_activity =  rootView.findViewById(R.id.sp_activity);
        cb_lifetime_athlete =  rootView.findViewById(R.id.cb_lifetime_athlete);
        et_gc_user =  rootView.findViewById(R.id.et_gc_user);
        et_gc_pass =  rootView.findViewById(R.id.et_gc_pass);
        sp_units =  rootView.findViewById(R.id.sp_units);
        sp_units.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 3 || position == 4 || position == 5) {//ft, in
                    cm_ll.setVisibility(View.GONE);
                    ft_ll.setVisibility(View.VISIBLE);
                } else {//cm
                    cm_ll.setVisibility(View.VISIBLE);
                    ft_ll.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        et_email_to =  rootView.findViewById(R.id.et_email_to);

        cb_auto_upload =  rootView.findViewById(R.id.cb_automatic_upload);
        cb_show_fat_mass =  rootView.findViewById(R.id.cb_fat_mass);
        tv_garmin_token_information = rootView.findViewById(R.id.garmin_token_information);
        Button b_gc_token_clear = rootView.findViewById(R.id.garmin_token_clear);
        b_gc_token_clear.setOnClickListener(v -> clearGarminTokens());

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        this.needs_to_sync = savedInstanceState == null;

        et_birthdate.setOnFocusChangeListener((view, b) -> {
            if ((b) && (getActivity() != null)) {
                Calendar c = Calendar.getInstance();
                if (birthdate_millis != -1) c.setTimeInMillis(birthdate_millis);
                else c.add(Calendar.YEAR, -18);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), (datePicker, i, i2, i3) -> {
                    Calendar c1 = Calendar.getInstance();
                    c1.set(i, i2, i3, 0, 0, 0);
                    c1.set(Calendar.MILLISECOND, 0);
                    birthdate_millis = c1.getTimeInMillis();
                    et_birthdate.setText(DateUtils.formatDateTime(getActivity(), birthdate_millis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR));
                    Log.v(TAG, "BIRTH=" + birthdate_millis);
                }, year, month, day);
                dpd.setTitle(R.string.edit_user_fragment_birthdate);
                dpd.setOnDismissListener(dialogInterface -> {
                    if ((et_birthdate != null) && (et_birthdate.focusSearch(View.FOCUS_DOWN) != null)) {
                        et_birthdate.focusSearch(View.FOCUS_DOWN).requestFocus();
                    }
                });
                dpd.show();
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (the_user == null || the_user.uuid == null) {
            updateGarminTokenInformation();
            return;
        }

        WorkManager.getInstance(requireContext())
                .getWorkInfosForUniqueWorkLiveData(
                        GarminTokenRefreshScheduler.uniqueWorkName(the_user.uuid))
                .observe(getViewLifecycleOwner(), workInfos -> {
                    garmin_refresh_work = workInfos == null
                            ? Collections.emptyList()
                            : workInfos;
                    User.reloadGarminTokens(requireContext(), the_user);
                    updateGarminTokenInformation();
                });
    }


    @Override
    public void onResume() {
        if (needs_to_sync) {
            setValues(the_user);
            needs_to_sync = false;
        }
        if (the_user != null) User.reloadGarminTokens(requireContext(), the_user);
        updateGarminTokenInformation();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_BIRTHDATE, birthdate_millis);
    }

    private void updateGarminTokenInformation() {
        if (tv_garmin_token_information == null) return;

        GarminTokenStatus.State state = GarminTokenStatus.getState(
                the_user, System.currentTimeMillis() / 1000);
        String accessExpiry = formatTokenExpiry(
                the_user == null ? -1 : the_user.garminOauth2ExpiryTimestamp);
        String connectionExpiry = formatTokenExpiry(
                the_user == null ? -1 : the_user.garminOauth1MfaExpirationTimestamp);
        tv_garmin_token_information.setText(getString(
                R.string.garmin_token_information,
                getTokenStateText(state),
                accessExpiry,
                connectionExpiry,
                getBackgroundRefreshText()));
    }

    private String getTokenStateText(GarminTokenStatus.State state) {
        switch (state) {
            case ACTIVE:
                return getString(R.string.garmin_token_status_active);
            case ACCESS_EXPIRED:
                return getString(R.string.garmin_token_status_access_expired);
            case CONNECTION_EXPIRED:
                return getString(R.string.garmin_token_status_connection_expired);
            case UNKNOWN:
                return getString(R.string.garmin_token_status_unknown);
            case NOT_AUTHENTICATED:
            default:
                return getString(R.string.garmin_token_status_not_authenticated);
        }
    }

    private String formatTokenExpiry(long expirySeconds) {
        if (expirySeconds <= 0) return getString(R.string.garmin_token_status_unknown);
        return DateUtils.formatDateTime(
                requireContext(),
                expirySeconds * 1000,
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_ABBREV_MONTH);
    }

    private String getBackgroundRefreshText() {
        if (the_user != null
                && the_user.garminOauth2Token != null
                && !the_user.garminOauth2Token.isEmpty()
                && !GarminTokenRefreshScheduler.hasRenewalCredentials(the_user)) {
            return getString(R.string.garmin_token_work_reconnect_required);
        }

        WorkInfo enqueuedWork = null;
        boolean failed = false;
        for (WorkInfo workInfo : garmin_refresh_work) {
            if (workInfo.getState() == WorkInfo.State.RUNNING) {
                return getString(R.string.garmin_token_work_running);
            }
            if (workInfo.getState() == WorkInfo.State.ENQUEUED) enqueuedWork = workInfo;
            if (workInfo.getState() == WorkInfo.State.BLOCKED && enqueuedWork == null) {
                enqueuedWork = workInfo;
            }
            if (workInfo.getState() == WorkInfo.State.FAILED) failed = true;
        }

        if (enqueuedWork != null) {
            long nextRun = enqueuedWork.getNextScheduleTimeMillis();
            if (nextRun > 0 && nextRun < Long.MAX_VALUE) {
                String formattedTime = DateUtils.formatDateTime(
                        requireContext(),
                        nextRun,
                        DateUtils.FORMAT_SHOW_DATE
                                | DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_SHOW_YEAR
                                | DateUtils.FORMAT_ABBREV_MONTH);
                return getString(R.string.garmin_token_work_scheduled, formattedTime);
            }
            return getString(R.string.garmin_token_work_queued);
        }
        if (failed) return getString(R.string.garmin_token_work_failed);
        return getString(R.string.garmin_token_work_not_scheduled);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_edit_user_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_edituser_cancel) {
            if (getActivity() != null)
                ((MainActivity) getActivity()).closeEditUserFragment(null);
            return true;
        } else if (itemId == R.id.action_edituser_done) {
            User user;
            if (((user = checkValues()) != null) && (getActivity() != null)) {
                ((MainActivity) getActivity()).closeEditUserFragment(user);
            }
            return true;
        } else if (itemId == R.id.action_database_restore) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
            return true;
        }
        return false;
    }

    /* Clear Garmin OAuth Tokens */
    private void clearGarminTokens() {
        if (getActivity() != null) {
            if (the_user != null && state.findUser(the_user.uuid) != null) {
                the_user.garminOauth1Token = "";
                the_user.garminOauth1TokenSecret = "";
                the_user.garminOauth1MfaToken = "";
                the_user.garminOauth1MfaExpirationTimestamp = -1;
                the_user.garminOauth2Token = "";
                the_user.garminOauth2ExpiryTimestamp = -1;
                GarminTokenRefreshScheduler.cancel(getActivity(), the_user);
                state.saveUser(the_user);
                updateGarminTokenInformation();
                Toast.makeText(getActivity(), R.string.gc_token_cleared, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
