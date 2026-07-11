package com.quantrity.antscaledisplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

final class KeyboardUtils {
    private KeyboardUtils() {}

    static void hide(Activity activity) {
        View focusedView = activity.getCurrentFocus();
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (focusedView != null && inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    static void dismissOnTouchOutsideInputs(View view, Activity activity) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((ignored, event) -> {
                hide(activity);
                return false;
            });
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                dismissOnTouchOutsideInputs(group.getChildAt(index), activity);
            }
        }
    }
}
