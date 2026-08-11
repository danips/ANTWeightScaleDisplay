package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity
        implements AppHost {
    private static final String TAG = "MainActivity";
    static final String ANT_RADIO_PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.dsi.ant.service.socket";
    static final String ANT_USB_PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.dsi.ant.usbservice";
    private View navigationView;
    private AppStateViewModel state;
    private UserSpinnerController userSpinnerController;
    private ActivityMainBinding binding;
    private AlertDialog uploadProgressDialog;
    private ProgressBar uploadProgressBar;

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
        state.attachForegroundUpload(this);
        state.foregroundUploadState().observe(this, this::renderForegroundUploadState);
        state.foregroundUploadResult().observe(this, this::renderForegroundUploadResult);
        userSpinnerController = new UserSpinnerController(this, state);
        state.loadResult().observe(this, this::onRepositoryLoaded);

        // Handle Back Press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    Fragment current_fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
                    if (current_fragment instanceof EditWeightFragment) {
                        closeEditWeightFragment(null, null, null,
                                ((EditWeightFragment) current_fragment).edit, false);
                    } else if (current_fragment instanceof EditUserFragment) {
                        closeEditUserFragment(null);
                    } else if (current_fragment instanceof EditGoalFragment) {
                        closeEditGoalFragment(null);
                    } else if (!(current_fragment instanceof WeightFragment)) {
                        navigate(NavigationDestination.WEIGHT);
                    } else {
                        // Weight is the root destination, so Back uses the system exit behavior.
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });

        showAntHardwareProblem(antHardwareAvailability(), true);

        if (savedInstanceState == null) {
            navigate(NavigationDestination.WEIGHT);
        } else {
            Fragment restored = getSupportFragmentManager().findFragmentById(R.id.content_frame);
            NavigationDestination selected = NavigationDestination.forFragment(restored);
            if (selected != null) selectNavigationDestination(selected);
        }
        state.ensureLoaded();
    }

    private void onRepositoryLoaded(RepositoryResult<Void> result) {
        if (!result.isSuccess()) {
            Log.e(TAG, result.message, result.error);
            showMessage(result.message);
            return;
        }
        GarminTokenRefreshScheduler.scheduleAll(getApplicationContext(), state.users());
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (state.users().isEmpty() && current instanceof WeightFragment) {
            openEditUserFragment(null);
        } else if (current instanceof WeightFragment) {
            ((WeightFragment) current).updateUi();
        }
    }

    public void openEditUserFragment(User user) {
        EditUserFragment euf = EditUserFragment.newInstance(user == null ? null : user.uuid);
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, euf).commit();

        // update selected item and title, then close the drawer
        selectNavigationDestination(NavigationDestination.USERS);
        setTitle(getString(R.string.lateral_menu_option_users));
    }

    public void closeEditUserFragment(User user) {
        dismissKeyboard();
        if (user == null) {
            navigate(NavigationDestination.USERS);
            return;
        }
        state.saveUser(user, result -> {
            if (handleMutationFailure(result)) return;
            GarminTokenRefreshScheduler.schedule(this, user);
            navigate(NavigationDestination.USERS);
        });
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

    public void closeEditWeightFragment(Weight weight, Weight original, User user,
                                        boolean edit, boolean change) {
        dismissKeyboard();
        NavigationDestination destination = edit
                ? NavigationDestination.HISTORY : NavigationDestination.WEIGHT;
        if (weight == null) {
            navigate(destination);
            return;
        }
        state.saveWeight(weight, original, result -> {
            if (handleMutationFailure(result)) return;
            navigate(destination);
            if (user != null && user.autoupload && change) uploadButton(this, weight, user);
        });
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
        dismissKeyboard();
        if (goal == null) {
            navigate(NavigationDestination.GOALS);
            return;
        }
        state.saveGoal(goal, result -> {
            if (!handleMutationFailure(result)) navigate(NavigationDestination.GOALS);
        });
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

    boolean ensureAntHardwareAvailable() {
        AntHardwareAvailability.Result availability = antHardwareAvailability();
        if (availability == AntHardwareAvailability.Result.AVAILABLE) return true;
        showAntHardwareProblem(availability, false);
        return false;
    }

    private AntHardwareAvailability.Result antHardwareAvailability() {
        boolean radioServiceInstalled = isPackageInstalled("com.dsi.ant.service.socket");
        AntSupport.Capability capability = radioServiceInstalled
                ? AntSupport.detect(this) : AntSupport.Capability.NONE;
        boolean integratedAnt = capability == AntSupport.Capability.BUILT_IN;
        boolean usbDeviceConnected = false;
        if (radioServiceInstalled && !integratedAnt) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            usbDeviceConnected = manager != null && manager.getDeviceList() != null
                    && !manager.getDeviceList().isEmpty();
        }
        boolean antUsbServiceInstalled = usbDeviceConnected
                && capability == AntSupport.Capability.ADD_ON;
        return AntHardwareAvailability.determine(radioServiceInstalled, integratedAnt,
                usbDeviceConnected, antUsbServiceInstalled);
    }

    private void showAntHardwareProblem(AntHardwareAvailability.Result availability,
                                        boolean allowSuppression) {
        switch (availability) {
            case RADIO_SERVICE_MISSING:
                goToMarketANTRadioService();
                break;
            case USB_DEVICE_MISSING:
                SharedPreferences settings = getSharedPreferences(
                        getPackageName() + "_preferences", Context.MODE_PRIVATE);
                if (!allowSuppression || !settings.getBoolean("never_no_ant_msg", false)) {
                    showNoAntMessage(allowSuppression);
                }
                break;
            case USB_SERVICE_MISSING:
                goToMarketANTUSBService();
                break;
            default:
                break;
        }
    }

    public void goToMarketANTRadioService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format(getResources().getString(R.string.msg_problem_service_not_found), getResources().getString(R.string.ant_radio_service)))
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dsi.ant.service.socket")));
                    } catch (android.content.ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(ANT_RADIO_PLAY_STORE_URL)));
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
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(ANT_USB_PLAY_STORE_URL)));
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

    void showNoAntMessage(boolean allowSuppression) {
        if (!this.isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.msg_problem_usb_stick_not_detected)
                    .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.dismiss());
            if (allowSuppression) {
                builder.setNeutralButton(R.string.msg_problem_usb_stick_not_detected_never,
                        (dialog, id) -> {
                            SharedPreferences settings = getSharedPreferences(
                                    getPackageName() + "_preferences", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("never_no_ant_msg", true);
                            editor.apply();
                            dialog.dismiss();
                        });
            }
            builder.create().show();
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
        activity.startForegroundUpload(weight, user, true, true);
    }

    void startForegroundUpload(Weight weight, User user,
                               boolean tryGarmin, boolean tryEmail) {
        if (!isOnline(this)) {
            showMessage(R.string.weight_fragment_msg_problem_no_internet_connection);
            return;
        }
        state.startForegroundUpload(weight, user, tryGarmin, tryEmail);
    }

    private void renderForegroundUploadState(ForegroundUploadState uploadState) {
        if (!uploadState.running) {
            dismissUploadProgress();
            return;
        }
        if (uploadProgressDialog == null) showUploadProgress(uploadState.total);
        if (uploadProgressBar != null) {
            uploadProgressBar.setMax(uploadState.total);
            uploadProgressBar.setProgress(uploadState.completed);
        }
    }

    private void renderForegroundUploadResult(OperationEvent<UploadResult> event) {
        UploadResult result = event.consume();
        if (result == null) return;
        if (result.garminSucceeded) {
            Toast.makeText(this, String.format(
                    getString(R.string.weight_fragment_msg_updating_success),
                    getString(R.string.edit_user_fragment_garmin_connect_category)),
                    Toast.LENGTH_SHORT).show();
        }
        StringBuilder errors = new StringBuilder();
        appendUploadError(errors, getString(
                R.string.edit_user_fragment_garmin_connect_category), result.garminError);
        appendUploadError(errors, getString(R.string.edit_user_fragment_email_category),
                result.emailError);
        if (errors.length() > 0) showMessage(errors.toString());
        if (result.emailMessage != null) {
            MeasurementTextFormatter.EmailMessage message = result.emailMessage;
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{message.recipient});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, message.subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message.body);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }

    private void showUploadProgress(int max) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView message = new TextView(this);
        message.setText(R.string.weight_fragment_msg_uploading);
        message.setPadding(0, 0, 0, 20);
        layout.addView(message);
        uploadProgressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        uploadProgressBar.setIndeterminate(false);
        uploadProgressBar.setMax(max);
        layout.addView(uploadProgressBar);
        uploadProgressDialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> state.cancelForegroundUpload())
                .setCancelable(false)
                .create();
        uploadProgressDialog.show();
    }

    private void dismissUploadProgress() {
        if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
            uploadProgressDialog.dismiss();
        }
        uploadProgressDialog = null;
        uploadProgressBar = null;
    }

    private static void appendUploadError(StringBuilder output, String category, String error) {
        if (error == null || error.isEmpty()) return;
        if (output.length() > 0) output.append('\n');
        output.append(category).append(": ").append(error);
    }

    @Override
    protected void onDestroy() {
        dismissUploadProgress();
        if (state != null) state.detachForegroundUpload(this);
        super.onDestroy();
    }

}
