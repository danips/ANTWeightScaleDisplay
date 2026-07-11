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
import java.util.Locale;

public class WeightFragment extends Fragment implements MenuProvider, AntWeightListener {
    //private static final String TAG = "WeightFragment";

    private FragmentWeightBinding binding;
    private boolean enableUploadButton = false;
    User userToUpload = null;
    private Spinner usersSpinner;
    private AppStateViewModel state;
    private AlertDialog antProgressDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWeightBinding.inflate(inflater, container, false);
        state = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        View rootView = binding.getRoot();

        // 1. Initialize Segment Cards (Using ItemSegmentBinding)
        for (BodySegment segment : BodySegment.values()) {
            setupSegment(segmentBinding(segment), segment.iconRes, getString(segment.labelRes));
        }

        // 2. Initialize Grid Cards (Using ItemMetricCardBinding)
        setupCard(binding.cardFat, Metric.PERCENTFAT);
        setupCard(binding.cardWater, Metric.PERCENTHYDRATION);
        setupCard(binding.cardMuscle, Metric.MUSCLEMASS);
        setupCard(binding.cardBone, Metric.BONEMASS);
        setupCard(binding.cardVisceral, Metric.VISCERALFATRATING);
        setupCard(binding.cardPhysique, Metric.PHYSIQUERATING);
        setupCard(binding.cardMetabolicAge, Metric.METABOLICAGE);
        setupCard(binding.cardBMR, Metric.BASALMET);

