package com.quantrity.antscaledisplay;

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
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.Spinner;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.quantrity.antscaledisplay.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements AppHost {
    private static final String TAG = "MainActivity";
    private View navigationView;
    private AppStateViewModel state;
    private UserSpinnerController userSpinnerController;
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
        for (NavigationDestination destination : NavigationDestination.values()) {
            navigationView.findViewById(destination.viewId).setOnClickListener(
                    view -> navigate(destination));
        }
        state = new ViewModelProvider(this).get(AppStateViewModel.class);
        userSpinnerController = new UserSpinnerController(this, state);

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

        if (!isPackageInstalled("com.dsi.ant.service.socket")) {
            goToMarketANTRadioService();
        } else {
            AntSupport.Capability antSupport = AntSupport.detect(this);
            if (antSupport != AntSupport.Capability.BUILT_IN) {
                if (antSupport == AntSupport.Capability.NONE) {
                    goToMarketANTUSBService();
                }

                SharedPreferences settings = getSharedPreferences(
                        getPackageName() + "_preferences", Context.MODE_PRIVATE);
                boolean neverNoAntMessage = settings.getBoolean("never_no_ant_msg", false);
                if (!neverNoAntMessage) {
                    boolean showMessage = true;
                    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    if (manager != null) {
                        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                        if (deviceList != null && !deviceList.isEmpty()) showMessage = false;
                    }
                    if (showMessage) showNoAntMessage();
                }
            }
        }

        if (!state.isLoaded()) new Thread(this::loadDB).start();

        if (savedInstanceState == null) {
            navigate(NavigationDestination.WEIGHT);
        } else {
            Fragment restored = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            NavigationDestination selected = NavigationDestination.forFragment(restored);
            if (selected != null) selectNavigationDestination(selected);
        }
    }

    public void reloadDB() {
        loadDB();
    }

    public void openEditUserFragment(User user) {
        EditUserFragment euf = EditUserFragment.newInstance(user == null ? null : user.uuid);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, euf).commit();

        // update selected item and title, then close the drawer
        selectNavigationDestination(NavigationDestination.USERS);
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
        navigate(NavigationDestination.USERS);
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
            selectNavigationDestination(NavigationDestination.WEIGHT);
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
            navigate(NavigationDestination.HISTORY);
        else
            navigate(NavigationDestination.WEIGHT);
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
        selectNavigationDestination(NavigationDestination.GOALS);
        setTitle(getString(R.string.lateral_menu_option_goals));
    }

    public void closeEditGoalFragment(Goal goal) {
        if (goal != null) state.saveGoal(goal, this::handleMutationFailure);
        dismissKeyboard();
        navigate(NavigationDestination.GOALS);
    }

    public boolean handleMutationFailure(RepositoryResult<Void> result) {
        if (result.isSuccess()) return false;
        Log.e(TAG, result.message, result.error);
        showMessage(getString(R.string.repository_save_error, result.message));
        return true;
    }

    public Spinner addUsersSpinner(Menu menu, AdapterView.OnItemSelectedListener oisListener) {
        return userSpinnerController.attach(menu, oisListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);
    }

    private boolean navigate(NavigationDestination destination) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (!destination.matches(current)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, destination.createFragment()).commit();
            setTitle(destination.titleResource);
        }
        selectNavigationDestination(destination);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void selectNavigationDestination(NavigationDestination selected) {
        for (NavigationDestination destination : NavigationDestination.values()) {
            CheckedTextView item = navigationView.findViewById(destination.viewId);
            item.setChecked(destination == selected);
        }
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

    public void showMessage(String msg) {
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

        AntWeightController rw = state.antWeightController();
        if (rw != null) {
            if (Debug.ON) Log.v(TAG, "onPause unregisterForAntIntents");
            rw.unregisterReceivers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AntWeightController rw = state.antWeightController();
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
