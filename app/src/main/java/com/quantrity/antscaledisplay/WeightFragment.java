package com.quantrity.antscaledisplay;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;

public class WeightFragment extends Fragment {
    private static final String TAG = "WeightFragment";

    private TextView weightTV = null;
    private TextView trunkPercentFatTV = null;
    private TextView trunkMuscleMassTV = null;
    private TextView leftArmPercentFatTV = null;
    private TextView leftArmMuscleMassTV = null;
    private TextView rightArmPercentFatTV = null;
    private TextView rightArmMuscleMassTV = null;
    private TextView leftLegPercentFatTV = null;
    private TextView leftLegMuscleMassTV = null;
    private TextView rightLegPercentFatTV = null;
    private TextView rightLegMuscleMassTV = null;
    private TextView percentFatTV = null;
    private TextView percentHydrationTV = null;
    private TextView boneMassTV = null;
    private TextView muscleMassTV = null;
    private TextView physiqueRatingTV = null;
    private TextView visceralFatRatingTV = null;
    private TextView metabolicAgeTV = null;
    private TextView basalMetTV = null;
    private TextView weightTV2 = null;
    private TextView percentFatTV2 = null;
    private TextView percentHydrationTV2 = null;
    private TextView physiqueRatingTV2 = null;
    private TextView visceralFatRatingTV2 = null;
    private ImageView weightIV = null;
    private ImageView trunkIV;
    private ImageView leftArmIV;
    private ImageView rightArmIV;
    private ImageView leftLegIV;
    private ImageView rightLegIV;
    private ImageView percentFatIV = null;
    private ImageView percentHydrationIV = null;
    private ImageView boneMassIV = null;
    private ImageView muscleMassIV = null;
    private ImageView physiqueRatingIV = null;
    private ImageView visceralFatRatingIV = null;
    private ImageView metabolicAgeIV = null;
    private ImageView basalMetIV = null;
    private ImageView weightIVmini = null;
    private ImageView trunkIVminiTop = null;
    private ImageView trunkIVminiBottom = null;
    private ImageView leftArmIVminiTop = null;
    private ImageView leftArmIVminiBottom = null;
    private ImageView rightArmIVminiTop = null;
    private ImageView rightArmIVminiBottom = null;
    private ImageView leftLegIVminiTop = null;
    private ImageView leftLegIVminiBottom = null;
    private ImageView rightLegIVminiTop = null;
    private ImageView rightLegIVminiBottom = null;
    private ImageView percentFatIVmini = null;
    private ImageView percentHydrationIVmini = null;
    private ImageView boneMassIVmini = null;
    private ImageView muscleMassIVmini = null;
    private ImageView physiqueRatingIVmini = null;
    private ImageView visceralFatRatingIVmini = null;
    private ImageView metabolicAgeIVmini = null;
    private ImageView basalMetIVmini = null;
    private TableRow armsTR = null;
    private TableRow legsTR = null;
    private TableRow physiqueRatingvisceralFatRatingTR = null;
    private RelativeLayout trunkRL = null;
    private LinearLayout trunkLL = null;

    private boolean enableUploadButton = false;
    User userToUpload = null;

    private Spinner usersSpinner;

