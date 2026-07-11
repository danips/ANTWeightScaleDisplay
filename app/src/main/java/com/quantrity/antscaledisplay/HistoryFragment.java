package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.quantrity.antscaledisplay.databinding.FragmentHistoryBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HistoryFragment extends Fragment implements MenuProvider {
    private final static String TAG = "HistoryFragment";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static String GARMIN_CONNECT_CHANNEL_ID = "GC";
    private final static int GARMIN_CONNECT_NOTIFICATION_ID = 0;

    HistoryAdapter mAdapter;

    //MenuItems for controlling the Download button
    private MenuItem downloadMI = null;
    private MenuItem gcMI = null;
    private Spinner usersSpinner = null;
    private RecyclerView mRecyclerView = null;
    private AppStateViewModel state;
    private FragmentHistoryBinding binding;

    // Launcher for CSV Export Directory Picker
    private final ActivityResultLauncher<Intent> csvExportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Log.v(TAG, "CSV Directory Selected: " + data.getData());

                    if (getActivity() != null && usersSpinner != null) {
                        List<Weight> wl = state.selectedWeights();
                        Calendar cal = Calendar.getInstance();
                        User user = (User) usersSpinner.getSelectedItem();
                        SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                        String displayName = user.name + "_" + format1.format(cal.getTime()) + ".csv";
                        Uri uri = data.getData();
                        ContentResolver contentResolver = getActivity().getContentResolver();

                        if (contentResolver != null && uri != null) {
                            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                            try {
                                Uri fileUri = DocumentsContract.createDocument(contentResolver, docUri, "text/csv", displayName);
                                if (fileUri != null) {
                                    ParcelFileDescriptor destFileDesc = contentResolver.openFileDescriptor(fileUri, "w", null);
                                    if (destFileDesc != null) {
                                        writeCSV(destFileDesc, user, format1, displayName, wl);
                                    }
                                }
                            } catch (FileNotFoundException e) {
                                Log.e(TAG, "Unable to open the CSV export destination", e);
                            }
                        }
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        mRecyclerView = binding.historyRecyclerView;

        if (getActivity() != null) {
            // use a linear layout manager
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);

            mAdapter = new HistoryAdapter(state.selectedWeights(), getActivity(), state.selectedUser(), this);
            mRecyclerView.setAdapter(mAdapter);
        }

        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        mRecyclerView.setAdapter(null);
        mRecyclerView = null;
        mAdapter = null;
        usersSpinner = null;
        downloadMI = null;
        gcMI = null;
        binding = null;
        super.onDestroyView();
    }

    void deleteWeight(Weight weight) {
        state.deleteWeight(weight);
    }

    void editWeight(Weight weight, User user) {
        if (getActivity() != null) {
            ((MainActivity) getActivity()).openEditWeightFragment(weight, user, true);
        }
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Log.v(TAG, "onItemSelected " + view);
            if ((view != null) && (getActivity() != null)) {
                //Log.v(TAG, "onItemSelected2 " + view);
                User user = (User) adapterView.getItemAtPosition(i);
                state.selectUser(user);

                //Mostrar todos los pesos del usuario
                mAdapter.replaceAll(state.selectedWeights(), user);

                if ((user.gc_user != null && user.gc_pass != null)) {
                    downloadMI.setVisible(true);
                    gcMI.setVisible(true);
                } else downloadMI.setVisible(false);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_history_menu, menu);
        if (getActivity() != null) {
            usersSpinner = ((MainActivity) getActivity()).addUsersSpinner(menu, oisListener);
        }
        downloadMI = menu.findItem(R.id.action_download_history);
        downloadMI.setVisible(false);
        gcMI = menu.findItem(R.id.action_download_history_gc);
        gcMI.setVisible(false);
        MenuItem csvMI = menu.findItem(R.id.action_export_history);
        csvMI.setVisible(false);
        if (getActivity() != null) {
            User user = state.selectedUser();
            if ((user != null) && ((user.gc_user != null && user.gc_pass != null))) {
                //|| (user.tp_access_token != null && user.tp_refresh_token != null))) {
                downloadMI.setVisible(true);
                //Hide GC
                gcMI.setVisible(!(user.gc_user == null && user.gc_pass == null));
            }
        }
        csvMI.setVisible(mAdapter.getItemCount() != 0);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_download_history_gc) {
            download_history_gc();
            return true;
        } else if (itemId == R.id.action_export_history_csv) {
            export_history_csv();
            return true;
        } else if (itemId == R.id.action_jump_to_date) { // <--- Add this block
            showJumpToDateDialog();
            return true;
        }
        return false;
    }

    private void showJumpToDateDialog() {
        if ((getContext() == null) || (getActivity() == null)) return;

        final Calendar c = Calendar.getInstance();
        android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(getActivity(),
                (view, year, month, dayOfMonth) -> {
                    Calendar target = Calendar.getInstance();
                    target.set(year, month, dayOfMonth);
                    jumpToClosestDate(target.getTimeInMillis());
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void jumpToClosestDate(long targetTime) {
        if (getActivity() == null || mRecyclerView == null) return;

        // 1. Get the current list from MainActivity
        List<Weight> history = state.selectedWeights();
        if (history == null || history.isEmpty()) return;

        int closestIndex = -1;
        long minDiff = Long.MAX_VALUE;

        // 2. Iterate to find the closest date match
        for (int i = 0; i < history.size(); i++) {
            long diff = Math.abs(history.get(i).date - targetTime);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        // 3. Scroll to that position
        if (closestIndex != -1) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                // scrollToPositionWithOffset(index, 0) puts the item at the very top of the screen
                layoutManager.scrollToPositionWithOffset(closestIndex, 0);

                // Optional: Show a toast confirming the date found
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String foundDate = sdf.format(new Date(history.get(closestIndex).date));
                Toast.makeText(getContext(), "Jumped to " + foundDate, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void export_history_csv() {
        if (Debug.ON) Log.v(TAG, "export_history_csv");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        // Using new Launcher instead of deprecated startActivityForResult
        csvExportLauncher.launch(intent);
    }

    private void writeCSV(ParcelFileDescriptor destFileDesc, User user, SimpleDateFormat format, String filename, List<Weight> wl) {
        FileWriter fCsv = new FileWriter(destFileDesc.getFileDescriptor());
        writeCSV(fCsv, user, format, filename, wl);
    }

    private void writeCSV(FileWriter fCsv, User user, SimpleDateFormat format, String filename, List<Weight> wl) {
        try {
            fCsv.append(getString(R.string.edit_user_fragment_user)).append(",");
            fCsv.append(getString(R.string.history_fragment_date));
            for (Metric metric : Metric.exportMetrics()) {
                fCsv.append(",").append(getString(metric.getLabelRes()));
            }
            fCsv.append("\n");

            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
            df.applyPattern("#.##");
            for (Weight w : wl) {
                fCsv.append(user.name).append(",");
                fCsv.append((w.date != -1) ? format.format(w.date) : "");
                for (Metric metric : Metric.exportMetrics()) {
                    fCsv.append(",").append(MetricFormatter.csv(df, user, w, metric));
                }
                fCsv.append("\n");
            }

            fCsv.flush();
        } catch (Exception e) {
            Log.e(TAG, "CSV_DIRECTORY_PICKER_RESULT :: " + e);
        } finally {
            try {
                if (fCsv != null) fCsv.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to close the CSV export", e);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), String.format(getString(R.string.history_fragment_action_database_backup_ok), filename), Toast.LENGTH_LONG).show()
                );
            }
        }
    }

    private void download_history_gc() {
        if ((getActivity() == null) || (getContext() == null)) return;

        if (Build.VERSION.SDK_INT < 29) {
            try {
                ProviderInstaller.installIfNeeded(getContext());
            } catch (final GooglePlayServicesRepairableException e) {
                Log.e(TAG, "GooglePlayServicesRepairableException.");
                // Thrown when Google Play Services is not installed, up-to-date, or enabled
                // Show dialog to allow users to install, update, or otherwise enable Google Play services.
                final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                getActivity().runOnUiThread(() -> Objects.requireNonNull(apiAvailability.getErrorDialog(getActivity(), e.getConnectionStatusCode(), PLAY_SERVICES_RESOLUTION_REQUEST)).show());
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e(TAG, "Google Play Services not available. GooglePlayServicesNotAvailableException");
            }
        }

        final NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
                    boolean success = false;
                    String errorMessage = "";
                    try {
                        final User user = (User) usersSpinner.getSelectedItem();
                        GarminForegroundSession garmin = new GarminForegroundSession(
                                user, state.users(), getActivity());
                        if (garmin.signIn()) {
                            String history = garmin.downloadHistory();
                            success = history != null;
                            if (success) {
                                List<Weight> wl = state.weights();
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

                                    oW.percentFat = (jsonobject.isNull("bodyFat")) ? -1 : jsonobject.getDouble("bodyFat");
                                    oW.percentHydration = (jsonobject.isNull("bodyWater")) ? -1 : jsonobject.getDouble("bodyWater");
                                    if (!jsonobject.isNull("metabolicAge")) {
                                        bodyMetabolicAge = jsonobject.getDouble("metabolicAge");
                                        double res = bodyMetabolicAge / 365250;
                                        res = res / 86400;
                                        oW.metabolicAge = (int) Math.round(res);
                                    }
                                    oW.visceralFatRating = (jsonobject.isNull("visceralFat")) ? -1 : jsonobject.getInt("visceralFat");
                                    oW.physiqueRating = (jsonobject.isNull("physiqueRating")) ? -1 : jsonobject.getInt("physiqueRating");
                                    oW.muscleMass = (jsonobject.isNull("muscleMass")) ? -1 : jsonobject.getInt("muscleMass") / 1000.0;
                                    oW.boneMass = (jsonobject.isNull("boneMass")) ? -1 : jsonobject.getInt("boneMass") / 1000.0;

                                    boolean repeated = false;
                                    //Comprobar que no sea repetida
                                    for (Weight w : wl) {
                                        //if (Debug.ON) Log.v(TAG, "Comparing " + w.date + " and " + lDate + " = " + (w.date - lDate));
                                        long delta = lDate - w.date;

                                        if (delta > 7200000L)//60*60*1000
                                        {
                                            //if (Debug.ON) Log.v(TAG, ".- Too old measurements IN=" + new Date(w.date) + " and NEW=" + new Date(lDate) + " = " + (w.date - lDate));
                                            break;
                                        } else {
                                            delta = Math.abs(delta);
                                            if ((w.uuid.equals(user.uuid)) && (delta <= 7200000L)) {
                                                repeated = (Math.abs(oW.weight - w.weight) < 0.05);
                                                int out = 1;
                                                if (repeated && (oW.percentFat != -1)) {
                                                    repeated = (Math.abs(oW.percentFat - w.percentFat) < 0.01);
                                                    out = 2;
                                                }
                                                if (repeated && (oW.percentHydration != -1)) {
                                                    repeated = (Math.abs(oW.percentHydration - w.percentHydration) < 0.01);
                                                    out = 3;
                                                }
                                                if (repeated && (oW.boneMass != -1)) {
                                                    repeated = (Math.abs(oW.boneMass - w.boneMass) < 0.01);
                                                    out = 4;
                                                }
                                                if (repeated && (oW.muscleMass != -1)) {
                                                    repeated = (Math.abs(oW.muscleMass - w.muscleMass) < 0.01);
                                                    out = 5;
                                                }
                                                if (repeated && (oW.physiqueRating != -1)) {
                                                    repeated = (oW.physiqueRating == w.physiqueRating);
                                                    out = 6;
                                                }
                                                if (repeated && (oW.percentFat != -1)) {
                                                    repeated = (Math.round(oW.visceralFatRating) == Math.round(w.visceralFatRating));
                                                    out = 7;
                                                }
                                                if (repeated && (oW.metabolicAge != -1)) {
                                                    repeated = (oW.metabolicAge - w.metabolicAge <= 1);
                                                    out = 8;
                                                }

                                                if (repeated) break;
                                                else if (Debug.ON) {
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
                                                    Log.v(TAG, "out=" + out);
                                                    Log.v(TAG, "r1=" + repeated + " " + Math.abs(oW.weight - w.weight));
                                                    Log.v(TAG, "r2=" + repeated + " " + Math.abs(oW.percentFat - w.percentFat));
                                                    Log.v(TAG, "r3=" + repeated + " " + Math.abs(oW.percentHydration - w.percentHydration));
                                                    Log.v(TAG, "r4=" + repeated + " " + Math.abs(oW.boneMass - w.boneMass));
                                                    Log.v(TAG, "r5=" + repeated + " " + Math.abs(oW.muscleMass - w.muscleMass));
                                                    Log.v(TAG, "r6=" + repeated + " " + oW.physiqueRating + " " + w.physiqueRating);
                                                    Log.v(TAG, "r7=" + repeated + " " + Math.abs(oW.visceralFatRating - w.visceralFatRating) + " " + oW.visceralFatRating + " " + w.visceralFatRating);
                                                    Log.v(TAG, "r8=" + repeated + " " + oW.metabolicAge + " " + w.metabolicAge);
                                                    Log.v(TAG, "r9=" + repeated + " " + Math.abs(oW.basalMet - w.basalMet) + " " + oW.basalMet + " " + w.basalMet);
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
                                if (Debug.ON)
                                    Log.v(TAG, "Total samples: " + jsonArray.length() + " adding " + tmp.size());
                                wl.addAll(tmp);
                                Collections.sort(wl, new Weight.DateComparator());
                                //Hacer que se muestre la nueva lista
                                getActivity().runOnUiThread(() -> mAdapter.replaceAll(state.selectedWeights(), user));

                                //Guardar el nuevo historial a disco
                                state.replaceWeights(wl);

                                notificationManager.cancel(GARMIN_CONNECT_NOTIFICATION_ID);
                                //success = true;
                            }
                        } else {
                            errorMessage = getString(
                                    R.string.weight_fragment_msg_wrong_credentials);
                        }
                        //gc.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload the selected history entries", e);
                        errorMessage = "Exception: " + e;
                    }
                    if (!success) {
                        // When the loop is finished, updates the notification
                        mBuilder.setContentText(errorMessage)
                                // Removes the progress bar
                                .setProgress(0, 0, false);
                        notificationManager.notify(GARMIN_CONNECT_NOTIFICATION_ID, mBuilder.build());
                    }
                }).start();
    }

}
