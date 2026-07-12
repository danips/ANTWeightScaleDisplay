package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.quantrity.antscaledisplay.databinding.FragmentWeightBinding;
import com.quantrity.antscaledisplay.databinding.ItemMetricCardBinding;
import com.quantrity.antscaledisplay.databinding.ItemSegmentBinding;

import java.util.ArrayList;

public class WeightFragment extends Fragment implements MenuProvider, AntWeightListener {
    //private static final String TAG = "WeightFragment";

    private static final Metric[] CARD_METRICS = {
            Metric.PERCENTFAT, Metric.PERCENTHYDRATION, Metric.MUSCLEMASS, Metric.BONEMASS,
            Metric.VISCERALFATRATING, Metric.PHYSIQUERATING, Metric.METABOLICAGE,
            Metric.BASALMET
    };

    private FragmentWeightBinding binding;
    private boolean enableUploadButton = false;
    User userToUpload = null;
    private Spinner usersSpinner;
    private AppStateViewModel state;
    private AlertDialog antProgressDialog;
    private MeasurementPresentationFactory presentationFactory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWeightBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        presentationFactory = new MeasurementPresentationFactory(
                new MeasurementPresentationFactory.Strings() {
                    @Override public String get(int resourceId) {
                        return WeightFragment.this.getString(resourceId);
                    }

                    @Override public String format(int resourceId, Object... arguments) {
                        return WeightFragment.this.getString(resourceId, arguments);
                    }
                });
        View rootView = binding.getRoot();

        // 1. Initialize Segment Cards (Using ItemSegmentBinding)
        for (BodySegment segment : BodySegment.values()) {
            setupSegment(segmentBinding(segment), segment.iconRes, getString(segment.labelRes));
        }

        // 2. Initialize Grid Cards (Using ItemMetricCardBinding)
        for (Metric metric : CARD_METRICS) setupCard(cardBinding(metric), metric);

