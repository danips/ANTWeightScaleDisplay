package com.quantrity.antscaledisplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.quantrity.antscaledisplay.databinding.DialogLayoutBinding;
import com.quantrity.antscaledisplay.databinding.ItemsViewBinding;

import java.util.ArrayList;

/**
 * Created by Md Farhan Raja on 2/23/2017.
 */

public class SpinnerDialog {
    final ArrayList<String> items;
    final Activity context;
    final String dTitle;
    OnSpinerItemClick onSpinerItemClick;
    AlertDialog alertDialog;
    int pos;
    int style;
    boolean cancellable=false;
    boolean showKeyboard=false;
    final boolean useContainsFilter=false;


    public SpinnerDialog(Activity activity, ArrayList<String> items, String dialogTitle) {
        this.items = items;
        this.context = activity;
        this.dTitle = dialogTitle;
    }

    void bindOnSpinnerListener(OnSpinerItemClick onSpinnerItemClick1) {
        this.onSpinerItemClick = onSpinnerItemClick1;
    }

    void showSpinnerDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        DialogLayoutBinding binding = DialogLayoutBinding.inflate(context.getLayoutInflater());
        binding.spinerTitle.setText(dTitle);
        final ListView listView = binding.list;
        final EditText searchBox = binding.searchBox;
        if(isShowKeyboard()){
            showKeyboard(searchBox);
        }
        final ArrayAdapterWithContainsFilter adapter = new ArrayAdapterWithContainsFilter(context, R.layout.items_view, items);
        listView.setAdapter(adapter);
        adb.setView(binding.getRoot());
        alertDialog = adb.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().getAttributes().windowAnimations = style;
        }

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            TextView t = ItemsViewBinding.bind(view).text1;
            for (int j = 0; j < items.size(); j++) {
                if (t.getText().toString().equalsIgnoreCase(items.get(j))) {
                    pos = j;
                }
            }
            onSpinerItemClick.onClick(t.getText().toString(), pos);
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
                if(isUseContainsFilter()){
                    adapter.getContainsFilter(searchBox.getText().toString());
                } else {
                    adapter.getFilter().filter(searchBox.getText().toString());
                }
            }
        });

        alertDialog.setCancelable(isCancellable());
        alertDialog.setCanceledOnTouchOutside(isCancellable());
        alertDialog.show();
    }

    public void closeSpinnerDialog() {
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

    private boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    private boolean isShowKeyboard() {
        return showKeyboard;
    }

    private boolean isUseContainsFilter() {
        return useContainsFilter;
    }


    public void setShowKeyboard(boolean showKeyboard) {
        this.showKeyboard = showKeyboard;
    }
}
