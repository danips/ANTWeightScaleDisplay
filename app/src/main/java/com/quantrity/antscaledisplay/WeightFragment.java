package com.quantrity.antscaledisplay;

import android.os.Bundle;
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

import com.quantrity.antscaledisplay.databinding.FragmentWeightBinding;
import com.quantrity.antscaledisplay.databinding.ItemMetricCardBinding;
import com.quantrity.antscaledisplay.databinding.ItemSegmentBinding;

import java.util.ArrayList;
import java.util.Locale;

public class WeightFragment extends Fragment implements MenuProvider {
    //private static final String TAG = "WeightFragment";

    private FragmentWeightBinding binding;
    private boolean enableUploadButton = false;
    User userToUpload = null;
    private Spinner usersSpinner;

    public WeightFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentWeightBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        // 1. Initialize Segment Cards (Using ItemSegmentBinding)
        setupSegment(binding.segTrunk, R.drawable.ic_trunk, getString(R.string.weight_fragment_icon_desc_trunk));
        setupSegment(binding.segLeftArm, R.drawable.ic_left_arm, getString(R.string.weight_fragment_icon_desc_leftArm));
        setupSegment(binding.segRightArm, R.drawable.ic_right_arm, getString(R.string.weight_fragment_icon_desc_rightArm));
        setupSegment(binding.segLeftLeg, R.drawable.ic_left_leg, getString(R.string.weight_fragment_icon_desc_leftLeg));
        setupSegment(binding.segRightLeg, R.drawable.ic_right_leg, getString(R.string.weight_fragment_icon_desc_rightLeg));

        // 2. Initialize Grid Cards (Using ItemMetricCardBinding)
        setupCard(binding.cardFat, R.drawable.ic_percent_fat, getString(R.string.weight_fragment_icon_desc_percentFat));
        setupCard(binding.cardWater, R.drawable.ic_percent_hydration, getString(R.string.weight_fragment_icon_desc_percentHydration));
        setupCard(binding.cardMuscle, R.drawable.ic_muscle_mass, getString(R.string.weight_fragment_icon_desc_muscleMass));
        setupCard(binding.cardBone, R.drawable.ic_bone_mass, getString(R.string.weight_fragment_icon_desc_boneMass));
        setupCard(binding.cardVisceral, R.drawable.ic_visceral_fat_rating, getString(R.string.weight_fragment_icon_desc_visceralFat));
        setupCard(binding.cardPhysique, R.drawable.ic_physique_rating, getString(R.string.weight_fragment_icon_desc_physiqueRating));
        setupCard(binding.cardMetabolicAge, R.drawable.ic_metabolic_age, getString(R.string.weight_fragment_icon_desc_metabolicAge));
        setupCard(binding.cardBMR, R.drawable.ic_metabolic, getString(R.string.weight_fragment_icon_desc_basalMet));