        binding.fab.setOnClickListener(view -> {
            MainActivity ma = (MainActivity)getActivity();
            if (ma != null) {
                AntWeightController rw = ma.getAntWeightController();
                if (rw != null) {
                    ma.openEditWeightFragment(rw.weight, rw.user, true);
                } else {
                    ma.openEditWeightFragment(null, null, false);
                }
            }
        });

        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && activity.getAntWeightController() != null) {
            activity.getAntWeightController().detachListener(this);
        }
        dismissAntProgress();
        super.onDestroyView();
        presentationFactory = null;
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && activity.getAntWeightController() != null) {
            AntWeightController controller = activity.getAntWeightController();
            controller.attachListener(this);
            if (controller.isRunning()) onAntProgress(controller.progress());
        }
        updateUi();
    }

    void updateUi() {
        if (getActivity() == null || binding == null) return;

        final MainActivity mainActivity = (MainActivity) getActivity();
        final AntWeightController rw = mainActivity.getAntWeightController();

        mainActivity.runOnUiThread(() -> {
            if (getActivity() == null || binding == null) return;

            Weight displayWeight = null;
            User displayUser = state.selectedUser();

            // 1. Live Data or 2. History
            if (rw != null && rw.weight != null && rw.weight.weight != -1 && rw.user != null) {
                displayWeight = rw.weight;
                displayUser = rw.user;
            } else if (displayUser != null) {
                ArrayList<Weight> history = state.weights();
                if (history != null && !history.isEmpty()) {
                    for (Weight w : history) {
                        if (w.uuid.equals(displayUser.uuid)) {
                            displayWeight = w;
                            break;
                        }
                    }
                }
            }

            if (displayWeight == null) {
                resetAllCards();
                return;
            }

            // Trend
            ArrayList<Weight> history = state.weights();
            Weight lastWeight = null;
            if (history != null) {
                for (Weight w : history) {
                    if (w.uuid.equals(displayUser.uuid) && (w.date != displayWeight.date)) {
                        if (lastWeight == null || lastWeight.date < w.date) lastWeight = w;
                    }
                }
            }

            MeasurementPresentationFactory.MetricDisplay weightDisplay =
                    presentationFactory.metric(Metric.WEIGHT, displayUser, displayWeight,
                            lastWeight, displayUser.age, displayUser.isMale);
            MeasurementPresentationFactory.MetricDisplay bmiDisplay =
                    presentationFactory.bmi(displayUser, displayWeight, displayUser.height_cm,
                            displayUser.age, displayUser.isMale);
            binding.weightTV.setText(weightDisplay.primaryText);
            binding.bmiTV.setText(bmiDisplay.available ? bmiDisplay.primaryText : "-");
            updateBMIStatus(binding.iconWeight, binding.bmiDescTV, bmiDisplay);
            updateTrend(binding.weightIVmini, weightDisplay);

            // Segmental Analysis (Uses helper method for ItemSegmentBinding)
            boolean hasData = false;
            for (BodySegment segment : BodySegment.values()) {
                MeasurementPresentationFactory.SegmentDisplay display =
                        presentationFactory.segment(segment, displayUser, displayWeight, lastWeight);
                hasData |= display.currentValue != -1;
                updateSegmentUI(segmentBinding(segment), display);
            }
            binding.segmentalData.setVisibility(hasData ? View.VISIBLE : View.GONE);

            for (Metric metric : CARD_METRICS) {
                updateCardData(cardBinding(metric), presentationFactory.metric(
                        metric, displayUser, displayWeight, lastWeight,
                        displayUser.age, displayUser.isMale));
            }

            if ((rw != null) && (displayUser.gc_user != null) && (displayUser.gc_pass != null)) {
                if ((userToUpload != null) && (!enableUploadButton) && (displayUser.autoupload)) {
                    MainActivity.uploadButton(mainActivity, rw.weight, userToUpload);
                }
                enableUploadButton = true;
                userToUpload = displayUser;
            } else {
                enableUploadButton = false;
            }
            getActivity().invalidateOptionsMenu();
        });
    }

    // --- Helpers for ItemSegmentBinding ---
    private void setupSegment(ItemSegmentBinding seg, int iconRes, String title) {
        seg.metricTitle.setText(title);
        seg.metricIcon.setImageResource(iconRes);
    }

    private ItemSegmentBinding segmentBinding(BodySegment segment) {
        switch (segment) {
            case TRUNK: return binding.segTrunk;
            case LEFT_ARM: return binding.segLeftArm;
            case RIGHT_ARM: return binding.segRightArm;
            case LEFT_LEG: return binding.segLeftLeg;
            case RIGHT_LEG: return binding.segRightLeg;
            default: throw new IllegalArgumentException("Unknown segment " + segment);
        }
    }

    private void updateSegmentUI(ItemSegmentBinding seg,
                                 MeasurementPresentationFactory.SegmentDisplay display) {
        if (display.available) {
            seg.metricValue.setText(display.primaryText);
            if (!display.secondaryText.isEmpty()) {
                seg.metricSubValue.setText(display.secondaryText);
                seg.metricSubValue.setVisibility(View.VISIBLE);
            } else {
                seg.metricSubValue.setVisibility(View.GONE);
            }
            // Segments are Blue by default
            seg.metricIcon.setBackgroundResource(R.drawable.rounded_blue);

            updateTrend(seg.metricTrend, display.currentValue, display.previousValue);
        } else {
            seg.metricValue.setText("-");
            seg.metricSubValue.setVisibility(View.GONE);
            seg.metricIcon.setBackgroundResource(R.drawable.rounded_grey);
            seg.metricTrend.setVisibility(View.INVISIBLE);
        }
    }

    private void resetSegment(ItemSegmentBinding seg) {
        seg.metricValue.setText("-");
        seg.metricSubValue.setVisibility(View.GONE);
        seg.metricIcon.setBackgroundResource(R.drawable.rounded_grey);
        seg.metricTrend.setVisibility(View.INVISIBLE);
    }

    // --- Helpers for ItemMetricCardBinding (Grid) ---
    private void setupCard(ItemMetricCardBinding card, Metric metric) {
        card.metricTitle.setText(metric.getLabelRes());
        card.metricIcon.setImageResource(metric.getIconRes());
    }

    private ItemMetricCardBinding cardBinding(Metric metric) {
        switch (metric) {
            case PERCENTFAT: return binding.cardFat;
            case PERCENTHYDRATION: return binding.cardWater;
            case MUSCLEMASS: return binding.cardMuscle;
            case BONEMASS: return binding.cardBone;
            case VISCERALFATRATING: return binding.cardVisceral;
            case PHYSIQUERATING: return binding.cardPhysique;
            case METABOLICAGE: return binding.cardMetabolicAge;
            case BASALMET: return binding.cardBMR;
            default: throw new IllegalArgumentException("Unsupported card metric " + metric);
        }
    }

    private void updateCardData(ItemMetricCardBinding card,
                                MeasurementPresentationFactory.MetricDisplay display) {
        card.metricValue.setText(display.available ? display.primaryText : "-");
        if (!display.secondaryText.isEmpty()) {
            card.metricSubValue.setText(display.secondaryText);
            card.metricSubValue.setVisibility(View.VISIBLE);
        } else {
            card.metricSubValue.setVisibility(View.GONE);
        }
        updateIconBackground(card.metricIcon, display.status);
        updateTrend(card.metricTrend, display.currentValue, display.previousValue);
        card.metricCard.setVisibility(display.available ? View.VISIBLE : View.GONE);
    }

    private void resetCard(ItemMetricCardBinding card) {
        card.metricValue.setText("-");
        card.metricSubValue.setVisibility(View.GONE);
        card.metricIcon.setBackgroundResource(R.drawable.rounded_grey);
        card.metricTrend.setVisibility(View.INVISIBLE);
        card.metricCard.setVisibility(View.VISIBLE);
    }

    private void resetAllCards() {
        binding.weightTV.setText("-");
        binding.bmiTV.setText("-");
        binding.bmiDescTV.setText("");
        binding.weightIVmini.setVisibility(View.INVISIBLE);
        binding.iconWeight.setBackgroundResource(R.drawable.rounded_blue);

        for (BodySegment segment : BodySegment.values()) resetSegment(segmentBinding(segment));
        for (Metric metric : CARD_METRICS) resetCard(cardBinding(metric));
    }

    private void updateIconBackground(ImageView iv, MeasurementPresentationFactory.Status status) {
        if (status == MeasurementPresentationFactory.Status.HEALTHY) {
            iv.setBackgroundResource(R.drawable.rounded_green);
        } else if (status == MeasurementPresentationFactory.Status.DANGER) {
            iv.setBackgroundResource(R.drawable.rounded_red);
        } else if (status == MeasurementPresentationFactory.Status.WARNING) {
            iv.setBackgroundResource(R.drawable.rounded_yellow);
        }
        else iv.setBackgroundResource(R.drawable.rounded_blue);
    }

    private void updateBMIStatus(ImageView icon, TextView description,
                                 MeasurementPresentationFactory.MetricDisplay display) {
        updateIconBackground(icon, display.status);
        description.setText(display.secondaryText);
    }

    private void updateTrend(ImageView view,
                             MeasurementPresentationFactory.MetricDisplay display) {
        updateTrend(view, display.currentValue, display.previousValue);
    }

    private void updateTrend(ImageView view, double current, double previous) {
        if (current != -1 && previous != -1) updateTrendIcon(current, previous, view);
        else view.setVisibility(View.INVISIBLE);
    }

    private void updateTrendIcon(double now, double last, ImageView iv) {
        if ((now <= last + 0.05) && (now >= last - 0.05)) {
            iv.setImageResource(R.drawable.ic_equal);
        } else if (now > last) {
            iv.setImageResource(R.drawable.ic_more);
        } else {
            iv.setImageResource(R.drawable.ic_less);
        }
        iv.setVisibility(View.VISIBLE);
    }

    // --- Menu ---
    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.fragment_weight_menu, menu);
        if (getActivity() != null)
            usersSpinner = ((MainActivity)getActivity()).addUsersSpinner(menu, oisListener);
        if (!enableUploadButton) {
            MenuItem upload = menu.findItem(R.id.action_weight_upload);
            upload.setVisible(false);
        }
    }

    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (view != null && getActivity() != null) {
                state.selectUser((User)adapterView.getItemAtPosition(i));
                updateUi();
            }
        }
        @Override public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.action_weight) {
            User user = null;
            if (usersSpinner != null) user = (User)usersSpinner.getSelectedItem();
            if ((user == null) && (getActivity() != null)) {
                ((MainActivity)getActivity()).openEditUserFragment(null);
                return true;
            }
            if (getActivity() != null) {
                AntWeightController rw =
                        ((MainActivity) getActivity()).startAntWeightMeasurement(this);
                rw.setProfile(user);
                rw.start();
                enableUploadButton = false;
                userToUpload = null;
                getActivity().invalidateOptionsMenu();
            }
            return true;
        } else if (itemId == R.id.action_weight_upload) {
            if (getActivity() != null) {
                if (((MainActivity)getActivity()).getAntWeightController() != null) {
                    MainActivity.uploadButton((MainActivity)getActivity(),
                            ((MainActivity)getActivity()).getAntWeightController().weight,
                            userToUpload);
                }
            }
            enableUploadButton = false;
            if (getActivity() != null) getActivity().invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    @Override
    public void onAntProgress(AntWeightSession.Progress progress) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            int message = progress == AntWeightSession.Progress.FOUND
                    ? R.string.weight_fragment_msg_found
                    : progress == AntWeightSession.Progress.WAITING
                    ? R.string.weight_fragment_msg_waiting
                    : R.string.weight_fragment_msg_searching;
            if (antProgressDialog == null) {
                antProgressDialog = new AlertDialog.Builder(requireContext())
                        .setMessage(message)
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            MainActivity activity = (MainActivity) getActivity();
                            if (activity != null && activity.getAntWeightController() != null) {
                                activity.getAntWeightController().cancel();
                            }
                        })
                        .setCancelable(false)
                        .create();
                antProgressDialog.show();
            } else {
                antProgressDialog.setMessage(getString(message));
            }
        });
    }

    @Override
    public void onAntSuccess(Weight weight, User user) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            dismissAntProgress();
            userToUpload = user;
            updateUi();
        });
    }

    @Override
    public void onAntFailure(AntWeightSession.Failure failure, String detail) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            dismissAntProgress();
            userToUpload = null;
            enableUploadButton = false;
            updateUi();
            if (failure == AntWeightSession.Failure.CANCELLED) return;
            int message = failureMessage(failure);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.weight_process_msg_error)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(detail == null ? getString(message)
                            : getString(R.string.weight_process_msg_problem_while, detail));
            if (failure == AntWeightSession.Failure.PERMISSION) {
                builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:com.dsi.ant.service.socket"));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }).setCancelable(false);
            } else {
                builder.setPositiveButton(android.R.string.ok, null);
            }
            builder.show();
        });
    }

    @Override
    public void onAntPersistenceFailure(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            dismissAntProgress();
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.showMessage(
                    getString(R.string.repository_save_error, message));
        });
    }

    private int failureMessage(AntWeightSession.Failure failure) {
        switch (failure) {
            case BIND: return R.string.weight_process_msg_problem_bind;
            case PERMISSION: return R.string.msg_problem_ant_permission_disabled;
            case WEIGHT_TIMEOUT: return R.string.weight_process_msg_problem_timeout_weight;
            case MEASUREMENT_TIMEOUT: return R.string.weight_process_msg_problem_timeout_measurements;
            case SCALE_NOT_READY: return R.string.weight_process_msg_problem_scale_not_ready;
            case NOT_BAREFOOT: return R.string.weight_process_msg_problem_not_barefoot;
            default: return R.string.weight_process_msg_problem_timeout;
        }
    }

    private void dismissAntProgress() {
        if (antProgressDialog != null) antProgressDialog.dismiss();
        antProgressDialog = null;
    }
}
