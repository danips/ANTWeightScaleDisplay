package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import java.util.Locale;

/** Compact color swatch which opens touch-friendly HSV controls when selected. */
public final class GoalColorPickerButton extends AppCompatButton {
    private static final int[] HUE_COLORS = {
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.BLUE, Color.MAGENTA, Color.RED
    };

    private int color = Color.RED;
    private AlertDialog dialog;

    public GoalColorPickerButton(Context context) {
        this(context, null);
    }

    public GoalColorPickerButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.buttonStyle);
    }

    public GoalColorPickerButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAllCaps(false);
        setGravity(Gravity.CENTER);
        setMinHeight(dp(48));
        setOnClickListener(view -> showPicker());
        renderColor();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        renderColor();
    }

    private void showPicker() {
        if (dialog != null && dialog.isShowing()) return;

        final int alpha = Color.alpha(color);
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(24);
        content.setPadding(horizontalPadding, dp(8), horizontalPadding, 0);

        TextView preview = new TextView(getContext());
        preview.setGravity(Gravity.CENTER);
        preview.setTextSize(18);
        content.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        TextView hueLabel = label(R.string.color_picker_hue);
        SeekBar hue = slider(359, Math.round(hsv[0]));
        TextView saturationLabel = label(R.string.color_picker_saturation);
        SeekBar saturation = slider(100, Math.round(hsv[1] * 100));
        TextView brightnessLabel = label(R.string.color_picker_brightness);
        SeekBar brightness = slider(100, Math.round(hsv[2] * 100));

        addControl(content, hueLabel, hue);
        addControl(content, saturationLabel, saturation);
        addControl(content, brightnessLabel, brightness);

        Runnable refresh = () -> {
            hsv[0] = hue.getProgress();
            hsv[1] = saturation.getProgress() / 100f;
            hsv[2] = brightness.getProgress() / 100f;
            int selected = Color.HSVToColor(alpha, hsv);
            styleSwatch(preview, selected);
            hue.setProgressDrawable(track(HUE_COLORS));
            saturation.setProgressDrawable(track(new int[]{
                    Color.HSVToColor(alpha, new float[]{hsv[0], 0, hsv[2]}),
                    Color.HSVToColor(alpha, new float[]{hsv[0], 1, hsv[2]})
            }));
            brightness.setProgressDrawable(track(new int[]{
                    Color.HSVToColor(alpha, new float[]{hsv[0], hsv[1], 0}),
                    Color.HSVToColor(alpha, new float[]{hsv[0], hsv[1], 1})
            }));
            hue.setContentDescription(getResources().getString(
                    R.string.color_picker_hue_value, hue.getProgress()));
            saturation.setContentDescription(getResources().getString(
                    R.string.color_picker_saturation_value, saturation.getProgress()));
            brightness.setContentDescription(getResources().getString(
                    R.string.color_picker_brightness_value, brightness.getProgress()));
        };

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                refresh.run();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        };
        hue.setOnSeekBarChangeListener(listener);
        saturation.setOnSeekBarChangeListener(listener);
        brightness.setOnSeekBarChangeListener(listener);
        refresh.run();

        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.color_picker_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (ignored, which) ->
                        setColor(Color.HSVToColor(alpha, hsv)))
                .create();
        dialog.setOnDismissListener(ignored -> dialog = null);
        // SeekBar does not reliably assign bounds to a custom progress drawable until its
        // first layout. Reapply the gradients once real bounds exist so they are visible before
        // the user touches a slider.
        content.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                content.removeOnLayoutChangeListener(this);
                refresh.run();
            }
        });
        dialog.show();
    }

    private TextView label(int textResource) {
        TextView label = new TextView(getContext());
        label.setText(textResource);
        label.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        return label;
    }

    private SeekBar slider(int max, int progress) {
        SeekBar slider = new SeekBar(getContext());
        slider.setMax(max);
        slider.setProgress(progress);
        slider.setSplitTrack(false);
        slider.setMinimumHeight(dp(48));
        return slider;
    }

    private void addControl(LinearLayout parent, TextView label, SeekBar slider) {
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(12);
        parent.addView(label, labelParams);
        parent.addView(slider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
    }

    private InsetDrawable track(int[] colors) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, colors);
        gradient.setCornerRadius(dp(6));
        gradient.setStroke(dp(1), 0x55000000);
        return new InsetDrawable(gradient, 0, dp(18), 0, dp(18));
    }

    private void renderColor() {
        String hex = hex(color);
        setText(hex);
        setTextColor(contrastingTextColor(color));
        setBackgroundTintList(ColorStateList.valueOf(color));
        setContentDescription(getResources().getString(
                R.string.color_picker_button_description, hex));
    }

    private void styleSwatch(TextView swatch, int selectedColor) {
        String hex = hex(selectedColor);
        swatch.setText(hex);
        swatch.setTextColor(contrastingTextColor(selectedColor));
        swatch.setContentDescription(getResources().getString(
                R.string.color_picker_button_description, hex));
        GradientDrawable background = new GradientDrawable();
        background.setColor(selectedColor);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), 0x66000000);
        swatch.setBackground(background);
    }

    private static String hex(int color) {
        if (Color.alpha(color) == 255) {
            return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
        }
        return String.format(Locale.US, "#%08X", color);
    }

    private static int contrastingTextColor(int background) {
        double brightness = Color.red(background) * 0.299
                + Color.green(background) * 0.587
                + Color.blue(background) * 0.114;
        return brightness >= 160 ? Color.BLACK : Color.WHITE;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.color = color;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.getSuperState());
        setColor(saved.color);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (dialog != null) dialog.dismiss();
        super.onDetachedFromWindow();
    }

    private static final class SavedState extends BaseSavedState {
        int color;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel source) {
            super(source);
            color = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel destination, int flags) {
            super.writeToParcel(destination, flags);
            destination.writeInt(color);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
