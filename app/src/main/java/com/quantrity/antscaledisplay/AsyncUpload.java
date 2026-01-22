package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.WeightScaleMesg;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AsyncUpload {
    private static final String TAG = "AsyncUpload";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final WeakReference<MainActivity> activityRef;
    private final Weight weight;
    private final User user;
    private final boolean try_gc;
    private final boolean try_email;

    // Replacements for AsyncTask/ProgressDialog
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private volatile boolean isCancelled = false;

    private static final DateFormat dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
    private static final DecimalFormat df2 = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
    private static final DecimalFormat df1 = (DecimalFormat) DecimalFormat.getInstance(Locale.US);

    static {
        df2.applyPattern("#.##");
        df1.applyPattern("#.#");
    }

    AsyncUpload(MainActivity activity, Weight weight, User user, boolean try_gc, boolean try_email) {
        this.activityRef = new WeakReference<>(activity);
        this.weight = weight;
        this.user = user;
        this.try_gc = try_gc;
        this.try_email = try_email;
    }

    // Emulates AsyncTask.execute()
    public void execute(String... paths) {
        final MainActivity activity = activityRef.get();
        if (activity == null) return;

        // 1. Pre-Execute: Show UI
        boolean start_gc = (user.gc_user != null) && (user.gc_pass != null)
                && (!user.gc_user.isEmpty()) && (!user.gc_pass.isEmpty()) && try_gc;
        boolean start_email = (user.email_to != null) && (!user.email_to.isEmpty()) && try_email;
        final int max = ((start_gc) ? 1 : 0) + ((start_email) ? 1 : 0);

        showCustomProgressDialog(activity, max);

        // 2. Background Execution
        executor.execute(() -> {
            doInBackground(start_gc, start_email, paths);

            // 3. Post-Execute: Dismiss UI
            mainHandler.post(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            });
        });
    }

    // Emulates AsyncTask.cancel()
    public void cancel() {
        isCancelled = true;
        // ExecutorService doesn't support easy interruption of running tasks like AsyncTask
        // without keeping a Future reference, but the flag is sufficient for logical cancellation.
    }

    private void doInBackground(boolean start_gc, boolean start_email, String... paths) {
        final MainActivity activity = activityRef.get();
        if (activity == null) return;

        for (String path : paths) {
            gcThread gct = null;
            if (start_gc) {
                if (Build.VERSION.SDK_INT < 29) {
                    try {
                        ProviderInstaller.installIfNeeded(activity);
                    } catch (final GooglePlayServicesRepairableException e) {
                        Log.e(TAG, "GooglePlayServicesRepairableException.");
                        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                        mainHandler.post(() -> Objects.requireNonNull(apiAvailability.getErrorDialog(activity, e.getConnectionStatusCode(), PLAY_SERVICES_RESOLUTION_REQUEST)).show());
                    } catch (GooglePlayServicesNotAvailableException e) {
                        Log.e(TAG, "Google Play Services not available. GooglePlayServicesNotAvailableException");
                    }
                }
                gct = new gcThread();
                gct.path = path;
                gct.start();
            }

            emailThread et = null;
            if (start_email) {
                et = new emailThread();
                et.start();
            }

            try {
                if (gct != null) gct.join();
                if (et != null) et.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if ((start_gc && gct.gc_error != null) || (start_email && et.email_error != null)) {
                final StringBuilder sb = new StringBuilder();
                if (start_gc && gct.gc_error != null)
                    sb.append(activity.getString(R.string.edit_user_fragment_garmin_connect_category)).append(": ").append(gct.gc_error);
                if (start_email && et.email_error != null)
                    sb.append(activity.getString(R.string.edit_user_fragment_email_category)).append(": ").append(et.email_error);

                mainHandler.post(() -> activity.showMessage(sb.toString()));
            }

            if (isCancelled) break;
        }
    }

    // Replaces the deprecated ProgressDialog with an AlertDialog containing a ProgressBar
    private void showCustomProgressDialog(MainActivity activity, int max) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvMessage = new TextView(activity);
        tvMessage.setText(activity.getString(R.string.weight_fragment_msg_uploading));
        tvMessage.setPadding(0, 0, 0, 20);
        layout.addView(tvMessage);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(max);
        layout.addView(progressBar);

        builder.setView(layout);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void incProgress() {
        mainHandler.post(() -> {
            if (progressBar != null) {
                progressBar.incrementProgressBy(1);
            }
        });
    }

    private String export2byte(Weight weight) {
        Date date = new Date(weight.date);
        WeightScaleMesg weightMesg = new WeightScaleMesg();
        weightMesg.setTimestamp(new DateTime(date));

        weightMesg.setWeight((float) weight.weight);
        if (weight.percentFat != -1) weightMesg.setPercentFat((float) weight.percentFat);
        if (weight.percentHydration != -1)
            weightMesg.setPercentHydration((float) weight.percentHydration);
        if (weight.boneMass != -1) weightMesg.setBoneMass((float) weight.boneMass);
        if (weight.muscleMass != -1) weightMesg.setMuscleMass((float) weight.muscleMass);
        if (weight.physiqueRating != -1)
            weightMesg.setPhysiqueRating((short) weight.physiqueRating);
        if (weight.visceralFatRating != -1)
            weightMesg.setVisceralFatRating((short) Math.round(weight.visceralFatRating));
        if (weight.metabolicAge != -1) weightMesg.setMetabolicAge((short) weight.metabolicAge);
        if (weight.basalMet != -1) weightMesg.setActiveMet((float) weight.basalMet);
        else if (weight.activeMet != -1) weightMesg.setActiveMet((float) weight.activeMet);
        if ((weight.height != -1) && (weight.weight != -1)) weightMesg.setBmi((float) (weight.weight / Math.pow((weight.height) / 100, 2)));

        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setType(com.garmin.fit.File.WEIGHT);
        fileIdMesg.setManufacturer(Manufacturer.TANITA);
        fileIdMesg.setProduct(1);
        fileIdMesg.setSerialNumber(1L);

        FileEncoder encode;
        try {
            String filename = activityRef.get().getFilesDir() + "/weight.fit";
            encode = new FileEncoder(new File(activityRef.get().getFilesDir(), "weight.fit"), Fit.ProtocolVersion.V2_0);
            encode.write(fileIdMesg);
            encode.write(weightMesg);
            encode.close();
            return filename;
        } catch (FitRuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class gcThread extends Thread {
        String gc_error = null;
        String path = null;

        @Override
        public void run() {
            String encoding_path = export2byte(weight);
            MainActivity activity = activityRef.get();

            if ((encoding_path == null) || (encoding_path.isEmpty())) {
                if (activity != null) {
                    gc_error = activity.getString(R.string.weight_fragment_msg_uploading_encoding_failure);
                }
            } else {
                if (activity != null) {
                    GarminConnect gc = new GarminConnect(user, activity.getUsersArray(), activity);
                    if (gc.signin(user)) {
                        String result = gc.uploadFitFile(new File(encoding_path));
                        if (result == null) {
                            updateSuccess(R.string.edit_user_fragment_garmin_connect_category);
                        } else {
                            gc_error = result;
                        }
                    } else {
                        gc_error = activity.getString(R.string.weight_fragment_msg_wrong_credentials);
                    }
                }
            }
            incProgress();
        }
    }

    private class emailThread extends Thread {
        String email_error = null;

        @Override
        public void run() {
            try {
                final MainActivity activity = activityRef.get();
                if (activity != null) {
                    String subject = user.name + " " + activity.getString(R.string.lateral_menu_option_weight) + " " + dtf.format(weight.date);
                    StringBuilder body = new StringBuilder();

                    //Build email body
                    body.append(activity.getString(R.string.edit_user_fragment_user) + ": " + user.name + "\n");
                    body.append(activity.getString(R.string.edit_user_fragment_height) + ": "
                            + ((user.usesCm) ? user.height_cm + " " + activity.getString(R.string.edit_user_fragment_units_tag_cm)
                            : user.height_ft + " " + activity.getString(R.string.edit_user_fragment_units_tag_ft) + user.height_in + " " + activity.getString(R.string.edit_user_fragment_units_tag_in)) + "\n");

                    if (weight.weight != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_weight) + ": " + user.printMass(activity, weight.weight) + "\n");
                    if (weight.percentFat != -1) {
                        if (user.show_fat_mass) {
                            body.append(activity.getString(R.string.weight_fragment_icon_desc_percentFat) + ": " + user.printMass(activity, weight.weight * weight.percentFat / 100) + "\n");
                        } else {
                            body.append(activity.getString(R.string.weight_fragment_icon_desc_percentFat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), (float) weight.percentFat) + "\n");
                        }
                    }
                    if (weight.percentHydration != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_percentHydration) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.percentHydration) + "\n");

                    if (weight.trunkPercentFat != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_trunk_percent_fat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.trunkPercentFat) + "\n");
                    if (weight.trunkMuscleMass != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_trunk_muscle_mass) + ": " + user.printMass(activity, weight.trunkMuscleMass) + "\n");
                    if (weight.leftArmPercentFat != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_left_arm_percent_fat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.leftArmPercentFat) + "\n");
                    if (weight.leftArmMuscleMass != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_left_arm_muscle_mass) + ": " + user.printMass(activity, weight.leftArmMuscleMass) + "\n");
                    if (weight.rightArmPercentFat != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_right_arm_percent_fat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.rightArmPercentFat) + "\n");
                    if (weight.rightArmMuscleMass != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_right_arm_muscle_mass) + ": " + user.printMass(activity, weight.rightArmMuscleMass) + "\n");
                    if (weight.leftLegPercentFat != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_left_leg_percent_fat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.leftLegPercentFat) + "\n");
                    if (weight.leftLegMuscleMass != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_left_leg_muscle_mass) + ": " + user.printMass(activity, weight.leftLegMuscleMass) + "\n");
                    if (weight.rightLegPercentFat != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_right_leg_percent_fat) + ": " + String.format(activity.getString(R.string.weight_fragment_percent_tag), weight.rightLegPercentFat) + "\n");
                    if (weight.rightLegMuscleMass != -1)
                        body.append(activity.getString(R.string.graphs_fragment_measurement_right_leg_muscle_mass) + ": " + user.printMass(activity, weight.rightLegMuscleMass) + "\n");

                    if (weight.boneMass != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_boneMass) + ": " + user.printMass(activity, weight.boneMass) + "\n");
                    if (weight.muscleMass != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_muscleMass) + ": " + user.printMass(activity, weight.muscleMass) + "\n");
                    if (weight.physiqueRating != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_physiqueRating) + ": " + weight.physiqueRating + "\n");
                    if (weight.visceralFatRating != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_visceralFat) + ": " + df2.format(weight.visceralFatRating) + "\n");
                    if (weight.metabolicAge != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_metabolicAge) + ": " + String.format(activity.getString(R.string.weight_fragment_years_tag), weight.metabolicAge) + "\n");
                    if (weight.activeMet != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_activeMet) + ": " + String.format(activity.getString(R.string.weight_fragment_kcal_tag), weight.activeMet) + "\n");
                    if (weight.basalMet != -1)
                        body.append(activity.getString(R.string.weight_fragment_icon_desc_basalMet) + ": " + String.format(activity.getString(R.string.weight_fragment_kcal_tag), weight.basalMet));

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("vnd.android.cursor.dir/email");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{user.email_to});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
                    activity.startActivity(Intent.createChooser(emailIntent, "Send email..."));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            incProgress();
        }
    }

    private void updateSuccess(final int resId) {
        final MainActivity activity = activityRef.get();
        if (activity != null) {
            mainHandler.post(() -> {
                Toast toast = Toast.makeText(activity, String.format(activity.getString(R.string.weight_fragment_msg_updating_success), activity.getString(resId)), Toast.LENGTH_SHORT);
                toast.show();
            });
        }
    }
}