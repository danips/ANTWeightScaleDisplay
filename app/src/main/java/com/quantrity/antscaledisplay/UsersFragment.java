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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class UsersFragment extends Fragment implements MenuProvider {
    private final static String TAG = "UsersFragment";

    private static final int BUFFER_SIZE = 8192;

    private String dst;

    private UsersAdapter mAdapter;
    private AppStateViewModel state;

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
                    boolean ok = false;
                    Uri uri = data.getData();
                    if (getActivity() != null && uri != null) {
                        ok = UsersFragment.unzip(uri, getActivity().getFilesDir().toString(), getActivity().getContentResolver());
                    }

                    if (ok && getActivity() != null) {
                        Toast.makeText(getActivity(), getString(R.string.history_fragment_action_database_restore_ok), Toast.LENGTH_LONG).show();

                        //Recargar base de datos
                        ((MainActivity) getActivity()).reloadDB();
                        getActivity().invalidateOptionsMenu();
                        if (mAdapter != null) {
                            mAdapter.replaceAll(state.users());
                        }
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_users, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.users_recycler_view);
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

        return rootView;
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
                ((MainActivity) getActivity()).openEditUserFragment(null);
            return true;
        }
        return false;
    }

    void editUser(User user) {
        if (getActivity() != null)
            ((MainActivity) getActivity()).openEditUserFragment(user);
    }

    void deleteUser(User user) {
        GarminTokenRefreshScheduler.cancel(requireContext(), user);
        RepositoryResult<Void> result = state.deleteUser(user);
        if (!result.isSuccess()) Log.e(TAG, result.message, result.error);
    }

    private void saveBackup() {
        ZipOutputStream out;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dst)));
            saveBackup(out);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to create the backup archive", e);
        }
    }

    private void saveBackup(ParcelFileDescriptor destFileDesc) {
        FileOutputStream fileOutputStream = new FileOutputStream(destFileDesc.getFileDescriptor());
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        saveBackup(zipOutputStream);
    }

    private void saveBackup(ZipOutputStream out) {
        if (getActivity() == null) return;
        BufferedInputStream origin;
        out.setLevel(Deflater.BEST_COMPRESSION);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (File file : AppRepository.get(getActivity()).dataFiles()) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(file.getName());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Unable to add a file to the backup archive", e);
                } finally {
                    origin.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to save the backup archive", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close the backup archive", e);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), String.format(getString(R.string.history_fragment_action_database_backup_ok), dst), Toast.LENGTH_LONG).show()
                );
            }
        }
    }

    static boolean unzip(Uri zipFile, String location, ContentResolver contentResolver) {
        boolean ok = false;
        try {
            ZipInputStream zin = new ZipInputStream(contentResolver.openInputStream(zipFile));
            ok = unzip(zin, location);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open the backup archive", e);
        }
        return ok;
    }

    private static boolean unzip(ZipInputStream zin, String location) {
        boolean ok = false;
        int size;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            if (!location.endsWith("/")) {
                location += "/";
            }
            File f = new File(location);
            if (!f.isDirectory()) {
                boolean ignored = f.mkdirs();
            } else {
                File fdelete = new File(location + "users");
                if (fdelete.exists()) {
                    boolean ignored = fdelete.delete();
                }
                fdelete = new File(location + "history");
                if (fdelete.exists()) {
                    boolean ignored = fdelete.delete();
                }
                fdelete = new File(location + "goals");
                if (fdelete.exists()) {
                    boolean ignored = fdelete.delete();
                }
            }
            try {
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();
                    File unzipFile = new File(path);

                    if (ze.isDirectory()) {
                        if (!unzipFile.isDirectory()) {
                            boolean ignored = unzipFile.mkdirs();
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        File parentDir = unzipFile.getParentFile();
                        if (null != parentDir) {
                            if (!parentDir.isDirectory()) {
                                boolean ignored = parentDir.mkdirs();
                            }
                        }

                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ((size = zin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                fout.write(buffer, 0, size);
                            }

                            zin.closeEntry();
                            ok = true;
                        } finally {
                            fout.flush();
                            fout.close();
                        }
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            Log.v(TAG, "Unzip exception " + e.getMessage());
        }
        return ok;
    }
}
