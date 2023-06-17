package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.dsi.ant.AntSupportChecker;
import com.google.android.material.navigation.NavigationView;

import java.text.Collator;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CoreInterface {
    private static final String TAG = "MainActivity";
    private static final int NAV_POS_WEIGHT = 0;
    private static final int NAV_POS_GOALS = 1;
    private static final int NAV_POS_GRAPHS = 2;
    private static final int NAV_POS_HISTORY = 3;
    private static final int NAV_POS_USERS = 4;

    public static final int DIRECTORY_PICKER_RESULT = 102;
    public static final int FILE_PICKER_RESULT = 103;
    public static final int CSV_DIRECTORY_PICKER_RESULT = 109;
    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 110;

    private NavigationView navigationView;
    EditWeightFragment ewf;


    private ArrayList<User> mUsersArray;
    private ArrayList<Weight> mHistoryArray;
    private ArrayList<Goal> mGoalsArray;

    private RequestWeight rw = null;

    private User selectedUser = null;

    private void loadDB() {
        //Read existing users
        User.deserializeUsers(getApplicationContext(), mUsersArray);

        //First time open users tab and request data
        if (mUsersArray.size() == 0) {
            selectedUser = null;
            runOnUiThread(() -> openEditUserFragment(null));
        } else {
            if (mUsersArray.size() == 1) {
                selectedUser = mUsersArray.get(0);
            }
            else {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String selected_user = settings.getString("selected_user", null);
                final Collator collator = Collator.getInstance();
                Collections.sort(mUsersArray, (o1, o2) -> collator.compare(o1.name, o2.name));
                if ((selected_user == null) || selected_user.equals("")) {
                    selectedUser = mUsersArray.get(0);
                }
                else {
                    Iterator<User> it = mUsersArray.iterator();
                    User user = null;
                    while (it.hasNext()) {
                        User tmp = it.next();
                        if (tmp.name.equals(selected_user)) {
                            user = tmp;
                            break;
                        }
                    }
                    if (user == null) {
                        selectedUser = mUsersArray.get(0);
                    }
                    else {
                        selectedUser = user;
                    }
                }
            }
        }
        Weight.deserializeHistory(getApplicationContext(), mHistoryArray);
        /* TODO: get latest measurement for selected user */

        Goal.deserializeGoals(getApplicationContext(), mGoalsArray);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.lateral_menu_open, R.string.lateral_menu_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (!isPackageInstalled("com.dsi.ant.service.socket")) goToMarketANTRadioService();
        else if (!AntSupportChecker.hasAntFeature(this)) {
            if (!AntSupportChecker.hasAntAddOn(this)) {
                goToMarketANTUSBService();
            }

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean never_no_ant_msg = settings.getBoolean("never_no_ant_msg", false);
            if (!never_no_ant_msg) {
                boolean show_message = true;
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                if (manager != null) {
                    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                    if (!((deviceList == null) || (deviceList.size() == 0))) show_message = false;
                }
                if (show_message) showNoAntMessage();
            }
        }

        //Everything is OK, start loading data asynchronously
        //Deserialize Users
        mUsersArray = new ArrayList<>();
        mHistoryArray = new ArrayList<>();
        mGoalsArray = new ArrayList<>();
        new Thread(this::loadDB).start();

        if (savedInstanceState == null) {
            selectItem(getString(R.string.lateral_menu_option_weight));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///// <CoreInterface
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void reloadDB() {
        mUsersArray.clear();
        mHistoryArray.clear();
        mGoalsArray.clear();
        loadDB();
    }

    @Override
    public ArrayList<User> getUsersArray() { return mUsersArray; }

    @Override
    public ArrayList<Weight> getHistoryArray() { return mHistoryArray; }

    @Override
    public ArrayList<Goal> getGoalsArray() { return mGoalsArray; }

    @Override
    public ArrayList<Weight> getHistoryArraySelectedUser(){
        if (selectedUser == null)
        {
            return new ArrayList<>();
        }
        ArrayList<Weight> wal = (ArrayList<Weight>) mHistoryArray.clone();

        Iterator<Weight> itr = wal.iterator();
        while (itr.hasNext())
        {
            Weight w = (Weight) itr.next();
            if (!w.uuid.equals(selectedUser.uuid))
            {
                itr.remove();
            }
        }
        return wal;
    }

    @Override
    public Weight getLastHistorySelectedUser(){
        Weight w = null;

        if ((selectedUser != null) && (!mHistoryArray.isEmpty())){
            for (Weight weight : mHistoryArray)
            {
                if ((weight.uuid.equals(selectedUser.uuid))) {
                    w = mHistoryArray.get(0);
                    break;
                }
            }
        }
        return w;
    }

    @Override
    public ArrayList<Goal> getGoalsArraySelectedUser(){
        ArrayList<Goal> wal = new ArrayList<>();

        if (selectedUser != null) {
            for (Goal g : mGoalsArray) {
                if (g.uuid.equals(selectedUser.uuid)) wal.add(g);
            }
        }
        return wal;
    }

    @Override
    public void openEditUserFragment(User user) {
        EditUserFragment euf = new EditUserFragment();
        euf.the_user = user;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, euf).commit();

        // update selected item and title, then close the drawer
        navigationView.getMenu().getItem(NAV_POS_USERS).setChecked(true);
        setTitle(getString(R.string.lateral_menu_option_users));
    }

    @Override
    public void closeEditUserFragment(User user) {
        if (user != null) {
            if (Debug.ON) Log.v(TAG, "closeEditUserFragment " + mUsersArray.contains(user) + " " + mUsersArray.size());
            if (!mUsersArray.contains(user)) mUsersArray.add(0, user);
            User.serializeUsers(this, mUsersArray);
            selectedUser = user;
        }

        if (this.getCurrentFocus()!= null) {
            InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
        }

        selectItem(getString(R.string.lateral_menu_option_users));
    }

    @Override
    public void openEditWeightFragment(Weight weight, User user, boolean edit) {
        if (mUsersArray.isEmpty())
        {
            openEditUserFragment(null);
        }
        else {
            if (ewf == null) ewf = new EditWeightFragment();
            if (weight != null) {
                try {
                    ewf.old_weight = (Weight) weight.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    ewf.old_weight = null;
                }
            }
            else
            {
                ewf.old_weight = null;
            }
            ewf.the_weight = weight;
            ewf.the_user = user;
            ewf.edit = edit;
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, ewf, "EditWeightFragmentTag").commit();

            // update selected item and title, then close the drawer
            navigationView.getMenu().getItem(NAV_POS_WEIGHT).setChecked(true);
            setTitle(getString(R.string.weight_edit_fragment_edit_weight));
        }
    }

    @Override
    public void closeEditWeightFragment(MainActivity activity, Weight weight, User user, boolean edit, boolean change) {
        if (weight != null) {
            if (!edit) {
                mHistoryArray.add(0, weight);
            }
            Collections.sort(mHistoryArray, new Weight.DateComparator());
            Weight.serializeWeight(this, mHistoryArray);
            if ((user != null) && user.autoupload && change)
            {
                uploadButton(activity, weight, user);
            }
        }

        if (this.getCurrentFocus()!= null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
        }

        if (edit)
            selectItem(getString(R.string.lateral_menu_option_history));
        else
            selectItem(getString(R.string.lateral_menu_option_weight));
    }

    @Override
    public void openEditGoalFragment(Goal goal) {
        if (selectedUser == null) {
            showMessage(R.string.edit_user_fragment_msg_user_missing);
            return;
        }

        EditGoalFragment egf = new EditGoalFragment();
        egf.the_goal = goal;
        egf.the_user = selectedUser;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, egf).commit();

        // update selected item and title, then close the drawer
        navigationView.getMenu().getItem(NAV_POS_GOALS).setChecked(true);
        setTitle(getString(R.string.lateral_menu_option_goals));
    }

    @Override
    public void closeEditGoalFragment(Goal goal) {
        if (goal != null) {
            if (!mGoalsArray.contains(goal)) mGoalsArray.add(0, goal);
            /* TODO: Sort by end date */
            Goal.serializeGoals(this, mGoalsArray);
        }

        if (this.getCurrentFocus()!= null) {
            InputMethodManager inputMethodManager = (InputMethodManager)  getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
        }

        selectItem(getString(R.string.lateral_menu_option_goals));
    }

    @Override
    public RequestWeight getRequestWeight() { return rw; }

    @Override
    public RequestWeight newRequestWeight(WeightFragment fragment) {
        return (rw = new RequestWeight(fragment));
    }

    @Override
    public void saveWeight(Weight w) {
        mHistoryArray.add(0, w);
        Weight.serializeWeight(this, mHistoryArray);
    }

    public void deleteWeight(Weight weight) {
        boolean ret;
        ret = mHistoryArray.remove(weight);
        Weight.serializeWeight(this, mHistoryArray);
        if (ret && (Debug.ON)) Log.v(TAG, "Unable to remove weight from history");
    }


    @Override
    public void saveGoal(Goal goal) {
        mGoalsArray.add(0, goal);
        Goal.serializeGoals(this, mGoalsArray);
    }

    public void deleteGoal(Goal goal) {
        boolean ret;
        ret = mGoalsArray.remove(goal);
        Goal.serializeGoals(this, mGoalsArray);
        if (ret && (Debug.ON)) Log.v(TAG, "Unable to remove goal from history");
    }

    @Override
    public User getSelectedUser() {
        return selectedUser;
    }

    @Override
    public void setSelectedUser(User user) {
        selectedUser = user;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("selected_user", user.name);
        editor.apply();
    }

    private final View.OnTouchListener spinnerOnTouch = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            showSearch((Spinner)v);
            return true;
        }
        return false;
    };
    private final View.OnKeyListener spinnerOnKey = (v, keyCode, event) -> {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showSearch((Spinner)v);
            return true;
        }
        return false;
    };

    private void showSearch(final Spinner v)
    {
        final ArrayList<User> mUsersArray = getUsersArray();
        ArrayList<String> items=new ArrayList<>();
        for (User user : mUsersArray) {
            items.add(user.toString());
        }
        final SpinnerDialog spinnerDialog;
        spinnerDialog = new SpinnerDialog(MainActivity.this, items, getString(R.string.edit_user_fragment_user));
        spinnerDialog.setCancellable(true);
        spinnerDialog.setShowKeyboard(true);


        spinnerDialog.bindOnSpinerListener((item, position) -> {
            setSelectedUser(mUsersArray.get(position));
            v.setSelection(position);
            spinnerDialog.closeSpinerDialog();
        });
        spinnerDialog.showSpinerDialog();
    }

    public Spinner addUsersSpinner(Menu menu, AdapterView.OnItemSelectedListener oisListener) {
        MenuItem mSpinnerItem = menu.findItem(R.id.action_select_user);

        ArrayList<User> mUsersArray = getUsersArray();
        if (mUsersArray.size() > 0) {
            Spinner spinner = (Spinner)mSpinnerItem.getActionView();
            final ArrayAdapter<User> adapter = new ArrayAdapter<>(this, R.layout.fragment_weight_user_spinner_item, mUsersArray);
            adapter.setDropDownViewResource(R.layout.fragment_weight_user_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            if (mUsersArray.size() > 1) {
                spinner.setSelection(mUsersArray.indexOf(selectedUser), false);
                if (mUsersArray.size() > 10)
                {
                    spinner.setOnTouchListener(spinnerOnTouch);
                    spinner.setOnKeyListener(spinnerOnKey);
                }
            } else mSpinnerItem.setVisible(false);
            spinner.setOnItemSelectedListener(oisListener);

            return spinner;
        } else {
            if (Debug.ON) Log.v(TAG, "mUsersArray.size() = 0; " + mUsersArray.size());
            mSpinnerItem.setVisible(false);
            return null;
        }
    }

    @Override
    public void deleteHistoryAndUser(User user) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String selected_user = settings.getString("selected_user", null);
        if ((selected_user == null) || selected_user.equals(user.name) || (selected_user.equals(""))) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove("selected_user");
            editor.apply();
            selectedUser = null;
        }

        Iterator<Weight> it = mHistoryArray.iterator();
        while (it.hasNext()) if (it.next().uuid.equals(user.uuid)) it.remove();
        Weight.serializeWeight(this, mHistoryArray);

        Iterator<Goal> it2 = mGoalsArray.iterator();
        while (it2.hasNext()) if (it2.next().uuid.equals(user.uuid)) it2.remove();
        Goal.serializeGoals(this, mGoalsArray);

        mUsersArray.remove(user);
        User.serializeUsers(getApplicationContext(), mUsersArray);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///// >CoreInterface
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment current_fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (current_fragment instanceof EditWeightFragment)
            {
                closeEditWeightFragment((MainActivity)current_fragment.getActivity(),null, null, ((EditWeightFragment)current_fragment).edit, false);
            }
            else if (current_fragment instanceof EditUserFragment)
            {
                closeEditUserFragment(null);
            }
            else
            {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return selectItem(item.getTitle().toString());
    }

    private boolean selectItem(String item) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        Fragment current_fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        int pos = 0;

        if (item.equals(getString(R.string.lateral_menu_option_weight))) {
            if (!(current_fragment instanceof WeightFragment))
            {
                pos = NAV_POS_WEIGHT;
                fragment = new WeightFragment();
            }
        } else if (item.equals(getString(R.string.lateral_menu_option_users))) {
            if (!(current_fragment instanceof UsersFragment))
            {
                pos = NAV_POS_USERS;
                fragment = new UsersFragment();
            }
        } else if (item.equals(getString(R.string.lateral_menu_option_history))) {
            if (!(current_fragment instanceof HistoryFragment))
            {
                pos = NAV_POS_HISTORY;
                fragment = new HistoryFragment();
            }
        } else if (item.equals(getString(R.string.lateral_menu_option_graphs))) {
            if (!(current_fragment instanceof GraphsFragment)) {
                pos = NAV_POS_GRAPHS;
                fragment = new GraphsFragment();
            }
        } else if (item.equals(getString(R.string.lateral_menu_option_goals))) {
            if (!(current_fragment instanceof GoalsFragment)) {
                pos = NAV_POS_GOALS;
                fragment = new GoalsFragment();
            }
        }

        // update selected item and title, then close the drawer
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
            setTitle(item);
            navigationView.getMenu().getItem(pos).setChecked(true);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void setTitle(CharSequence title) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Debug.ON) Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void getDescription(View view) {
        int id = view.getId();
        if (id == R.id.weightIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_weight, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.trunkIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_trunk, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.leftArmIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_leftArm, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.rightArmIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_rightArm, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.leftLegIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_leftLeg, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.rightLegIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_rightLeg, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.percentFatIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_percentFat, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.percentHydrationIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_percentHydration, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.boneMassIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_boneMass, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.muscleMassIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_muscleMass, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.physiqueRatingIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_physiqueRating, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.visceralFatRatingIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_visceralFat, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.metabolicAgeIV) {
            Toast.makeText(getApplicationContext(), String.format("%s / %s", getString(R.string.weight_fragment_icon_desc_metabolicAge), getString(R.string.weight_fragment_icon_desc_activeMet)), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.basalMetIV) {
            Toast.makeText(getApplicationContext(), R.string.weight_fragment_icon_desc_basalMet, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isPackageInstalled(String packageName) {
        try{
            getApplicationContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_SERVICES);
            return true;
        } catch( PackageManager.NameNotFoundException e ){
            return false;
        }
    }

    public void goToMarketANTRadioService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.msg_problem_service_not_found), getResources().getString(R.string.ant_radio_service)))
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dsi.ant.service.socket")));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.dsi.ant.service.socket")));
                    }
                    dialog.cancel();
                    finish();
                }).setCancelable(false).create().show();
    }

    public void goToMarketANTUSBService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.msg_problem_service_not_found), getResources().getString(R.string.ant_usb_service)))
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dsi.ant.usbservice")));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.dsi.ant.usbservice")));
                    }
                    dialog.cancel();
                    finish();
                })
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(false).create().show();
    }

    void showMessage(int id) {
        if (!this.isFinishing()) {
            showMessage(getString(id));
        }
    }

    void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.dismiss()).create().show();
    }

    void showNoAntMessage() {
        if (!this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.msg_problem_usb_stick_not_detected)
                    .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.dismiss())
                    .setNeutralButton(R.string.msg_problem_usb_stick_not_detected_never, (dialog, id) -> {
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("never_no_ant_msg", true);
                        editor.apply();
                        dialog.dismiss();
                    })
                    .create().show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (rw != null) {
            if (Debug.ON) Log.v(TAG, "onPause unregisterForAntIntents");
            rw.unregisterForAntIntents();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rw != null) {
            if (Debug.ON) Log.v(TAG, "onResume registerForAntIntents " + rw.state);
            rw.registerForAntIntents();
        }
    }

    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     *
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     *
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     * @param context -
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static boolean isOnline(MainActivity activity) {
        if (activity == null) return false;
        ConnectivityManager cm = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public static void uploadButton(MainActivity activity, Weight weight, User user) {
        if (!isOnline(activity)) {
            activity.showMessage(R.string.weight_fragment_msg_problem_no_internet_connection);
            return;
        }

        AsyncUpload au = new AsyncUpload(activity, weight, user, true, true);
        au.execute(activity.getCacheDir() + "/weight.fit");
    }

    public static double parseNumber(EditText et) {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        Number number;
        try {
            number = format.parse(et.getText().toString().trim());
            if (number != null) return number.doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
