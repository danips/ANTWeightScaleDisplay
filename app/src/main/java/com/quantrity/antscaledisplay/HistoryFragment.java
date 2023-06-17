package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

public class HistoryFragment extends Fragment {
    private final static String TAG = "HistoryFragment";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static String GARMIN_CONNECT_CHANNEL_ID = "GC";
    private final static int GARMIN_CONNECT_NOTIFICATION_ID = 0;

    HistoryAdapter mAdapter;

    //MenuItems for controlling the Download button
    private MenuItem downloadMI = null;
    private MenuItem gcMI = null;
    private Spinner usersSpinner = null;

    public HistoryFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.history_recycler_view);

        if (getActivity() != null) {
            // use a linear layout manager
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);

            mAdapter = new HistoryAdapter(((MainActivity) getActivity()).getHistoryArraySelectedUser(), getActivity(), ((MainActivity) getActivity()).getSelectedUser());
            mRecyclerView.setAdapter(mAdapter);
        }

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Log.v(TAG, "onItemSelected " + view);
            if ((view != null) && (getActivity() != null)) {
                //Log.v(TAG, "onItemSelected2 " + view);
                User user = (User)adapterView.getItemAtPosition(i);
                ((MainActivity)getActivity()).setSelectedUser(user);

                //Mostrar todos los pesos del usuario
                mAdapter.replaceAll(((MainActivity)getActivity()).getHistoryArraySelectedUser(), user);

                if ((user.gc_user != null && user.gc_pass != null)) {
                    downloadMI.setVisible(true);
                    gcMI.setVisible(true);
                } else downloadMI.setVisible(false);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        //Log.v(TAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_history_menu, menu);
        if (getActivity() != null) {
            usersSpinner = ((MainActivity)getActivity()).addUsersSpinner(menu, oisListener);
        }
        downloadMI = menu.findItem(R.id.action_download_history);
        downloadMI.setVisible(false);
        gcMI = menu.findItem(R.id.action_download_history_gc);
        gcMI.setVisible(false);
        MenuItem csvMI = menu.findItem(R.id.action_export_history);
        csvMI.setVisible(false);
        User user = ((MainActivity) getActivity()).getSelectedUser();
        if ((user != null) && ((user.gc_user != null && user.gc_pass != null))) {
                //|| (user.tp_access_token != null && user.tp_refresh_token != null))) {
            downloadMI.setVisible(true);
            //Hide GC
            gcMI.setVisible(!(user.gc_user == null && user.gc_pass == null));
        }
        csvMI.setVisible(mAdapter.getItemCount() != 0);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == R.id.action_download_history_gc) {
            download_history_gc();
            return true;
        } else if (itemId == R.id.action_export_history_csv) {
            export_history_csv();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void export_history_csv() {
        if (Debug.ON) Log.v(TAG, "export_history_csv");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, MainActivity.CSV_DIRECTORY_PICKER_RESULT);
        }
        else {
            ExFilePicker exFilePicker = new ExFilePicker();
            exFilePicker.setCanChooseOnlyOneItem(true);
            exFilePicker.setSortButtonDisabled(true);
            exFilePicker.setChoiceType(ExFilePicker.ChoiceType.DIRECTORIES);
            exFilePicker.start(this, MainActivity.CSV_DIRECTORY_PICKER_RESULT);
        }
    }

    private void writeCSV(String dst, User user, SimpleDateFormat format, String filename, List<Weight> wl)
    {
        try {
            FileWriter fCsv = new FileWriter(dst);
            writeCSV(fCsv, user, format, filename, wl);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCSV(ParcelFileDescriptor destFileDesc, User user, SimpleDateFormat format, String filename, List<Weight> wl)
    {
        FileWriter fCsv = new FileWriter(destFileDesc.getFileDescriptor());
        writeCSV(fCsv, user, format, filename, wl);
    }

    private void writeCSV(FileWriter fCsv, User user, SimpleDateFormat format, String filename, List<Weight> wl)
    {
        try {
            fCsv.append(getString(R.string.edit_user_fragment_user)).append(",");
            fCsv.append(getString(R.string.history_fragment_date)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_weight)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_percentFat)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_percentHydration)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_muscleMass)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_physiqueRating)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_visceralFat)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_boneMass)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_metabolicAge)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_basalMet)).append(",");
            fCsv.append(getString(R.string.weight_fragment_icon_desc_activeMet)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_trunk_percent_fat)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_trunk_muscle_mass)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_left_arm_percent_fat)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_left_arm_muscle_mass)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_right_arm_percent_fat)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_right_arm_muscle_mass)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_left_leg_percent_fat)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_left_leg_muscle_mass)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_right_leg_percent_fat)).append(",");
            fCsv.append(getString(R.string.graphs_fragment_measurement_right_leg_muscle_mass)).append("\n");

            DecimalFormat df = (DecimalFormat)DecimalFormat.getInstance(Locale.US);
            df.applyPattern("#.##");
            for (Weight w : wl) {
                fCsv.append(user.name).append(",");
                fCsv.append((w.date != -1) ? format.format(w.date) : "").append(",");
                fCsv.append((w.weight != -1) ? df.format(w.weight) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.percentFat != -1) ? df.format(w.weight * w.percentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.percentFat != -1) ? df.format(w.percentFat) : "").append(",");
                }
                fCsv.append((w.percentHydration != -1) ? df.format(w.percentHydration) : "").append(",");
                fCsv.append((w.muscleMass != -1) ? df.format(w.muscleMass) : "").append(",");
                fCsv.append((w.physiqueRating != -1) ? w.physiqueRating+"" : "").append(",");
                fCsv.append((w.visceralFatRating != -1) ? df.format(w.visceralFatRating) : "").append(",");
                fCsv.append((w.boneMass != -1) ? df.format(w.boneMass) : "").append(",");
                fCsv.append((w.metabolicAge != -1) ? w.metabolicAge + "":"").append(",");
                fCsv.append((w.basalMet != -1) ? df.format(w.basalMet) : "").append(",");
                fCsv.append((w.activeMet != -1) ? df.format(w.activeMet) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.trunkPercentFat != -1) ? user.printMass(getContext(), w.weight * w.trunkPercentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.trunkPercentFat != -1) ? df.format(w.trunkPercentFat) : "").append(",");
                }
                fCsv.append((w.trunkMuscleMass != -1) ? df.format(w.trunkMuscleMass) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.leftArmPercentFat != -1) ? df.format(w.weight * w.leftArmPercentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.leftArmPercentFat != -1) ? df.format(w.leftArmPercentFat) : "").append(",");
                }
                fCsv.append((w.leftArmMuscleMass != -1) ? df.format(w.leftArmMuscleMass) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.rightArmPercentFat != -1) ? df.format(w.weight * w.rightArmPercentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.rightArmPercentFat != -1) ? df.format(w.rightArmPercentFat) : "").append(",");
                }
                fCsv.append((w.rightArmMuscleMass != -1) ? df.format(w.rightArmMuscleMass) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.leftLegPercentFat != -1) ? df.format(w.weight * w.leftLegPercentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.leftLegPercentFat != -1) ? df.format(w.leftLegPercentFat) : "").append(",");
                }
                fCsv.append((w.leftLegMuscleMass != -1) ? df.format(w.leftLegMuscleMass) : "").append(",");
                if (user.show_fat_mass) {
                    fCsv.append((w.rightLegPercentFat != -1) ? df.format(w.weight * w.rightLegPercentFat / 100) : "").append(",");
                }
                else
                {
                    fCsv.append((w.rightLegPercentFat != -1) ? df.format(w.rightLegPercentFat) : "").append(",");
                }
                fCsv.append((w.rightLegMuscleMass != -1) ? df.format(w.rightLegMuscleMass) : "").append("\n");
            }

            fCsv.flush();
        } catch (Exception e) {
            if (Debug.ON) Log.e(TAG, "CSV_DIRECTORY_PICKER_RESULT :: " + e);
        } finally {
            try {
                if (fCsv != null) fCsv.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(getActivity(), String.format(getString(R.string.history_fragment_action_database_backup_ok), filename), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult " + requestCode + " " + resultCode + " " + data);
        if ((requestCode == MainActivity.CSV_DIRECTORY_PICKER_RESULT) && (getActivity() != null) && (resultCode == Activity.RESULT_OK) && (data != null)) {
            List<Weight> wl = ((MainActivity)getActivity()).getHistoryArraySelectedUser();
            Calendar cal = Calendar.getInstance();
            User user = (User) usersSpinner.getSelectedItem();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
            String displayName = user.name + "_" + format1.format(cal.getTime()) + ".csv";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Uri uri = data.getData();
                ContentResolver contentResolver;
                if ((getActivity() != null) && ((contentResolver = getActivity().getContentResolver()) != null)) {
                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                    try {
                        Uri fileUri = DocumentsContract.createDocument(contentResolver, docUri, "text/csv", displayName);
                        ParcelFileDescriptor destFileDesc = contentResolver.openFileDescriptor(fileUri, "w", null);
                        writeCSV(destFileDesc, user, format1, displayName, wl);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
                if ((result != null) && (result.getCount() > 0) && (!result.getNames().isEmpty()) && (getActivity() != null)) {
                    String dst = result.getPath() + result.getNames().get(0) + File.separator + displayName;
                    writeCSV(dst, user, format1, displayName, wl);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void download_history_gc() {
        if ((getActivity() == null) || (getContext() == null)) return;

        try {
            ProviderInstaller.installIfNeeded(getContext());
        } catch (final GooglePlayServicesRepairableException e) {
            com.quantrity.antscaledisplay.Log.e("SecurityException", "GooglePlayServicesRepairableException.");
            // Thrown when Google Play Services is not installed, up-to-date, or enabled
            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
            final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            getActivity().runOnUiThread(() -> Objects.requireNonNull(apiAvailability.getErrorDialog(getActivity(), e.getConnectionStatusCode(), PLAY_SERVICES_RESOLUTION_REQUEST)).show());
        } catch (GooglePlayServicesNotAvailableException e) {
            com.quantrity.antscaledisplay.Log.e("SecurityException", "Google Play Services not available. GooglePlayServicesNotAvailableException");
        }

        final NotificationManager notificationManager = (NotificationManager)getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext(), GARMIN_CONNECT_CHANNEL_ID);
        mBuilder.setContentTitle(String.format(getString(R.string.history_fragment_download), getString(R.string.edit_user_fragment_garmin_connect_category)))
                .setContentText(getString(R.string.history_fragment_download_in_progress))
                .setSmallIcon(R.drawable.ic_gc)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_gc))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Issue the initial notification with zero progress
        mBuilder.setProgress(1, 0, false);
        notificationManager.notify(GARMIN_CONNECT_NOTIFICATION_ID, mBuilder.build());

        // Start a lengthy operation in a background thread
        // Starts the thread by calling the run() method in its Runnable
        new Thread(
                () -> {
                    if ((getActivity() == null) || (getContext() == null)) return;
                    StringBuilder result = new StringBuilder();
                    boolean success = false;
                    try {
                        final User user = (User)usersSpinner.getSelectedItem();
                        GarminConnect gc = new GarminConnect();
                        if (!gc.signin(user.gc_user.trim().replaceAll("[\n\r]", ""), user.gc_pass.trim().replaceAll("[\n\r]", ""))) {
                            result.append(getString(R.string.weight_fragment_msg_wrong_credentials));
                        }
                        else {
                            success = gc.downloadHistory(getActivity(), result);
                            if (success) {
                                String history = result.toString();
                                List<Weight> wl = ((MainActivity)getActivity()).getHistoryArray();
                                List<Weight> tmp = new ArrayList<>();

                                JSONObject jo = new JSONObject(history);
                                JSONArray jsonArray = jo.getJSONArray("dailyWeightSummaries");
                                if (Debug.ON) Log.v(TAG, "Total samples: " + jsonArray.length());
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    // Sets the progress indicator to a max value, the current
                                    // completion percentage, and "determinate" state
                                    mBuilder.setProgress(jsonArray.length(), i + 1, false);
                                    // Displays the progress bar for the first time.
                                    notificationManager.notify(GARMIN_CONNECT_NOTIFICATION_ID, mBuilder.build());

                                    JSONObject jsono = jsonArray.getJSONObject(i);
                                    JSONObject jsonobject = jsono.getJSONObject("latestWeight");

                                    long lDate;
                                    double weight, bodyMetabolicAge;
                                    Weight oW = new Weight();

                                    if (!jsonobject.isNull("timestampGMT")) {
                                        lDate = jsonobject.getLong("timestampGMT");
                                    } else if (!jsonobject.isNull("date")) {
                                        lDate = jsonobject.getLong("date");
                                    } else {
                                        continue;
                                    }
                                    weight = jsonobject.getDouble("weight");
                                    oW.weight = weight / 1000.0;

                                    oW.percentFat = (jsonobject.isNull("bodyFat"))  ? -1 : jsonobject.getDouble("bodyFat");
                                    oW.percentHydration = (jsonobject.isNull("bodyWater"))  ? -1 : jsonobject.getDouble("bodyWater");
                                    if (!jsonobject.isNull("metabolicAge")) {
                                        bodyMetabolicAge = jsonobject.getDouble("metabolicAge");
                                        double res = bodyMetabolicAge / 365250;
                                        res = res / 86400;
                                        oW.metabolicAge = (int)Math.round(res);
                                    }
                                    oW.visceralFatRating = (jsonobject.isNull("visceralFat"))  ? -1 : jsonobject.getInt("visceralFat");
                                    oW.physiqueRating = (jsonobject.isNull("physiqueRating"))  ? -1 : jsonobject.getInt("physiqueRating");
                                    oW.muscleMass = (jsonobject.isNull("muscleMass"))  ? -1 : jsonobject.getInt("muscleMass") / 1000.0;
                                    oW.boneMass = (jsonobject.isNull("boneMass"))  ? -1 : jsonobject.getInt("boneMass") / 1000.0;

                                    boolean repeated = false;
                                    //Comprobar que no sea repetida
                                    for (Weight w : wl) {
                                        //if (Debug.ON) Log.v(TAG, "Comparing " + w.date + " and " + lDate + " = " + (w.date - lDate));
                                        long delta = lDate - w.date;

                                        if (delta > 7200000L)//60*60*1000
                                        {
                                            //if (Debug.ON) Log.v(TAG, ".- Too old measurements IN=" + new Date(w.date) + " and NEW=" + new Date(lDate) + " = " + (w.date - lDate));
                                            break;
                                        }
                                        else
                                        {
                                            delta = Math.abs(delta);
                                            if ((w.uuid.equals(user.uuid)) && (delta <= 7200000L)) {
                                                repeated = true;
                                                repeated &= (Math.abs(oW.weight - w.weight) < 0.05);
                                                int out = 1;
                                                if (repeated && (oW.percentFat != -1))
                                                {
                                                    repeated &= (Math.abs(oW.percentFat - w.percentFat) < 0.01);
                                                    out = 2;
                                                }
                                                if (repeated && (oW.percentHydration != -1))
                                                {
                                                    repeated &= (Math.abs(oW.percentHydration - w.percentHydration) < 0.01);
                                                    out = 3;
                                                }
                                                if (repeated && (oW.boneMass != -1))
                                                {
                                                    repeated &= (Math.abs(oW.boneMass - w.boneMass) < 0.01);
                                                    out = 4;
                                                }
                                                if (repeated && (oW.muscleMass != -1))
                                                {
                                                    repeated &= (Math.abs(oW.muscleMass - w.muscleMass) < 0.01);
                                                    out = 5;
                                                }
                                                if (repeated && (oW.physiqueRating != -1))
                                                {
                                                    repeated &= (oW.physiqueRating == w.physiqueRating);
                                                    out = 6;
                                                }
                                                if (repeated && (oW.percentFat != -1))
                                                {
                                                    repeated &= (Math.round(oW.visceralFatRating) == Math.round(w.visceralFatRating));
                                                    out = 7;
                                                }
                                                if (repeated && (oW.metabolicAge != -1))
                                                {
                                                    repeated &= (oW.metabolicAge - w.metabolicAge <= 1);
                                                    out = 8;
                                                }

                                                if (repeated) break;
                                                else if (Debug.ON)
                                                {
                                                    Log.v(TAG, jsonobject.toString());
                                                    Log.v(TAG, "Same date: DIFF=" + (lDate - w.date));

                                                    Log.v(TAG, "***** WHEN=" + lDate + " " + " WEIGHT=" + weight + " BODYFAT=" + oW.percentFat
                                                            + " BODYWATER=" + oW.percentHydration
                                                            + " AGE=" + oW.metabolicAge
                                                            + " VISCERALFAT=" + oW.visceralFatRating + " PHYRATING=" + oW.physiqueRating
                                                            + " MUSCLEMASS=" + oW.muscleMass + " BONEMASS=" + oW.boneMass);

                                                    Date tp = new Date(w.date);
                                                    Log.v(TAG, "***** WHEN=" + w.date + " " + tp + " WEIGHT=" + w.weight + " BODYFAT=" + w.percentFat
                                                            + " BODYWATER=" + w.percentHydration
                                                            + " activeMet=" + w.activeMet + " basalMet=" + w.basalMet + " AGE=" + w.metabolicAge
                                                            + " VISCERALFAT=" + w.visceralFatRating + " PHYRATING=" + w.physiqueRating
                                                            + " MUSCLEMASS=" + w.muscleMass + " BONEMASS=" + w.boneMass);
                                                    Log.v(TAG, "out="+out);
                                                    Log.v(TAG, "r1=" + repeated + " " + Math.abs(oW.weight - w.weight));
                                                    Log.v(TAG, "r2=" + repeated + " " + Math.abs(oW.percentFat - w.percentFat));
                                                    Log.v(TAG, "r3=" + repeated+ " " + Math.abs(oW.percentHydration - w.percentHydration));
                                                    Log.v(TAG, "r4=" + repeated+ " " + Math.abs(oW.boneMass - w.boneMass));
                                                    Log.v(TAG, "r5=" + repeated+ " " + Math.abs(oW.muscleMass - w.muscleMass));
                                                    Log.v(TAG, "r6=" + repeated+ " " + oW.physiqueRating  + " " + w.physiqueRating);
                                                    Log.v(TAG, "r7=" + repeated+ " " + Math.abs(oW.visceralFatRating - w.visceralFatRating) + " " + oW.visceralFatRating + " " + w.visceralFatRating);
                                                    Log.v(TAG, "r8=" + repeated+ " " + oW.metabolicAge + " " + w.metabolicAge);
                                                    Log.v(TAG, "r9=" + repeated+ " " + Math.abs(oW.basalMet - w.basalMet) + " " + oW.basalMet + " " + w.basalMet);
                                                }
                                            }
                                        }
                                    }

                                    if (!repeated) {
                                        oW.date = lDate;
                                        oW.uuid = user.uuid;
                                        oW.height = user.height_cm;
                                        oW.age = user.age;
                                        oW.isMale = user.isMale;

                                        //Insertar en la lista
                                        tmp.add(oW);
                                    }
                                }

                                //Ordenar la lista
                                if (Debug.ON) Log.v(TAG, "Total samples: " + jsonArray.length() + " adding " + tmp.size());
                                wl.addAll(tmp);
                                Collections.sort(wl, new Weight.DateComparator());
                                //Hacer que se muestre la nueva lista
                                getActivity().runOnUiThread(() -> mAdapter.replaceAll(((MainActivity)getActivity()).getHistoryArraySelectedUser(), user));

                                //Guardar el nuevo historial a disco
                                Weight.serializeWeight(getActivity(), wl);

                                notificationManager.cancel(GARMIN_CONNECT_NOTIFICATION_ID);
                                success = true;
                            }
                        }
                        gc.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        result.append("Exception: ").append(e);
                    }
                    if (!success) {
                        // When the loop is finished, updates the notification
                        mBuilder.setContentText(result.toString())
                                // Removes the progress bar
                                .setProgress(0, 0, false);
                        notificationManager.notify(GARMIN_CONNECT_NOTIFICATION_ID, mBuilder.build());
                    }
                }).start();
    }

}