        binding.fab.setOnClickListener(view -> {
            MainActivity ma = (MainActivity)getActivity();
            if (ma != null) {
                AntWeightController rw = ma.getRequestWeight();
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
        if (activity != null && activity.getRequestWeight() != null) {
            activity.getRequestWeight().detachListener(this);
        }
        dismissAntProgress();
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && activity.getRequestWeight() != null) {
            AntWeightController controller = activity.getRequestWeight();
            controller.attachListener(this);
            if (controller.isRunning()) onAntProgress(controller.progress());
        }
        updateUi();
    }

    void updateUi() {
        if (getActivity() == null || binding == null) return;

        final MainActivity mainActivity = (MainActivity) getActivity();
        final AntWeightController rw = mainActivity.getRequestWeight();

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

            // Main Weight
            binding.weightTV.setText(displayUser.printMass(getContext(), displayWeight.weight));
            float h = (float) displayUser.height_cm;
            float bmi = (float) (displayWeight.weight / Math.pow(h / 100, 2));
            binding.bmiTV.setText(String.format(Locale.getDefault(), "%.2f", bmi));

            int bmiStatus = RequestWeight.getBMIDesc((byte)displayUser.age, bmi, displayUser.isMale);
            updateBMIStatus(binding.iconWeight, binding.bmiDescTV, bmiStatus);

            if (lastWeight != null) {
                updateTrendIcon(displayWeight.weight, lastWeight.weight, binding.weightIVmini);
            }

            // Segmental Analysis (Uses helper method for ItemSegmentBinding)
            boolean hasData = false;
            for (BodySegment segment : BodySegment.values()) {
                double percent = segment.fatMetric.value(displayWeight);
                double muscle = segment.muscleMetric.value(displayWeight);
                double current = displayUser.show_fat_mass ? muscle : percent;
                double previous = lastWeight == null ? -1
                        : (displayUser.show_fat_mass
                        ? segment.muscleMetric.value(lastWeight) : segment.fatMetric.value(lastWeight));
                hasData |= current != -1;
                updateSegmentUI(segmentBinding(segment), percent, muscle, displayWeight,
                        displayUser, previous, current);
            }
            binding.segmentalData.setVisibility(hasData ? View.VISIBLE : View.GONE);

            // Metric Grid Cards (Uses helper method for ItemMetricCardBinding)

            // Fat
            double fatMass = displayWeight.weight * displayWeight.percentFat / 100;
            String fatMain, fatSub;
            if (displayUser.show_fat_mass) {
                fatMain = displayUser.printMass(getContext(), fatMass);
                fatSub = String.format(getString(R.string.weight_fragment_percent_tag), displayWeight.percentFat);
            } else {
                fatMain = String.format(getString(R.string.weight_fragment_percent_tag), displayWeight.percentFat);
                fatSub = displayUser.printMass(getContext(), fatMass);
            }
            int fatStatus = RequestWeight.getPercentFatDesc((byte) displayUser.age, (float) displayWeight.percentFat, displayUser.isMale);
            if (fatStatus == 0) {
                fatSub = getString(R.string.fat_percent_value_0);
            } else if (fatStatus == 1) {
                fatSub = getString(R.string.fat_percent_value_1);
            } else if (fatStatus == 2) {
                fatSub = getString(R.string.fat_percent_value_2);
            } else if (fatStatus == 3) {
                fatSub = getString(R.string.fat_percent_value_3);
            }
            updateCardData(binding.cardFat, fatMain, fatSub, fatStatus, lastWeight != null ? lastWeight.percentFat : -1, displayWeight.percentFat);

            // Water
            double waterMass = displayWeight.weight * displayWeight.percentHydration / 100;
            String waterMain = String.format(getString(R.string.weight_fragment_percent_tag), displayWeight.percentHydration);
            String waterSub = displayUser.printMass(getContext(), waterMass);
            int waterStatus = RequestWeight.getPercentHydrationDesc((float)displayWeight.percentHydration, displayUser.isMale);
            updateCardData(binding.cardWater, waterMain, waterSub, waterStatus, lastWeight != null ? lastWeight.percentHydration : -1, displayWeight.percentHydration);

            // Muscle & Bone
            updateCardData(binding.cardMuscle, displayUser.printMass(getContext(), displayWeight.muscleMass), null, -1, lastWeight != null ? lastWeight.muscleMass : -1, displayWeight.muscleMass);
            int boneStatus = RequestWeight.getBoneMassDesc((float)displayWeight.weight, (float)displayWeight.boneMass, displayUser.isMale);
            updateCardData(binding.cardBone, displayUser.printMass(getContext(), displayWeight.boneMass), null, boneStatus, lastWeight != null ? lastWeight.boneMass : -1, displayWeight.boneMass);

            // Other
            if (displayWeight.visceralFatRating != -1) {
                int s;
                String subVal;
                if (displayWeight.visceralFatRating < 13) {
                    s = 1;
                    subVal = getString(R.string.visceral_fat_sub13);
                } else {
                    s = 3;
                    subVal = getString(R.string.visceral_fat_plus13);
                }
                updateCardData(binding.cardVisceral, String.format(Locale.getDefault(),"%.2f", displayWeight.visceralFatRating), subVal, s, lastWeight != null ? lastWeight.visceralFatRating : -1, displayWeight.visceralFatRating);
            } else {
                //resetCard(binding.cardVisceral);
                binding.cardVisceral.metricCard.setVisibility(View.GONE);
            }

            if (displayWeight.physiqueRating != -1) {
                String desc = "";
                int status = -1;
                if (displayWeight.physiqueRating == 1) {
                    desc = getString(R.string.physique_rating_1);
                    status = 2;
                } else if (displayWeight.physiqueRating == 2) {
                    desc = getString(R.string.physique_rating_2);
                    status = 2;
                } else if (displayWeight.physiqueRating == 3) {
                    desc = getString(R.string.physique_rating_3);
                    status = 2;
                } else if (displayWeight.physiqueRating == 4) {
                    desc = getString(R.string.physique_rating_4);
                    status = 1;
                } else if (displayWeight.physiqueRating == 5) {
                    desc = getString(R.string.physique_rating_5);
                    status = 1;
                } else if (displayWeight.physiqueRating == 6) {
                    desc = getString(R.string.physique_rating_6);
                    status = 1;
                } else if (displayWeight.physiqueRating == 7) {
                    desc = getString(R.string.physique_rating_7);
                    status = 2;
                } else if (displayWeight.physiqueRating == 8) {
                    desc = getString(R.string.physique_rating_8);
                    status = 1;
                } else if (displayWeight.physiqueRating == 9) {
                    desc = getString(R.string.physique_rating_9);
                    status = 1;
                }
                updateCardData(binding.cardPhysique, String.valueOf(displayWeight.physiqueRating), desc, status, lastWeight != null ? lastWeight.physiqueRating : -1, displayWeight.physiqueRating);
            } else {
                //resetCard(binding.cardPhysique);
                binding.cardPhysique.metricCard.setVisibility(View.GONE);
            }

            if (displayWeight.metabolicAge != -1) {
                int s = (displayWeight.metabolicAge <= displayUser.age) ? 1 : 3;
                updateCardData(binding.cardMetabolicAge, String.format(getString(R.string.weight_fragment_years_tag), displayWeight.metabolicAge), null, s, lastWeight != null ? lastWeight.metabolicAge : -1, displayWeight.metabolicAge);
            } else {
                //resetCard(binding.cardMetabolicAge);
                binding.cardMetabolicAge.metricCard.setVisibility(View.GONE);
            }

            if (displayWeight.basalMet != -1) {
                updateCardData(binding.cardBMR, String.format(getString(R.string.weight_fragment_kcal_tag), displayWeight.basalMet), null, -1, lastWeight != null ? lastWeight.basalMet : -1, displayWeight.basalMet);
            } else {
                //resetCard(binding.cardBMR);
                binding.cardBMR.metricCard.setVisibility(View.GONE);
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

    private void updateSegmentUI(ItemSegmentBinding seg, double percent, double muscle, Weight w, User u, double lastVal, double currVal) {
        if (percent != -1) {
            String val, sub;
            if (u.show_fat_mass) {
                val = u.printMass(getContext(), w.weight * percent / 100);
            } else {
                val = String.format(getString(R.string.weight_fragment_percent_tag), percent);
            }
            sub = (muscle != -1) ? u.printMass(getContext(), muscle) : "";
            seg.metricValue.setText(val);
            if (!sub.isEmpty()) {
                seg.metricSubValue.setText(sub);
                seg.metricSubValue.setVisibility(View.VISIBLE);
            } else {
                seg.metricSubValue.setVisibility(View.GONE);
            }
            // Segments are Blue by default
            seg.metricIcon.setBackgroundResource(R.drawable.rounded_blue);

            if (lastVal != -1) {
                updateTrendIcon(currVal, lastVal, seg.metricTrend);
            } else {
                seg.metricTrend.setVisibility(View.INVISIBLE);
            }
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
    }

    // --- Helpers for ItemMetricCardBinding (Grid) ---
    private void setupCard(ItemMetricCardBinding card, Metric metric) {
        card.metricTitle.setText(metric.getLabelRes());
        card.metricIcon.setImageResource(metric.getIconRes());
    }

    private void updateCardData(ItemMetricCardBinding card, String mainVal, String subVal, int status, double lastVal, double currVal) {
        card.metricValue.setText(mainVal);
        if (subVal != null && !subVal.isEmpty()) {
            card.metricSubValue.setText(subVal);
            card.metricSubValue.setVisibility(View.VISIBLE);
        } else {
            card.metricSubValue.setVisibility(View.GONE);
        }
        updateIconBackground(card.metricIcon, status);

        if (lastVal != -1) {
            updateTrendIcon(currVal, lastVal, card.metricTrend);
        } else {
            card.metricTrend.setVisibility(View.INVISIBLE);
        }
        card.metricCard.setVisibility(currVal != -1 ? View.VISIBLE : View.GONE);
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

        resetSegment(binding.segTrunk);
        resetSegment(binding.segLeftArm);
        resetSegment(binding.segRightArm);
        resetSegment(binding.segLeftLeg);
        resetSegment(binding.segRightLeg);

        resetCard(binding.cardFat);
        resetCard(binding.cardWater);
        resetCard(binding.cardMuscle);
        resetCard(binding.cardBone);
        resetCard(binding.cardVisceral);
        resetCard(binding.cardPhysique);
        resetCard(binding.cardMetabolicAge);
        resetCard(binding.cardBMR);
    }

    private void updateIconBackground(ImageView iv, int status) {
        if (status == 1) iv.setBackgroundResource(R.drawable.rounded_green);
        else if (status == 3) iv.setBackgroundResource(R.drawable.rounded_red);
        else if (status == 0 || status == 2) iv.setBackgroundResource(R.drawable.rounded_yellow);
        else iv.setBackgroundResource(R.drawable.rounded_blue);
    }

    private void updateBMIStatus(ImageView iv, TextView tv,int status) {
        if (status == 1) {
            iv.setBackgroundResource(R.drawable.rounded_green);
            tv.setText(R.string.bmi_value_1);
        } else if (status == 3) {
            iv.setBackgroundResource(R.drawable.rounded_red);
            tv.setText(R.string.bmi_value_3);
        } else if (status == 0) {
            iv.setBackgroundResource(R.drawable.rounded_yellow);
            tv.setText(R.string.bmi_value_0);
        } else if (status == 2) {
            iv.setBackgroundResource(R.drawable.rounded_yellow);
            tv.setText(R.string.bmi_value_2);
        } else {
            iv.setBackgroundResource(R.drawable.rounded_blue);
            tv.setText("");
        }
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
                AntWeightController rw = ((MainActivity) getActivity()).newRequestWeight(this);
                rw.setProfile(user);
                rw.start();
                enableUploadButton = false;
                userToUpload = null;
                getActivity().invalidateOptionsMenu();
            }
            return true;
        } else if (itemId == R.id.action_weight_upload) {
            if (getActivity() != null) {
                if (((MainActivity)getActivity()).getRequestWeight() != null) {
                    MainActivity.uploadButton((MainActivity)getActivity(), ((MainActivity)getActivity()).getRequestWeight().weight, userToUpload);
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
                            if (activity != null && activity.getRequestWeight() != null) {
                                activity.getRequestWeight().cancel();
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
