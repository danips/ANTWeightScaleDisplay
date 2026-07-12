package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.quantrity.antscaledisplay.databinding.DialogLayoutBinding;
import com.quantrity.antscaledisplay.databinding.ItemsViewBinding;

import java.util.ArrayList;

final class SpinnerDialog {
    private final ArrayList<String> items;
    private final Activity context;
    private final String title;
    private OnSpinnerItemClick onSpinnerItemClick;
    private AlertDialog alertDialog;
    private boolean cancellable;
    private boolean showKeyboard;


    public SpinnerDialog(Activity activity, ArrayList<String> items, String dialogTitle) {
        this.items = items;
        this.context = activity;
        this.title = dialogTitle;
    }

    void bindOnSpinnerListener(OnSpinnerItemClick listener) {
        this.onSpinnerItemClick = listener;
    }

    void showSpinnerDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        DialogLayoutBinding binding = DialogLayoutBinding.inflate(context.getLayoutInflater());
        binding.spinerTitle.setText(title);
        final ListView listView = binding.list;
        final EditText searchBox = binding.searchBox;
        if (showKeyboard) showKeyboard(searchBox);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.items_view, items);
        listView.setAdapter(adapter);
        adb.setView(binding.getRoot());
        alertDialog = adb.create();
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            TextView t = ItemsViewBinding.bind(view).text1;
            int position = 0;
            for (int j = 0; j < items.size(); j++) {
                if (t.getText().toString().equalsIgnoreCase(items.get(j))) {
                    position = j;
                }
            }
            onSpinnerItemClick.onClick(t.getText().toString(), position);
            closeSpinnerDialog();
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                adapter.getFilter().filter(searchBox.getText().toString());
            }
        });

        alertDialog.setCancelable(cancellable);
        alertDialog.setCanceledOnTouchOutside(cancellable);
        alertDialog.show();
    }

    void closeSpinnerDialog() {
        hideKeyboard();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    private void hideKeyboard(){
        KeyboardUtils.hide(context);
    }

    private void showKeyboard(final EditText et){
        et.requestFocus();
        et.postDelayed(() -> {
            InputMethodManager keyboard=(InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(et,0);
        }
                ,200);
    }

    void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    void setShowKeyboard(boolean showKeyboard) {
        this.showKeyboard = showKeyboard;
    }
}
