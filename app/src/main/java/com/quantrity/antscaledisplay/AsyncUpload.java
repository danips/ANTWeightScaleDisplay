package com.quantrity.antscaledisplay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
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
import java.util.Random;

class AsyncUpload extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = "AsyncUpload";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final WeakReference<MainActivity> activityRef;
    private final Weight weight;
    private final User user;
    private ProgressDialog pd;
    private final boolean try_gc;
    private final boolean try_email;

    private static final DateFormat dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
    private static final DecimalFormat df2 = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
    private static final DecimalFormat df1 = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
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

    @Override
    protected Boolean doInBackground(String... paths) {
        boolean start_gc = (user.gc_user != null) && (user.gc_pass != null)
                && (!user.gc_user.equals("")) && (!user.gc_pass.equals("")) && try_gc;
        boolean start_email = (user.email_to != null) && (!user.email_to.equals("")) && try_email;

        final int max = ((start_gc) ? 1 : 0) + ((start_email) ? 1 : 0);
        final MainActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                pd = new ProgressDialog(activity);
                pd.setMessage(activity.getString(R.string.weight_fragment_msg_uploading));
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setCancelable(false);
                pd.setMax(max);
                pd.show();
            });

            for (String path : paths) {
                gcThread gct = null;
                if (start_gc) {
                    try {
                        ProviderInstaller.installIfNeeded(activity);
                    } catch (final GooglePlayServicesRepairableException e) {
                        com.quantrity.antscaledisplay.Log.e("SecurityException", "GooglePlayServicesRepairableException.");
                        // Thrown when Google Play Services is not installed, up-to-date, or enabled
                        // Show dialog to allow users to install, update, or otherwise enable Google Play services.
                        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                        activity.runOnUiThread(() -> apiAvailability.getErrorDialog(activity, e.getConnectionStatusCode(), PLAY_SERVICES_RESOLUTION_REQUEST).show());
                    } catch (GooglePlayServicesNotAvailableException e) {
                        com.quantrity.antscaledisplay.Log.e("SecurityException", "Google Play Services not available. GooglePlayServicesNotAvailableException");
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

                activity.runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) {
                        pd.dismiss();
                        pd = null;
                    }
                });

                if ((start_gc && gct.gc_error != null) || (start_email && et.email_error != null)) {
                    final StringBuilder sb = new StringBuilder();
                    if (start_gc && gct.gc_error != null)
                        sb.append(activity.getString(R.string.edit_user_fragment_garmin_connect_category)).append(": ").append(gct.gc_error);
                    if (start_email && et.email_error != null)
                        sb.append(activity.getString(R.string.edit_user_fragment_email_category)).append(": ").append(et.email_error);
                    activity.runOnUiThread(() -> activity.showMessage(sb.toString()));
                }

                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
        }

        return true;
    }

    private String export2byte(Weight weight) {
        Date date = new Date(weight.date);
        /* TODO: Remove on next release!!!!! */
        date.setSeconds(new Random().nextInt((59) + 1));
        WeightScaleMesg weightMesg = new WeightScaleMesg();
        weightMesg.setTimestamp(new DateTime(date));

        weightMesg.setWeight((float)weight.weight);
        if (weight.percentFat != -1) weightMesg.setPercentFat((float)weight.percentFat);
        if (weight.percentHydration != -1) weightMesg.setPercentHydration((float)weight.percentHydration);
        if (weight.boneMass != -1) weightMesg.setBoneMass((float)weight.boneMass);
        if (weight.muscleMass != -1) weightMesg.setMuscleMass((float)weight.muscleMass);
        if (weight.physiqueRating != -1) weightMesg.setPhysiqueRating((short)weight.physiqueRating);
        if (weight.visceralFatRating != -1) weightMesg.setVisceralFatRating((short) Math.round(weight.visceralFatRating));
        if (weight.metabolicAge != -1) weightMesg.setMetabolicAge((short)weight.metabolicAge);
        if (weight.basalMet != -1) weightMesg.setActiveMet((float)weight.basalMet);
        else if (weight.activeMet != -1) weightMesg.setActiveMet((float)weight.activeMet);

        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setType(com.garmin.fit.File.WEIGHT);
        fileIdMesg.setManufacturer(Manufacturer.TANITA);
        fileIdMesg.setProduct(1);
        fileIdMesg.setSerialNumber(1L);

        FileEncoder encode;
        try {
            String filename = activityRef.get().getFilesDir() + "/weight.fit";
            encode = new FileEncoder(new File(activityRef.get().getFilesDir(), "weight.fit"));//Fit.ProtocolVersion.V2_0);
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
                    GarminConnect gc = new GarminConnect(user, ((MainActivity)activity).getUsersArray(), activity);
                    if (!gc.signin(user.gc_user.trim().replaceAll("[\n\r]", ""), user.gc_pass.trim().replaceAll("[\n\r]", ""), activityRef.get())) {
                        gc_error = activity.getString(R.string.weight_fragment_msg_wrong_credentials);
                    } else {
                        String result = gc.uploadFitFile(new File(encoding_path), activity);
                        if (result == null) {
                            updateSuccess(R.string.edit_user_fragment_garmin_connect_category);
                        } else {
                            gc_error = result;
                        }
                        gc.close();
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

    private void incProgress()
    {
        MainActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> pd.incrementProgressBy(1));
        }
    }

    private void updateSuccess(final int resId)
    {
        final MainActivity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Toast toast = Toast.makeText(activity, String.format(activity.getString(R.string.weight_fragment_msg_updating_success), activity.getString(resId)), Toast.LENGTH_SHORT);
                toast.show();
            });
        }

    }
}
