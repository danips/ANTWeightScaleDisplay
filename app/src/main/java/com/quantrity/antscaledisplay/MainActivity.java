package com.quantrity.antscaledisplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.dsi.ant.AntSupportChecker;
import com.google.android.material.navigation.NavigationView;
import com.quantrity.antscaledisplay.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final int NAV_POS_WEIGHT = 0;
    private static final int NAV_POS_GOALS = 1;
    private static final int NAV_POS_GRAPHS = 2;
    private static final int NAV_POS_HISTORY = 3;
    private static final int NAV_POS_USERS = 4;

    private NavigationView navigationView;
    private AppStateViewModel state;
    private ActivityMainBinding binding;

    private void loadDB() {
        RepositoryResult<Void> result = state.reload();
        if (!result.isSuccess()) Log.e(TAG, result.message, result.error);
        ArrayList<User> users = state.users();
        GarminTokenRefreshScheduler.scheduleAll(getApplicationContext(), users);

        //First time open users tab and request data
        if (users.isEmpty()) {
            runOnUiThread(() -> openEditUserFragment(null));
        }

        /* Get latest measurement for selected user */
        // Notify the WeightFragment to refresh its data now that DB is loaded
        runOnUiThread(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (currentFragment instanceof WeightFragment) {
                ((WeightFragment) currentFragment).updateUi();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.appBarMain.toolbar;
        setSupportActionBar(toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.lateral_menu_open, R.string.lateral_menu_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = binding.navView;
        navigationView.setNavigationItemSelectedListener(this);
        state = new ViewModelProvider(this).get(AppStateViewModel.class);

        // Handle Back Press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    Fragment current_fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
                    if (current_fragment instanceof EditWeightFragment) {
                        closeEditWeightFragment(null, null, ((EditWeightFragment) current_fragment).edit, false);
                    } else if (current_fragment instanceof EditUserFragment) {
                        closeEditUserFragment(null);
                    } else {
                        // Disable this callback and call default back behavior (finish activity)
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });

        if (!isPackageInstalled("com.dsi.ant.service.socket")) goToMarketANTRadioService();
        else if (!AntSupportChecker.hasAntFeature(this)) {
            if (!AntSupportChecker.hasAntAddOn(this)) {
                goToMarketANTUSBService();
            }

            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            boolean never_no_ant_msg = settings.getBoolean("never_no_ant_msg", false);
            if (!never_no_ant_msg) {
                boolean show_message = true;
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                if (manager != null) {
                    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                    if (!((deviceList == null) || (deviceList.isEmpty()))) show_message = false;
                }
                if (show_message) showNoAntMessage();
            }
        }

        if (!state.isLoaded()) new Thread(this::loadDB).start();

        if (savedInstanceState == null) {
            selectItem(getString(R.string.lateral_menu_option_weight));
        }
    }

    public void reloadDB() {
        loadDB();
    }

    public void openEditUserFragment(User user) {
        EditUserFragment euf = EditUserFragment.newInstance(user == null ? null : user.uuid);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, euf).commit();

        // update selected item and title, then close the drawer
        navigationView.getMenu().getItem(NAV_POS_USERS).setChecked(true);
        setTitle(getString(R.string.lateral_menu_option_users));
    }

    public void closeEditUserFragment(User user) {
        if (user != null) {
            state.saveUser(user, result -> {
                if (handleMutationFailure(result)) return;
                GarminTokenRefreshScheduler.schedule(this, user);
            });
        }
        dismissKeyboard();
        selectItem(getString(R.string.lateral_menu_option_users));
    }

    public void openEditWeightFragment(Weight weight, User user, boolean edit) {
        if (state.users().isEmpty())
        {
            openEditUserFragment(null);
        }
        else {
            EditWeightFragment ewf = EditWeightFragment.newInstance(
                    weight == null ? null : weight.uuid,
                    weight == null ? -1 : weight.date,
                    user == null ? null : user.uuid,
                    edit);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, ewf, "EditWeightFragmentTag").commit();

            // update selected item and title, then close the drawer
            navigationView.getMenu().getItem(NAV_POS_WEIGHT).setChecked(true);
            setTitle(getString(R.string.weight_edit_fragment_edit_weight));
        }
    }

    public void closeEditWeightFragment(Weight weight, User user, boolean edit, boolean change) {
        if (weight != null) {
            state.saveWeight(weight, edit, result -> {
                if (handleMutationFailure(result)) return;
                if (user != null && user.autoupload && change) uploadButton(this, weight, user);
            });
        }

        dismissKeyboard();

        if (edit)
            selectItem(getString(R.string.lateral_menu_option_history));
        else
            selectItem(getString(R.string.lateral_menu_option_weight));
    }

    public void openEditGoalFragment(Goal goal) {
        User selectedUser = state.selectedUser();
        if (selectedUser == null) {
            showMessage(R.string.edit_user_fragment_msg_user_missing);
            return;
        }

        EditGoalFragment egf = EditGoalFragment.newInstance(
                goal == null ? null : goal.uuid,
                goal == null ? -1 : goal.start_date,
                goal == null ? null : goal.type.toString(),
                selectedUser.uuid);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, egf).commit();

        // update selected item and title, then close the drawer
        navigationView.getMenu().getItem(NAV_POS_GOALS).setChecked(true);
        setTitle(getString(R.string.lateral_menu_option_goals));
    }

    public void closeEditGoalFragment(Goal goal) {
        if (goal != null) state.saveGoal(goal, this::handleMutationFailure);
        dismissKeyboard();
        selectItem(getString(R.string.lateral_menu_option_goals));
    }

    public AntWeightController getAntWeightController() { return state.antWeightController(); }

    boolean handleMutationFailure(RepositoryResult<Void> result) {
        if (result.isSuccess()) return false;
        Log.e(TAG, result.message, result.error);
        showMessage(getString(R.string.repository_save_error, result.message));
        return true;
    }

    public AntWeightController startAntWeightMeasurement(WeightFragment fragment) {
        return state.newAntWeightController(fragment);
    }

    @SuppressLint("ClickableViewAccessibility")
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
        final ArrayList<User> mUsersArray = state.users();
        ArrayList<String> items=new ArrayList<>();
        for (User user : mUsersArray) {
            items.add(user.toString());
        }
        final SpinnerDialog spinnerDialog;
        spinnerDialog = new SpinnerDialog(MainActivity.this, items, getString(R.string.edit_user_fragment_user));
        spinnerDialog.setCancellable(true);
        spinnerDialog.setShowKeyboard(true);


        spinnerDialog.bindOnSpinnerListener((item, position) -> {
            state.selectUser(mUsersArray.get(position));
            v.setSelection(position);
            spinnerDialog.closeSpinnerDialog();
        });
        spinnerDialog.showSpinnerDialog();
    }

    public Spinner addUsersSpinner(Menu menu, AdapterView.OnItemSelectedListener oisListener) {
        MenuItem mSpinnerItem = menu.findItem(R.id.action_select_user);

        ArrayList<User> mUsersArray = state.users();
        if (!mUsersArray.isEmpty()) {
            Spinner spinner = (Spinner)mSpinnerItem.getActionView();
            // Check for null to avoid NPE on setAdapter
            if (spinner != null) {
                final ArrayAdapter<User> adapter = new ArrayAdapter<>(this, R.layout.fragment_weight_user_spinner_item, mUsersArray);
                adapter.setDropDownViewResource(R.layout.fragment_weight_user_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                if (mUsersArray.size() > 1) {
                    spinner.setSelection(mUsersArray.indexOf(state.selectedUser()), false);
                    if (mUsersArray.size() > 10) {
                        spinner.setOnTouchListener(spinnerOnTouch);
                        spinner.setOnKeyListener(spinnerOnKey);
                    }
                } else mSpinnerItem.setVisible(false);
                spinner.setOnItemSelectedListener(oisListener);
            }
            return spinner;
        } else {
            if (Debug.ON) Log.v(TAG, "mUsersArray.size() = 0; " + mUsersArray.size());
            mSpinnerItem.setVisible(false);
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getTitle() != null) {
            return selectItem(item.getTitle().toString());
        }
        return false;
    }

    private boolean selectItem(String item) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        Fragment current_fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        int pos = NAV_POS_WEIGHT;

        if (item.equals(getString(R.string.lateral_menu_option_weight))) {
            if (!(current_fragment instanceof WeightFragment))
            {
                //pos = NAV_POS_WEIGHT;
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
        } else {
            return false;
        }
        // update selected item and title, then close the drawer
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
            setTitle(item);
            navigationView.getMenu().getItem(pos).setChecked(true);
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);

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

    private void dismissKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) return;
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    void showNoAntMessage() {
        if (!this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.msg_problem_usb_stick_not_detected)
                    .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.dismiss())
                    .setNeutralButton(R.string.msg_problem_usb_stick_not_detected_never, (dialog, id) -> {
                        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
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

        AntWeightController rw = getAntWeightController();
        if (rw != null) {
            if (Debug.ON) Log.v(TAG, "onPause unregisterForAntIntents");
            rw.unregisterReceivers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AntWeightController rw = getAntWeightController();
        if (rw != null) {
            if (Debug.ON) Log.v(TAG, "onResume registerForAntIntents " + rw.state());
            rw.registerReceivers();
        }
    }

    public static boolean isOnline(MainActivity activity) {
        if (activity == null) return false;
        ConnectivityManager cm = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static void uploadButton(MainActivity activity, Weight weight, User user) {
        if (!isOnline(activity)) {
            activity.showMessage(R.string.weight_fragment_msg_problem_no_internet_connection);
            return;
        }

        ForegroundUpload upload = new ForegroundUpload(activity, weight, user, true, true);
        upload.execute();
    }

}
