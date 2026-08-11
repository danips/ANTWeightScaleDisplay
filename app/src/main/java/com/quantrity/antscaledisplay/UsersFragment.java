package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.quantrity.antscaledisplay.databinding.FragmentUsersBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UsersFragment extends Fragment implements MenuProvider {
    private final static String TAG = "UsersFragment";

    private UsersAdapter mAdapter;
    private AppStateViewModel state;
    private FragmentUsersBinding binding;

    // Launcher for Database Backup (Directory Picker)
    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                    String displayName = "db_" + format1.format(cal.getTime()) + ".bin";
                    Uri uri = data.getData();
                    if (getActivity() != null && uri != null) {
                        state.createBackup(getActivity().getContentResolver(), uri, displayName);
                    }
                }
            }
    );

    // Launcher for Database Restore (File Picker)
    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri uri = data.getData();
                    if (getActivity() != null && uri != null) {
                        state.restoreBackup(getActivity().getContentResolver(), uri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        RecyclerView mRecyclerView = binding.usersRecyclerView;
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        if (getActivity() != null)
            mAdapter = new UsersAdapter(state.users(), getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        //Declare it has items for the actionbar
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        state.loadResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && mAdapter != null) {
                mAdapter.replaceAll(state.users());
                requireActivity().invalidateOptionsMenu();
            }
        });
        state.operationResult(AppStateViewModel.OperationKind.BACKUP)
                .observe(getViewLifecycleOwner(), this::onOperationResult);
        state.operationResult(AppStateViewModel.OperationKind.RESTORE)
                .observe(getViewLifecycleOwner(), this::onOperationResult);

        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        binding.usersRecyclerView.setAdapter(null);
        mAdapter = null;
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.fragment_users_menu, menu);
        if (mAdapter != null && mAdapter.getItemCount() == 0)
            menu.findItem(R.id.action_database_backup).setVisible(false);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_database_backup) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            backupLauncher.launch(intent);
            return true;
        } else if (itemId == R.id.action_database_restore) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            restoreLauncher.launch(intent);
            return true;
        } else if (itemId == R.id.action_adduser) {//Open the edit user fragment with values resetted
            if (getActivity() != null)
                AppHost.from(this).openEditUserFragment(null);
            return true;
        }
        return false;
    }

    void editUser(User user) {
        if (getActivity() != null)
            AppHost.from(this).openEditUserFragment(user);
    }

    void deleteUser(User user) {
        state.deleteUser(user, result -> {
            if (getActivity() == null || AppHost.from(this).handleMutationFailure(result)) return;
            GarminTokenRefreshScheduler.cancel(requireContext(), user);
            if (mAdapter != null) mAdapter.replaceAll(state.users());
            requireActivity().supportInvalidateOptionsMenu();
        });
    }

    private void onOperationResult(OperationEvent<AppStateViewModel.OperationResult> event) {
        AppStateViewModel.OperationResult pending = event.peek();
        if (pending.kind != AppStateViewModel.OperationKind.BACKUP
                && pending.kind != AppStateViewModel.OperationKind.RESTORE) return;
        AppStateViewModel.OperationResult completed = event.consume();
        if (completed == null || getActivity() == null) return;
        if (!completed.result.isSuccess()) {
            Log.e(TAG, completed.result.message, completed.result.error);
            Toast.makeText(getActivity(), completed.result.message, Toast.LENGTH_LONG).show();
            return;
        }
        if (completed.kind == AppStateViewModel.OperationKind.BACKUP) {
            Toast.makeText(getActivity(), String.format(
                    getString(R.string.history_fragment_action_database_backup_ok),
                    completed.displayName), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getActivity(), R.string.history_fragment_action_database_restore_ok,
                Toast.LENGTH_LONG).show();
        getActivity().invalidateOptionsMenu();
        if (mAdapter != null) mAdapter.replaceAll(state.users());
    }
}
