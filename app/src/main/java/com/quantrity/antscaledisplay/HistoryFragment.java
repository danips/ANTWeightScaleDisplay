package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quantrity.antscaledisplay.databinding.FragmentHistoryBinding;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment implements MenuProvider,
        GarminHistoryDownloadCoordinator.Listener {
    private final static String TAG = "HistoryFragment";
    HistoryAdapter mAdapter;

    //MenuItems for controlling the Download button
    private MenuItem downloadMI = null;
    private MenuItem gcMI = null;
    private Spinner usersSpinner = null;
    private RecyclerView mRecyclerView = null;
    private AppStateViewModel state;
    private FragmentHistoryBinding binding;
    private GarminHistoryDownloadCoordinator historyDownload;

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

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        historyDownload = new GarminHistoryDownloadCoordinator(requireActivity(), this);
        getViewLifecycleOwner().getLifecycle().addObserver(historyDownload);
    }

    @Override public void onDestroyView() {
        if (historyDownload != null) historyDownload.close();
        historyDownload = null;
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
            downloadGarminHistory();
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

    private void downloadGarminHistory() {
        if (historyDownload == null || historyDownload.isRunning()) return;
        User user = state.selectedUser();
        historyDownload.start(user, state.users(), state.weights());
    }

    @Override
    public void onHistoryImported(ArrayList<Weight> weights, int added) {
        state.replaceWeights(weights);
        User selectedUser = state.selectedUser();
        if (mAdapter != null && selectedUser != null) {
            mAdapter.replaceAll(state.selectedWeights(), selectedUser);
        }
    }

    @Override
    public void onHistoryDownloadFailed(String message) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.showMessage(message);
    }

}