        binding.fab.setOnClickListener(view -> {
            MainActivity ma = (MainActivity)getActivity();
            if (ma != null) {
                RequestWeight rw = ma.getRequestWeight();
                if (rw != null) {
                    ma.openEditWeightFragment(rw.the_weight, rw.the_user, true);
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
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    void updateUi() {
        if (getActivity() == null || binding == null) return;

        final MainActivity mainActivity = (MainActivity) getActivity();
        final RequestWeight rw = mainActivity.getRequestWeight();

        mainActivity.runOnUiThread(() -> {
            if (getActivity() == null || binding == null) return;

            Weight displayWeight = null;
            User displayUser = mainActivity.getSelectedUser();

            // 1. Live Data or 2. History
            if (rw != null && rw.the_weight != null && rw.the_weight.weight != -1 && rw.the_user != null) {
                displayWeight = rw.the_weight;
                displayUser = rw.the_user;
            } else if (displayUser != null) {
                ArrayList<Weight> history = mainActivity.getHistoryArray();
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
            ArrayList<Weight> history = mainActivity.getHistoryArray();
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
            double lastVal, currVal;
            boolean hasData = false;
            currVal = displayUser.show_fat_mass ? displayWeight.trunkMuscleMass : displayWeight.trunkPercentFat;
            lastVal = lastWeight != null ? (displayUser.show_fat_mass ? lastWeight.trunkMuscleMass : lastWeight.trunkPercentFat) : -1;
            hasData |= currVal != -1;
            updateSegmentUI(binding.segTrunk, displayWeight.trunkPercentFat, displayWeight.trunkMuscleMass, displayWeight, displayUser, lastVal, currVal);

            currVal = displayUser.show_fat_mass ? displayWeight.leftArmMuscleMass : displayWeight.leftArmPercentFat;
            lastVal = lastWeight != null ? (displayUser.show_fat_mass ? lastWeight.leftArmMuscleMass : lastWeight.leftArmPercentFat) : -1;
            hasData |= currVal != -1;
            updateSegmentUI(binding.segLeftArm, displayWeight.leftArmPercentFat, displayWeight.leftArmMuscleMass, displayWeight, displayUser, lastVal, currVal);

            currVal = displayUser.show_fat_mass ? displayWeight.rightArmMuscleMass : displayWeight.rightArmPercentFat;
            lastVal = lastWeight != null ? (displayUser.show_fat_mass ? lastWeight.rightArmMuscleMass : lastWeight.rightArmPercentFat) : -1;
            hasData |= currVal != -1;
            updateSegmentUI(binding.segRightArm, displayWeight.rightArmPercentFat, displayWeight.rightArmMuscleMass, displayWeight, displayUser, lastVal, currVal);

            currVal = displayUser.show_fat_mass ? displayWeight.leftLegMuscleMass : displayWeight.leftLegPercentFat;
            lastVal = lastWeight != null ? (displayUser.show_fat_mass ? lastWeight.leftLegMuscleMass : lastWeight.leftLegPercentFat) : -1;
            hasData |= currVal != -1;
            updateSegmentUI(binding.segLeftLeg, displayWeight.leftLegPercentFat, displayWeight.leftLegMuscleMass, displayWeight, displayUser, lastVal, currVal);

            currVal = displayUser.show_fat_mass ? displayWeight.rightLegMuscleMass : displayWeight.rightLegPercentFat;
            lastVal = lastWeight != null ? (displayUser.show_fat_mass ? lastWeight.rightLegMuscleMass : lastWeight.rightLegPercentFat) : -1;
            hasData |= currVal != -1;
            updateSegmentUI(binding.segRightLeg, displayWeight.rightLegPercentFat, displayWeight.rightLegMuscleMass, displayWeight, displayUser, lastVal, currVal);
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
                    MainActivity.uploadButton(mainActivity, rw != null ? rw.the_weight : displayWeight, userToUpload);
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

    private void updateSegmentUI(ItemSegmentBinding seg, double percent, double muscle, Weight w, User u, double lastVal, double currVal) {
        if (percent != -1) {
            String val, sub;
            if (u.show_fat_mass) {
                val = u.printMass(getContext(), w.weight * percent / 100);
                sub = (muscle != -1) ? u.printMass(getContext(), muscle) : "";
            } else {
                val = String.format(getString(R.string.weight_fragment_percent_tag), percent);
                sub = (muscle != -1) ? u.printMass(getContext(), muscle) : "";
            }
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
    private void setupCard(ItemMetricCardBinding card, int iconRes, String title) {
        card.metricTitle.setText(title);
        card.metricIcon.setImageResource(iconRes);
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
                ((MainActivity)getActivity()).setSelectedUser((User)adapterView.getItemAtPosition(i));
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
                RequestWeight rw = ((MainActivity) getActivity()).newRequestWeight(this);
                rw.setProfile(user);
                rw.initService();
                enableUploadButton = false;
                userToUpload = null;
                getActivity().invalidateOptionsMenu();
            }
            return true;
        } else if (itemId == R.id.action_weight_upload) {
            if (getActivity() != null) {
                if (((MainActivity)getActivity()).getRequestWeight() != null) {
                    MainActivity.uploadButton((MainActivity)getActivity(), ((MainActivity)getActivity()).getRequestWeight().the_weight, userToUpload);
                }
            }
            enableUploadButton = false;
            if (getActivity() != null) getActivity().invalidateOptionsMenu();
            return true;
        }
        return false;
    }
}