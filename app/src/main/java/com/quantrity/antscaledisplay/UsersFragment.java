package com.quantrity.antscaledisplay;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

public class UsersFragment extends Fragment {
    private final static String TAG = "UsersFragment";

    private static final int BUFFER_SIZE = 8192;

    private String dst;

    private UsersAdapter mAdapter;

    public UsersFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_users, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.users_recycler_view);
	    // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        if (getActivity() != null)
            mAdapter = new UsersAdapter(((MainActivity)getActivity()).getUsersArray(), getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_users_menu, menu);
        if (mAdapter.getItemCount() == 0) menu.findItem(R.id.action_database_backup).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        ExFilePicker exFilePicker;
        int itemId = item.getItemId();
        if (itemId == R.id.action_database_backup) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, MainActivity.DIRECTORY_PICKER_RESULT);
            } else {
                exFilePicker = new ExFilePicker();
                exFilePicker.setCanChooseOnlyOneItem(true);
                exFilePicker.setSortButtonDisabled(true);
                exFilePicker.setChoiceType(ExFilePicker.ChoiceType.DIRECTORIES);
                exFilePicker.start(this, MainActivity.DIRECTORY_PICKER_RESULT);
            }
            return true;
        } else if (itemId == R.id.action_database_restore) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, MainActivity.FILE_PICKER_RESULT);
            } else {
                exFilePicker = new ExFilePicker();
                exFilePicker.setCanChooseOnlyOneItem(true);
                exFilePicker.setSortButtonDisabled(true);
                exFilePicker.setChoiceType(ExFilePicker.ChoiceType.FILES);
                exFilePicker.start(this, MainActivity.FILE_PICKER_RESULT);
            }
            return true;
        } else if (itemId == R.id.action_adduser) {//Open the edit user fragment with values resetted
            if (getActivity() != null)
                ((MainActivity) getActivity()).openEditUserFragment(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void editUser(User user)
    {
        if (getActivity() != null)
            ((MainActivity)getActivity()).openEditUserFragment(user);
    }

    private void saveBackup()
    {
        ZipOutputStream out;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dst)));
            saveBackup(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveBackup(ParcelFileDescriptor destFileDesc)
    {
        FileOutputStream fileOutputStream = new FileOutputStream(destFileDesc.getFileDescriptor());
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        saveBackup(zipOutputStream);
    }

    private void saveBackup(ZipOutputStream out)
    {
        if (getActivity() == null) return;
        BufferedInputStream origin;
        out.setLevel(Deflater.BEST_COMPRESSION);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            String[] files = {
                    User.usersFilePath(getActivity()), Weight.historyFilePath(getActivity()), Goal.goalsFilePath(getActivity())
            };
            for (String file : files) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(buffer, 0, BUFFER_SIZE)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    origin.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getActivity(), String.format(getString(R.string.history_fragment_action_database_backup_ok), dst), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.DIRECTORY_PICKER_RESULT) {
            if ((resultCode == Activity.RESULT_OK) && (data != null)) {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                String displayName = "db_" + format1.format(cal.getTime()) + ".bin";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Uri uri = data.getData();
                    ContentResolver contentResolver;
                    if ((getActivity() != null) && ((contentResolver = getActivity().getContentResolver()) != null)) {
                        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                        try {
                            Uri fileUri = DocumentsContract.createDocument(contentResolver, docUri, "application/octet-stream", displayName);
                            ParcelFileDescriptor destFileDesc = contentResolver.openFileDescriptor(fileUri, "w", null);
                            if (destFileDesc != null) {
                                dst = displayName;
                                saveBackup(destFileDesc);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
                    if ((result != null) && (result.getCount() > 0) && (getActivity() != null)) {
                        dst = result.getPath() + result.getNames().get(0) + File.separator + displayName;
                        if (Debug.ON) Log.v(TAG, "Destination backup: " + dst);

                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            saveBackup();
                        } else {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                        }
                    }
                }
            }
        } else if (requestCode == MainActivity.FILE_PICKER_RESULT) {
            boolean ok = false;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (resultCode == Activity.RESULT_OK)
                {
                    Uri uri;
                    if (data != null) {
                        uri = data.getData();
                        if (getActivity() != null)
                        {
                            ok = UsersFragment.unzip(uri, getActivity().getFilesDir().toString(), getActivity().getContentResolver());
                        }
                    }
                }
            }
            else {
                ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
                if ((result != null) && (result.getCount() > 0) && (getActivity() != null)) {
                    String file = result.getPath() + result.getNames().get(0);
                    ok = UsersFragment.unzip(file, getActivity().getFilesDir().toString());
                }
            }
            if (ok)
            {
                Toast.makeText(getActivity(), getString(R.string.history_fragment_action_database_restore_ok), Toast.LENGTH_LONG).show();

                //Recargar base de datos
                ((MainActivity)getActivity()).reloadDB();
                getActivity().invalidateOptionsMenu();
                mAdapter.notifyDataSetChanged();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MainActivity.REQUEST_CODE_WRITE_EXTERNAL_STORAGE)
        {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
            {
                saveBackup();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Unzip a zip file.  Will overwrite existing files.
     *
     * @param zipFile Full path of the zip file you'd like to unzip.
     * @param location Full path of the directory you'd like to unzip to (will be created if it doesn't exist).
     */
    static boolean unzip(String zipFile, String location) {
        boolean ok = false;
        try {
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
            ok = unzip(zin, location);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return ok;
    }

    static boolean unzip(Uri zipFile, String location, ContentResolver contentResolver) {
        boolean ok = false;
        try {
            ZipInputStream zin = new ZipInputStream(contentResolver.openInputStream(zipFile));
            ok = unzip(zin, location);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ok;
    }

    private static boolean unzip(ZipInputStream zin, String location) {
        boolean ok = false;
        int size;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            if ( !location.endsWith("/") ) {
                location += "/";
            }
            File f = new File(location);
            if(!f.isDirectory()) {
                f.mkdirs();
            }
            else
            {
                File fdelete = new File(location + "users");
                if (fdelete.exists()) {
                    fdelete.delete();
                }
                fdelete = new File(location + "history");
                if (fdelete.exists()) {
                    fdelete.delete();
                }
                fdelete = new File(location + "goals");
                if (fdelete.exists()) {
                    fdelete.delete();
                }
            }
            try {
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();
                    File unzipFile = new File(path);

                    if (ze.isDirectory()) {
                        if(!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        File parentDir = unzipFile.getParentFile();
                        if ( null != parentDir ) {
                            if ( !parentDir.isDirectory() ) {
                                parentDir.mkdirs();
                            }
                        }

                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
                                fout.write(buffer, 0, size);
                            }

                            zin.closeEntry();
                            ok = true;
                        }
                        finally {
                            fout.flush();
                            fout.close();
                        }
                    }
                }
            }
            finally {
                zin.close();
            }
        }
        catch (Exception e) {
            Log.v(TAG, "Unzip exception " + e.getMessage());
        }
        return ok;
    }
}
