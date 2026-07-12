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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UsersFragment extends Fragment implements MenuProvider {
    private final static String TAG = "UsersFragment";

    private String dst;

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
                    ContentResolver contentResolver;
                    if ((getActivity() != null) && ((contentResolver = getActivity().getContentResolver()) != null) && uri != null) {
                        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                        try {
                            Uri fileUri = DocumentsContract.createDocument(contentResolver, docUri, "application/octet-stream", displayName);
                            if (fileUri != null) {
                                ParcelFileDescriptor destFileDesc = contentResolver.openFileDescriptor(fileUri, "w", null);
                                if (destFileDesc != null) {
                                    dst = displayName;
                                    saveBackup(destFileDesc);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "Unable to open the backup destination", e);
                        }
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
                        restoreBackup(uri);
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
        GarminTokenRefreshScheduler.cancel(requireContext(), user);
        state.deleteUser(user, result -> {
            if (getActivity() != null) AppHost.from(this).handleMutationFailure(result);
        });
    }

    private void saveBackup(ParcelFileDescriptor destFileDesc) {
        if (getActivity() == null) return;
        File directory = getActivity().getFilesDir();
        new Thread(() -> {
            RepositoryResult<Integer> result = BackupArchive.create(
                    new FileOutputStream(destFileDesc.getFileDescriptor()), directory);
            if (!result.isSuccess()) Log.e(TAG, result.message, result.error);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (result.isSuccess()) Toast.makeText(getActivity(), String.format(
                        getString(R.string.history_fragment_action_database_backup_ok), dst),
                        Toast.LENGTH_LONG).show();
                else Toast.makeText(getActivity(), result.message, Toast.LENGTH_LONG).show();
            });
        }, "backup-archive-create").start();
    }

    private void restoreBackup(Uri uri) {
        if (getActivity() == null) return;
        ContentResolver resolver = getActivity().getContentResolver();
        File directory = getActivity().getFilesDir();
        new Thread(() -> {
            RepositoryResult<Integer> result;
            try {
                InputStream input = resolver.openInputStream(uri);
                result = BackupArchive.restore(input, directory);
            } catch (IOException exception) {
                result = RepositoryResult.failure("Unable to open the backup archive", exception);
            }
            if (!result.isSuccess()) Log.e(TAG, result.message, result.error);
            RepositoryResult<Integer> completed = result;
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (!completed.isSuccess()) {
                    Toast.makeText(getActivity(), completed.message, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(getActivity(),
                        R.string.history_fragment_action_database_restore_ok,
                        Toast.LENGTH_LONG).show();
                AppHost.from(this).reloadDB();
                getActivity().invalidateOptionsMenu();
                if (mAdapter != null) mAdapter.replaceAll(state.users());
            });
        }, "backup-archive-restore").start();
    }
}