    public WeightFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weight, container, false);

        weightTV = rootView.findViewById(R.id.weightTV);
        trunkPercentFatTV = rootView.findViewById(R.id.trunkPercentFatTV);
        trunkMuscleMassTV = rootView.findViewById(R.id.trunkMuscleMassTV);
        leftArmPercentFatTV = rootView.findViewById(R.id.leftArmPercentFatTV);
        leftArmMuscleMassTV = rootView.findViewById(R.id.leftArmMuscleMassTV);
        rightArmPercentFatTV = rootView.findViewById(R.id.rightArmPercentFatTV);
        rightArmMuscleMassTV = rootView.findViewById(R.id.rightArmMuscleMassTV);
        leftLegPercentFatTV = rootView.findViewById(R.id.leftLegPercentFatTV);
        leftLegMuscleMassTV = rootView.findViewById(R.id.leftLegMuscleMassTV);
        rightLegPercentFatTV = rootView.findViewById(R.id.rightLegPercentFatTV);
        rightLegMuscleMassTV = rootView.findViewById(R.id.rightLegMuscleMassTV);
        percentFatTV = rootView.findViewById(R.id.percentFatTV);
        percentHydrationTV = rootView.findViewById(R.id.percentHydrationTV);
        boneMassTV = rootView.findViewById(R.id.boneMassTV);
        muscleMassTV = rootView.findViewById(R.id.muscleMassTV);
        physiqueRatingTV = rootView.findViewById(R.id.physiqueRatingTV);
        visceralFatRatingTV = rootView.findViewById(R.id.visceralFatRatingTV);
        metabolicAgeTV = rootView.findViewById(R.id.metabolicAgeTV);
        basalMetTV = rootView.findViewById(R.id.basalMetTV);
        weightTV2 = rootView.findViewById(R.id.weightTV2);
        percentFatTV2 = rootView.findViewById(R.id.percentFatTV2);
        percentHydrationTV2 = rootView.findViewById(R.id.percentHydrationTV2);
        physiqueRatingTV2 = rootView.findViewById(R.id.physiqueRatingTV2);
        visceralFatRatingTV2 = rootView.findViewById(R.id.visceralFatRatingTV2);
        weightIV = rootView.findViewById(R.id.weightIV);

        trunkIV = rootView.findViewById(R.id.trunkIV);
        leftArmIV = rootView.findViewById(R.id.leftArmIV);
        rightArmIV = rootView.findViewById(R.id.rightArmIV);
        leftLegIV = rootView.findViewById(R.id.leftLegIV);
        rightLegIV = rootView.findViewById(R.id.rightLegIV);
        percentFatIV = rootView.findViewById(R.id.percentFatIV);
        percentHydrationIV = rootView.findViewById(R.id.percentHydrationIV);
        boneMassIV = rootView.findViewById(R.id.boneMassIV);
        muscleMassIV = rootView.findViewById(R.id.muscleMassIV);
        physiqueRatingIV = rootView.findViewById(R.id.physiqueRatingIV);
        visceralFatRatingIV = rootView.findViewById(R.id.visceralFatRatingIV);
        metabolicAgeIV = rootView.findViewById(R.id.metabolicAgeIV);
        basalMetIV = rootView.findViewById(R.id.basalMetIV);
        weightIVmini = rootView.findViewById(R.id.weightIVmini);
        trunkIVminiTop = rootView.findViewById(R.id.trunkIVminiTop);
        trunkIVminiBottom = rootView.findViewById(R.id.trunkIVminiBottom);
        leftArmIVminiTop = rootView.findViewById(R.id.leftArmIVminiTop);
        leftArmIVminiBottom = rootView.findViewById(R.id.leftArmIVminiBottom);
        rightArmIVminiTop = rootView.findViewById(R.id.rightArmIVminiTop);
        rightArmIVminiBottom = rootView.findViewById(R.id.rightArmIVminiBottom);
        leftLegIVminiTop = rootView.findViewById(R.id.leftLegIVminiTop);
        leftLegIVminiBottom = rootView.findViewById(R.id.leftLegIVminiBottom);
        rightLegIVminiTop = rootView.findViewById(R.id.rightLegIVminiTop);
        rightLegIVminiBottom = rootView.findViewById(R.id.rightLegIVminiBottom);
        percentFatIVmini = rootView.findViewById(R.id.percentFatIVmini);
        percentHydrationIVmini = rootView.findViewById(R.id.percentHydrationIVmini);
        boneMassIVmini = rootView.findViewById(R.id.boneMassIVmini);
        muscleMassIVmini = rootView.findViewById(R.id.muscleMassIVmini);
        physiqueRatingIVmini = rootView.findViewById(R.id.physiqueRatingIVmini);
        visceralFatRatingIVmini = rootView.findViewById(R.id.visceralFatRatingIVmini);
        metabolicAgeIVmini = rootView.findViewById(R.id.metabolicAgeIVmini);
        basalMetIVmini = rootView.findViewById(R.id.basalMetIVmini);
        armsTR = rootView.findViewById(R.id.armsTR);
        legsTR = rootView.findViewById(R.id.legsTR);
        physiqueRatingvisceralFatRatingTR = rootView.findViewById(R.id.physiqueRatingvisceralFatRatingTR);
        trunkRL = rootView.findViewById(R.id.trunkRL);
        trunkLL = rootView.findViewById(R.id.trunkLL);

        FloatingActionButton fab = rootView.findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            MainActivity ma = (MainActivity)getActivity();
            if (ma != null) {
                RequestWeight rw;
                if ((rw = ma.getRequestWeight()) != null) {
                    ma.openEditWeightFragment(rw.the_weight, rw.the_user, true);
                } else {
                    ma.openEditWeightFragment(null, null, false);
                }
            }
        });

        //Declare it has items for the actionbar
        setHasOptionsMenu(true);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        //Rewrite results of previous weight
        updateUi();
    }

    private void updateMiniImageView(double now, double last, final ImageView ivmini) {
        if (last != -1) {
            if ((now <= last + 0.05) && (now >= last - 0.05)) {
                ivmini.setImageResource(R.drawable.ic_equal);
            } else if (now > last) {
                ivmini.setImageResource(R.drawable.ic_more);
            } else {
                ivmini.setImageResource(R.drawable.ic_less);
            }

            ivmini.setVisibility(View.VISIBLE);
        }
    }

    // updates UI to reflect model
    void updateUi() {
        if (getActivity() == null) {
            Log.v(TAG, "********** updateUi with getActivity NULL **********");
            return;
        }
        final RequestWeight rw = ((MainActivity)getActivity()).getRequestWeight();

        getActivity().runOnUiThread(() -> {
            if (getActivity() == null) return;
            if ((rw == null) || (rw.the_weight == null) || (rw.the_weight.weight == -1) || (rw.the_user == null)) {
                resetFields(null, ((MainActivity)getActivity()).getSelectedUser());
                return;
            }

            resetFields(rw.the_weight, rw.the_user);
            //Get the most recent weight to compare
            User the_user = rw.the_user;

            ArrayList<Weight> history = ((MainActivity)getActivity()).getHistoryArray();
            Iterator<Weight> it = history.iterator();
            Weight last_weight = null;
            while (it.hasNext()) {
                Weight w = it.next();
                if (w.uuid.equals(the_user.uuid)) {
                    if ((w.date != rw.the_weight.date)) {
                        if (last_weight == null) last_weight = w;
                        else if (last_weight.date < w.date) last_weight = w;
                    }
                }
            }

            weightTV.setText(the_user.printMass(getContext(), rw.the_weight.weight));

            byte height = (byte)the_user.height_cm;
            float h = height & 0xff;
            float bmi = (float) (rw.the_weight.weight / Math.pow(h / 100, 2));

            switch (RequestWeight.getBMIDesc((byte)the_user.age, bmi, the_user.isMale)) {
                case 0:
                case 2:
                    weightIV.setBackgroundResource(R.drawable.rounded_yellow);
                    break;
                case 1:
                    weightIV.setBackgroundResource(R.drawable.rounded_green);
                    break;
                case 3:
                    weightIV.setBackgroundResource(R.drawable.rounded_red);
                    break;
                default:
                    weightIV.setBackgroundResource(R.drawable.rounded_blue);
                    break;
            }
            weightTV2.setText(getString(R.string.weight_fragment_bmi_tag) + " " + String.format("%.02f", bmi));

            if (last_weight != null) updateMiniImageView(rw.the_weight.weight, last_weight.weight, weightIVmini);


            if ((the_user.gc_user != null) && (the_user.gc_pass != null)) {

                if ((userToUpload != null) && (!enableUploadButton) && (the_user.autoupload))
                    uploadButton();

                enableUploadButton = true;
                userToUpload = the_user;
            }
            getActivity().invalidateOptionsMenu();


            if (rw.the_weight.percentFat != -1) {
                if (the_user.show_fat_mass) {
                    percentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.percentFat / 100));
                }
                else {
                    percentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.percentFat));
                }
                switch (RequestWeight.getPercentFatDesc((byte) the_user.age, (float) rw.the_weight.percentFat, the_user.isMale)) {
                    case 0:
                        percentFatTV2.setText(getString(R.string.fat_percent_value_0));
                        percentFatIV.setBackgroundResource(R.drawable.rounded_yellow);
                        break;
                    case 1:
                        percentFatTV2.setText(getString(R.string.fat_percent_value_1));
                        percentFatIV.setBackgroundResource(R.drawable.rounded_green);
                        break;
                    case 2:
                        percentFatTV2.setText(getString(R.string.fat_percent_value_2));
                        percentFatIV.setBackgroundResource(R.drawable.rounded_yellow);
                        break;
                    case 3:
                        percentFatTV2.setText(getString(R.string.fat_percent_value_3));
                        percentFatIV.setBackgroundResource(R.drawable.rounded_red);
                        break;
                    default:
                        percentFatTV2.setText("");
                        percentFatIV.setBackgroundResource(R.drawable.rounded_blue);
                }
                if (last_weight != null) updateMiniImageView(rw.the_weight.percentFat, last_weight.percentFat, percentFatIVmini);

                armsTR.setVisibility(View.VISIBLE);
                legsTR.setVisibility(View.VISIBLE);
                trunkIV.setBackgroundResource(R.drawable.rounded_blue);
                leftArmIV.setBackgroundResource(R.drawable.rounded_blue);
                rightArmIV.setBackgroundResource(R.drawable.rounded_blue);
                leftLegIV.setBackgroundResource(R.drawable.rounded_blue);
                rightLegIV.setBackgroundResource(R.drawable.rounded_blue);
                if (the_user.show_fat_mass)
                {
                    if (rw.the_weight.trunkPercentFat != -1) trunkPercentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.trunkPercentFat / 100));
                    if (rw.the_weight.leftArmPercentFat != -1) leftArmPercentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.leftArmPercentFat / 100));
                    if (rw.the_weight.rightArmPercentFat != -1) rightArmPercentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.rightArmPercentFat / 100));
                    if (rw.the_weight.leftLegPercentFat != -1) leftLegPercentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.leftLegPercentFat / 100));
                    if (rw.the_weight.rightLegPercentFat != -1) rightLegPercentFatTV.setText(the_user.printMass(getContext(), rw.the_weight.weight * rw.the_weight.rightLegPercentFat / 100));
                }
                else {
                    if (rw.the_weight.trunkPercentFat != -1) trunkPercentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.trunkPercentFat));
                    if (rw.the_weight.leftArmPercentFat != -1) leftArmPercentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.leftArmPercentFat));
                    if (rw.the_weight.rightArmPercentFat != -1) rightArmPercentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.rightArmPercentFat));
                    if (rw.the_weight.leftLegPercentFat != -1) leftLegPercentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.leftLegPercentFat));
                    if (rw.the_weight.rightLegPercentFat != -1) rightLegPercentFatTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.rightLegPercentFat));
                }
                if ((rw.the_weight.trunkPercentFat != -1) && (last_weight != null)) updateMiniImageView(rw.the_weight.trunkPercentFat, last_weight.trunkPercentFat, trunkIVminiTop);
                if ((rw.the_weight.leftArmPercentFat != -1) && (last_weight != null)) updateMiniImageView(rw.the_weight.leftArmPercentFat, last_weight.leftArmPercentFat, leftArmIVminiTop);
                if ((rw.the_weight.rightArmPercentFat != -1) && (last_weight != null)) updateMiniImageView(rw.the_weight.rightArmPercentFat, last_weight.rightArmPercentFat, rightArmIVminiTop);
                if ((rw.the_weight.leftLegPercentFat != -1) && (last_weight != null)) updateMiniImageView(rw.the_weight.leftLegPercentFat, last_weight.leftLegPercentFat, leftLegIVminiTop);
                if ((rw.the_weight.rightLegPercentFat != -1) && (last_weight != null)) updateMiniImageView(rw.the_weight.rightLegPercentFat, last_weight.rightLegPercentFat, rightLegIVminiTop);

                if (rw.the_weight.trunkMuscleMass != -1) {
                    trunkMuscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.trunkMuscleMass));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.trunkMuscleMass, last_weight.trunkMuscleMass, trunkIVminiBottom);
                    trunkLL.setVisibility(View.VISIBLE);
                    trunkRL.setVisibility(View.VISIBLE);
                }
                else if (rw.the_weight.trunkPercentFat == -1)
                {
                    trunkLL.setVisibility(View.GONE);
                    trunkRL.setVisibility(View.GONE);
                }
                if (rw.the_weight.leftArmMuscleMass != -1) {
                    leftArmMuscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.leftArmMuscleMass));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.leftArmMuscleMass, last_weight.leftArmMuscleMass, leftArmIVminiBottom);
                }
                if (rw.the_weight.rightArmMuscleMass != -1) {
                    rightArmMuscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.rightArmMuscleMass));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.rightArmMuscleMass, last_weight.rightArmMuscleMass, rightArmIVminiBottom);
                }
                else
                {
                    if ((rw.the_weight.rightArmPercentFat == -1) && (rw.the_weight.leftArmPercentFat == -1) && (rw.the_weight.leftArmMuscleMass == -1))
                    {
                        armsTR.setVisibility(View.GONE);
                    }
                }
                if (rw.the_weight.leftLegMuscleMass != -1) {
                    leftLegMuscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.leftLegMuscleMass));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.leftLegMuscleMass, last_weight.leftLegMuscleMass, leftLegIVminiBottom);
                }
                if (rw.the_weight.rightLegMuscleMass != -1) {
                    rightLegMuscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.rightLegMuscleMass));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.rightLegMuscleMass, last_weight.rightLegMuscleMass, rightLegIVminiBottom);
                }
                else
                {
                    if ((rw.the_weight.rightLegPercentFat == -1) && (rw.the_weight.leftLegPercentFat == -1) && (rw.the_weight.leftLegMuscleMass == -1))
                    {
                        legsTR.setVisibility(View.GONE);
                    }
                }

                percentHydrationTV.setText(String.format(getString(R.string.weight_fragment_percent_tag), rw.the_weight.percentHydration));

                boneMassTV.setText(the_user.printMass(getContext(), rw.the_weight.boneMass));
                muscleMassTV.setText(the_user.printMass(getContext(), rw.the_weight.muscleMass));
                if (last_weight != null) updateMiniImageView(rw.the_weight.muscleMass, last_weight.muscleMass, muscleMassIVmini);
                muscleMassIV.setBackgroundResource(R.drawable.rounded_blue);

                switch (RequestWeight.getPercentHydrationDesc((float)rw.the_weight.percentHydration, the_user.isMale)) {
                    case 0:
                        percentHydrationTV2.setText(getString(R.string.hydration_percent_value_0));
                        percentHydrationIV.setBackgroundResource(R.drawable.rounded_yellow);
                        break;
                    case 1:
                        percentHydrationTV2.setText(getString(R.string.hydration_percent_value_1));
                        percentHydrationIV.setBackgroundResource(R.drawable.rounded_green);
                        break;
                    case 2:
                        percentHydrationTV2.setText(getString(R.string.hydration_percent_value_2));
                        percentHydrationIV.setBackgroundResource(R.drawable.rounded_yellow);
                        break;
                    default:
                        percentHydrationTV2.setText("");
                        percentHydrationIV.setBackgroundResource(R.drawable.rounded_blue);
                }
                if (last_weight != null) updateMiniImageView(rw.the_weight.percentHydration, last_weight.percentHydration, percentHydrationIVmini);

                switch (RequestWeight.getBoneMassDesc((float)rw.the_weight.weight, (float)rw.the_weight.boneMass, the_user.isMale)) {
                    case 0: boneMassIV.setBackgroundResource(R.drawable.rounded_red);
                        break;
                    case 1: boneMassIV.setBackgroundResource(R.drawable.rounded_green);
                        break;
                    default: boneMassIV.setBackgroundResource(R.drawable.rounded_blue);
                }
                if (last_weight != null) updateMiniImageView(rw.the_weight.boneMass, last_weight.boneMass, boneMassIVmini);

                basalMetIV.setBackgroundResource(R.drawable.rounded_blue);
                if (rw.the_weight.metabolicAge != -1) {
                    metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_age);
                    metabolicAgeTV.setText(String.format(getString(R.string.weight_fragment_years_tag), rw.the_weight.metabolicAge));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.metabolicAge, last_weight.metabolicAge, metabolicAgeIVmini);
                    if (rw.the_weight.metabolicAge <= (byte)the_user.age) metabolicAgeIV.setBackgroundResource(R.drawable.rounded_green);
                    else if (rw.the_weight.metabolicAge <= ((byte)the_user.age) * 1.1) metabolicAgeIV.setBackgroundResource(R.drawable.rounded_yellow);
                    else metabolicAgeIV.setBackgroundResource(R.drawable.rounded_red);
                    basalMetTV.setText(String.format(getString(R.string.weight_fragment_kcal_tag), rw.the_weight.basalMet));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.basalMet, last_weight.basalMet, basalMetIVmini);
                } else if (rw.the_weight.activeMet != -1) {
                    metabolicAgeIV.setImageResource(R.drawable.ic_metabolic);
                    metabolicAgeTV.setText(getString(R.string.weight_fragment_amr_abbreviation) + "\n" + String.format(getString(R.string.weight_fragment_kcal_tag), rw.the_weight.activeMet));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.activeMet, last_weight.activeMet, metabolicAgeIVmini);
                    metabolicAgeIV.setBackgroundResource(R.drawable.rounded_blue);
                    basalMetTV.setText(getString(R.string.weight_fragment_bmr_abbreviation) + "\n" + String.format(getString(R.string.weight_fragment_kcal_tag), rw.the_weight.basalMet));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.basalMet, last_weight.basalMet, basalMetIVmini);
                }


                if (rw.the_weight.physiqueRating != -1) {
                    physiqueRatingTV.setText(String.valueOf(rw.the_weight.physiqueRating));
                    if (last_weight != null)
                        updateMiniImageView(rw.the_weight.physiqueRating, last_weight.physiqueRating, physiqueRatingIVmini);
                    switch (rw.the_weight.physiqueRating) {
                        case 1:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_1));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_red);
                            break;
                        case 2:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_2));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_red);
                            break;
                        case 3:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_3));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_red);
                            break;
                        case 4:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_4));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        case 5:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_5));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        case 6:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_6));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        case 7:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_7));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        case 8:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_8));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        case 9:
                            physiqueRatingTV2.setText(getString(R.string.physique_rating_9));
                            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green);
                            break;
                        default:
                            physiqueRatingTV2.setText("");
                    }
                }

                 if (rw.the_weight.visceralFatRating != -1) {
                    visceralFatRatingTV.setText(String.format("%.2f", rw.the_weight.visceralFatRating));
                    if (last_weight != null) updateMiniImageView(rw.the_weight.visceralFatRating, last_weight.visceralFatRating, visceralFatRatingIVmini);

                    if ((rw.the_weight.visceralFatRating >= 1.0) && (rw.the_weight.visceralFatRating <= 12.5)) {
                        visceralFatRatingTV2.setText(getString(R.string.visceral_fat_sub13));
                        visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_green);
                    } else if ((rw.the_weight.visceralFatRating >= 12.5) && (rw.the_weight.visceralFatRating <= 59.0)) {
                        visceralFatRatingTV2.setText(getString(R.string.visceral_fat_plus13));
                        visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_red);
                    } else {
                        visceralFatRatingTV2.setText("");
                        visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_blue);
                    }
                }

                if ((rw.the_weight.physiqueRating == -1) && (rw.the_weight.visceralFatRating == -1)) physiqueRatingvisceralFatRatingTR.setVisibility(View.GONE);
                else physiqueRatingvisceralFatRatingTR.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resetFields(Weight weight, User user) {
        if (weight == null) {
            weightIV.setBackgroundResource(R.drawable.rounded_blue);

            weightIVmini.setVisibility(View.INVISIBLE);
            percentFatIVmini.setVisibility(View.INVISIBLE);
            percentHydrationIVmini.setVisibility(View.INVISIBLE);
            boneMassIVmini.setVisibility(View.INVISIBLE);
            muscleMassIVmini.setVisibility(View.INVISIBLE);
            physiqueRatingIVmini.setVisibility(View.INVISIBLE);
            visceralFatRatingIVmini.setVisibility(View.INVISIBLE);
            metabolicAgeIVmini.setVisibility(View.INVISIBLE);
            basalMetIVmini.setVisibility(View.INVISIBLE);

            trunkIV.setBackgroundResource(R.drawable.rounded_blue);
            leftArmIV.setBackgroundResource(R.drawable.rounded_blue);
            rightArmIV.setBackgroundResource(R.drawable.rounded_blue);
            leftLegIV.setBackgroundResource(R.drawable.rounded_blue);
            rightLegIV.setBackgroundResource(R.drawable.rounded_blue);
            percentHydrationIV.setBackgroundResource(R.drawable.rounded_blue);
            boneMassIV.setBackgroundResource(R.drawable.rounded_blue);
            muscleMassIV.setBackgroundResource(R.drawable.rounded_blue);
            physiqueRatingIV.setBackgroundResource(R.drawable.rounded_blue);
            visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_blue);
            metabolicAgeIV.setBackgroundResource(R.drawable.rounded_blue);
            basalMetIV.setBackgroundResource(R.drawable.rounded_blue);
            percentFatIV.setBackgroundResource(R.drawable.rounded_blue);
        } else {
            if (weight.percentFat == -1) {
                percentFatIVmini.setVisibility(View.INVISIBLE);
                percentHydrationIVmini.setVisibility(View.INVISIBLE);
                boneMassIVmini.setVisibility(View.INVISIBLE);
                muscleMassIVmini.setVisibility(View.INVISIBLE);
                physiqueRatingIVmini.setVisibility(View.INVISIBLE);
                visceralFatRatingIVmini.setVisibility(View.INVISIBLE);
                metabolicAgeIVmini.setVisibility(View.INVISIBLE);
                basalMetIVmini.setVisibility(View.INVISIBLE);

                trunkIV.setBackgroundResource(R.drawable.rounded_grey);
                leftArmIV.setBackgroundResource(R.drawable.rounded_grey);
                rightArmIV.setBackgroundResource(R.drawable.rounded_grey);
                leftLegIV.setBackgroundResource(R.drawable.rounded_grey);
                rightLegIV.setBackgroundResource(R.drawable.rounded_grey);
                percentHydrationIV.setBackgroundResource(R.drawable.rounded_grey);
                boneMassIV.setBackgroundResource(R.drawable.rounded_grey);
                muscleMassIV.setBackgroundResource(R.drawable.rounded_grey);
                physiqueRatingIV.setBackgroundResource(R.drawable.rounded_grey);
                visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_grey);
                metabolicAgeIV.setBackgroundResource(R.drawable.rounded_grey);
                basalMetIV.setBackgroundResource(R.drawable.rounded_grey);
                percentFatIV.setBackgroundResource(R.drawable.rounded_grey);
            }
            else
            {
                percentFatIV.setBackgroundResource(R.drawable.rounded_blue);
            }
        }

        char decimal_separator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        weightTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        weightTV2.setText(getString(R.string.weight_fragment_bmi_tag));

        String fat_conditional_format = String.format(getString(((user != null) && user.show_fat_mass) ? R.string.string_no_decimal_value : R.string.string_no_decimal_percent_value) , decimal_separator);

        trunkPercentFatTV.setText(fat_conditional_format);
        trunkMuscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        leftArmPercentFatTV.setText(fat_conditional_format);
        leftArmMuscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        rightArmPercentFatTV.setText(fat_conditional_format);
        rightArmMuscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        leftLegPercentFatTV.setText(fat_conditional_format);
        leftLegMuscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        rightLegPercentFatTV.setText(fat_conditional_format);
        rightLegMuscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        percentFatTV.setText(fat_conditional_format);
        percentHydrationTV.setText(String.format(getString(R.string.string_no_decimal_percent_value), decimal_separator));
        boneMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        muscleMassTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        physiqueRatingTV.setText(getString(R.string.string_no_value));
        visceralFatRatingTV.setText(String.format(getString(R.string.string_no_decimal_value), decimal_separator));
        metabolicAgeTV.setText(getString(R.string.string_no_value));
        basalMetTV.setText(getString(R.string.string_no_value));
        percentFatTV2.setText(getString(R.string.string_no_value));
        percentHydrationTV2.setText(getString(R.string.string_no_value));
        physiqueRatingTV2.setText(getString(R.string.string_no_value));
        visceralFatRatingTV2.setText(getString(R.string.string_no_value));
    }

    private void weightButton() {
        User user = null;

        if (usersSpinner != null) {
            user = (User)usersSpinner.getSelectedItem();
        }

        if ((user == null) && (getActivity() != null)) {
            showMessage(R.string.edit_user_fragment_msg_user_missing);
            ((MainActivity)getActivity()).openEditUserFragment(null);
            return;
        }

        if (getActivity() != null) {
            RequestWeight rw = ((MainActivity) getActivity()).newRequestWeight(this);
            rw.setProfile(user);
            rw.initService();

            enableUploadButton = false;
            userToUpload = null;
            getActivity().invalidateOptionsMenu();
        }
    }

    private void uploadButton() {
        if ((getActivity() != null) && ((MainActivity)getActivity()).getRequestWeight() != null)
        {
            MainActivity.uploadButton((MainActivity)getActivity(), ((MainActivity)getActivity()).getRequestWeight().the_weight, userToUpload);
        }
        enableUploadButton = false;
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }

    private void showMessage(int id) {
        showMessage(getString(id));
    }

    private void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(msg)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> dialog.cancel()).create().show();
    }


    private final AdapterView.OnItemSelectedListener oisListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            Log.v(TAG, "WF onItemSelected " + view);
            if (view != null) {
                Log.v(TAG, "WF onItemSelected2 " + view);
                if (getActivity() != null)
                    ((MainActivity)getActivity()).setSelectedUser((User)adapterView.getItemAtPosition(i));
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.fragment_weight_menu, menu);
        if (getActivity() != null)
            usersSpinner = ((MainActivity)getActivity()).addUsersSpinner(menu, oisListener);
        if (!enableUploadButton) {
            MenuItem upload = menu.findItem(R.id.action_weight_upload);
            upload.setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        int itemId = item.getItemId();
        if (itemId == R.id.action_weight) {
            weightButton();
            return true;
        } else if (itemId == R.id.action_weight_upload) {
            if (getActivity() != null)
                uploadButton();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
