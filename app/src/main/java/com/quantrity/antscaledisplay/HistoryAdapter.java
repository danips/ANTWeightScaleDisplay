package com.quantrity.antscaledisplay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.text.format.DateUtils;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantrity.antscaledisplay.databinding.RowWeightBinding;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    //private static final String TAG = "HistoryAdapter";

    private final ArrayList<Weight> mDataset;
    private final Context mContext;
    private final HistoryFragment parent;
    private User user;
    private final SimpleDateFormat dateFormatter;

    // Tracks the expanded/collapsed state of each item position
    private final SparseBooleanArray expandedStates = new SparseBooleanArray();

    // Provide a suitable constructor (depends on the kind of dataset)
    HistoryAdapter(ArrayList<Weight> myDataset, Context mContext, User user,
                   HistoryFragment parent) {
        mDataset = new ArrayList<>(myDataset);
        this.mContext = mContext;
        this.user = user;
        this.parent = parent;
        this.dateFormatter = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(mContext);
        this.dateFormatter.applyPattern(dateFormatter.toPattern().replaceAll("y", "yy").replaceAll("y{4}", "yy"));
    }

    void replaceAll(ArrayList<Weight> myDataset, User user) {
        this.user = user;
        int oldSize = mDataset.size();
        mDataset.clear();
        // Reset expanded states when data changes/reloads
        expandedStates.clear();
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);
        mDataset.addAll(myDataset);
        if (!myDataset.isEmpty()) notifyItemRangeInserted(0, myDataset.size());
    }

    // Provide a reference to the views for each data item
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private Weight weight;

        // Header Views
        final TextView dateTV;
        final TextView timeTV;
        final TextView weightTV;
        final TextView bmiTV;
        final ImageView expandIcon; // New Arrow Icon

        // Details Container
        final View detailsContainer; // The container to hide/show

        // Detail Views (Inside the container)
        final TextView trunkPercentFatTV;
        final TextView trunkMuscleMassTV;
        final TextView leftArmPercentFatTV;
        final TextView leftArmMuscleMassTV;
        final TextView rightArmPercentFatTV;
        final TextView rightArmMuscleMassTV;
        final TextView leftLegPercentFatTV;
        final TextView leftLegMuscleMassTV;
        final TextView rightLegPercentFatTV;
        final TextView rightLegMuscleMassTV;
        final TextView percentFatTV;
        final TextView percentHydrationTV;
        final TextView boneMassTV;
        final TextView muscleMassTV;
        final TextView physiqueRatingTV;
        final TextView visceralFatRatingTV;
        final TextView metabolicAgeTV;
        final TextView basalMetTV;
        final ImageView percentFatIV;
        final ImageView percentHydrationIV;
        final ImageView boneMassIV;
        //final ImageView muscleMassIV;
        final ImageView physiqueRatingIV;
        final ImageView visceralFatRatingIV;
        final ImageView metabolicAgeIV;
        final ImageView basalMetIV;

        final TableRow tftmTR;
        final TableRow laflamTR;
        final TableRow raframTR;
        final TableRow llfllmTR;
        final TableRow rlfrlmTR;
        final TableRow pfphTR;
        final TableRow bmmmTR;
        final TableRow prvfTR;
        final TableRow maamTR;

        ViewHolder(RowWeightBinding binding) {
            super(binding.getRoot());

            // New View References for Expandable Layout
            detailsContainer = binding.rowDetailsContainer;
            expandIcon = binding.expandIcon;

            // Existing View References
            dateTV = binding.rowWeightDateTV;
            timeTV = binding.rowWeightTimeTV;
            weightTV = binding.rowWeightWeightTV;
            bmiTV = binding.rowWeightBmiTV;

            trunkPercentFatTV = binding.rowWeightTrunkPercentFatTV;
            trunkMuscleMassTV = binding.rowWeightTrunkMuscleMassTV;
            leftArmPercentFatTV = binding.rowWeightLeftArmPercentFatTV;
            leftArmMuscleMassTV = binding.rowWeightLeftArmMuscleMassTV;
            rightArmPercentFatTV = binding.rowWeightRightArmPercentFatTV;
            rightArmMuscleMassTV = binding.rowWeightRightArmMuscleMassTV;
            leftLegPercentFatTV = binding.rowWeightLeftLegPercentFatTV;
            leftLegMuscleMassTV = binding.rowWeightLeftLegMuscleMassTV;
            rightLegPercentFatTV = binding.rowWeightRightLegPercentFatTV;
            rightLegMuscleMassTV = binding.rowWeightRightLegMuscleMassTV;
            percentFatTV = binding.rowWeightPercentFatTV;
            percentHydrationTV = binding.rowWeightPercentHydrationTV;
            boneMassTV = binding.rowWeightBoneMassTV;
            muscleMassTV = binding.rowWeightMuscleMassTV;
            physiqueRatingTV = binding.rowWeightPhysiqueRatingTV;
            visceralFatRatingTV = binding.rowWeightVisceralFatRatingTV;
            metabolicAgeTV = binding.rowWeightMetabolicAgeTV;
            basalMetTV = binding.rowWeightBasalMetTV;

            percentFatIV = binding.rowWeightPercentFatIV;
            percentHydrationIV = binding.rowWeightPercentHydrationIV;
            boneMassIV = binding.rowWeightBoneMassIV;
            physiqueRatingIV = binding.rowWeightPhysiqueRatingIV;
            visceralFatRatingIV = binding.rowWeightVisceralFatRatingIV;
            metabolicAgeIV = binding.rowWeightMetabolicAgeIV;
            basalMetIV = binding.rowWeightBasalMetIV;

            tftmTR = binding.tftmTR;
            laflamTR = binding.laflamTR;
            raframTR = binding.raframTR;
            llfllmTR = binding.llfllmTR;
            rlfrlmTR = binding.rlfrlmTR;
            pfphTR = binding.pfphTR;
            bmmmTR = binding.bmmmTR;
            prvfTR = binding.prvfTR;
            maamTR = binding.maamTR;

            binding.getRoot().setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            contextMenu.setHeaderTitle(R.string.users_fragment_user_contextmenu_title);
            MenuItem mi = contextMenu.add(0, view.getId(), 0, R.string.users_fragment_user_contextmenu_delete);
            mi.setOnMenuItemClickListener(menuItem -> {
                int position = mDataset.indexOf(weight);
                if (position != -1) {
                    mDataset.remove(weight);
                    expandedStates.delete(position); // Remove state for deleted item
                    notifyItemRemoved(position);
                    parent.deleteWeight(weight);
                }
                return true;
            });

            if (user != null) {
                if ((user.gc_user != null) && (user.gc_pass != null) && (!user.gc_user.isEmpty()) && (!user.gc_pass.isEmpty())) {
                    mi = contextMenu.add(0, view.getId(), 0, String.format(mContext.getString(R.string.users_fragment_user_contextmenu_upload_to), mContext.getString(R.string.edit_user_fragment_garmin_connect_category)));
                    mi.setOnMenuItemClickListener(menuItem -> {
                        ForegroundUpload upload = new ForegroundUpload(
                                (MainActivity) mContext, weight, user, true, false);
                        upload.execute();
                        return true;
                    });
                }
                if ((user.email_to != null) && !user.email_to.isEmpty()) {
                    mi = contextMenu.add(0, view.getId(), 0, String.format(mContext.getString(R.string.users_fragment_user_contextmenu_upload_to), mContext.getString(R.string.edit_user_fragment_email_category)));
                    mi.setOnMenuItemClickListener(menuItem -> {
                        ForegroundUpload upload = new ForegroundUpload(
                                (MainActivity) mContext, weight, user, false, true);
                        upload.execute();
                        return true;
                    });
                }

                mi = contextMenu.add(0, view.getId(), 0, mContext.getString(R.string.weight_edit_fragment_edit_weight));
                mi.setOnMenuItemClickListener(menuItem -> {
                    parent.editWeight(weight, user);
                    return true;
                });
            }
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowWeightBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Weight item = mDataset.get(position);
        holder.weight = item;
        int age_then = User.calcAge(user.birthdate, item.date);

        // --- EXPANSION LOGIC ---
        final boolean isExpanded = expandedStates.get(position, false);

        // Show/Hide details based on state
        if (holder.detailsContainer != null) {
            holder.detailsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }

        // Rotate arrow icon based on state
        if (holder.expandIcon != null) {
            holder.expandIcon.setRotation(isExpanded ? 180f : 0f);
        }

        // Click Listener for the Card/Row
        holder.itemView.setOnClickListener(v -> {
            // Use getBindingAdapterPosition() instead of the deprecated getAdapterPosition()
            int pos = holder.getBindingAdapterPosition();

            // Always check for NO_POSITION to prevent crashes during animations
            if (pos != RecyclerView.NO_POSITION) {
                boolean newState = !expandedStates.get(pos, false);
                if (newState) {
                    expandedStates.put(pos, true);
                } else {
                    expandedStates.delete(pos);
                }
                // Use notifyItemChanged to animate the specific row update
                notifyItemChanged(pos);
            }
        });
        // -----------------------

        holder.dateTV.setText(dateFormatter.format(item.date));
        holder.timeTV.setText(DateUtils.formatDateTime(mContext, item.date, DateUtils.FORMAT_SHOW_TIME));

        double bmi = item.weight / Math.pow((item.height) / 100, 2);

        holder.bmiTV.setText(MessageFormat.format("{0} {1}", mContext.getString(R.string.weight_fragment_bmi_tag), String.format(Locale.getDefault(), "%.02f", bmi)));

        holder.weightTV.setText(user.printMass(mContext, item.weight));

        // Note: The visibility logic below applies to the internal TableRows.
        // Even if these are set to VISIBLE, they won't show up if detailsContainer is GONE.

        if (item.boneMass != -1) {
            holder.boneMassTV.setText(user.printMass(mContext, item.boneMass));
            switch (HealthRangeClassifier.getBoneMassDesc((float) item.weight, (float) item.boneMass, item.isMale)) {
                case 0: holder.boneMassIV.setBackgroundResource(R.drawable.rounded_red_mini); break;
                case 1: holder.boneMassIV.setBackgroundResource(R.drawable.rounded_green_mini); break;
                default: holder.boneMassIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.boneMassTV.setVisibility(View.VISIBLE);
        } else holder.boneMassTV.setVisibility(View.INVISIBLE);

        if (item.muscleMass != -1) {
            holder.muscleMassTV.setText(user.printMass(mContext, item.muscleMass));
            holder.muscleMassTV.setVisibility(View.VISIBLE);
        } else holder.muscleMassTV.setVisibility(View.INVISIBLE);

        // BMI Color Logic
        Shader textShader;
        int c1, c2;
        switch (HealthRangeClassifier.getBMIDesc((byte) age_then, (float) bmi, item.isMale)) {
            case 0:
            case 2:
                c1 = Color.parseColor("#f3ae1b");
                c2 = Color.parseColor("#bb6008");
                break;
            case 1:
                c1 = Color.parseColor("#70c656");
                c2 = Color.parseColor("#53933f");
                break;
            case 3:
                c1 = Color.parseColor("#ef4444");
                c2 = Color.parseColor("#992f2f");
                break;
            default:
                c1 = Color.parseColor("#33b5e5");
                c2 = Color.parseColor("#2f6699");
                break;
        }
        textShader = new LinearGradient(0, 0, 0, holder.weightTV.getTextSize(),
                new int[]{ c1, c2 }, null, Shader.TileMode.CLAMP);
        holder.weightTV.getPaint().setShader(textShader);

        if (item.percentFat != -1) {
            if (user.show_fat_mass) {
                holder.percentFatTV.setText(user.printMass(mContext, item.weight * item.percentFat / 100));
            } else {
                holder.percentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.percentFat));
            }

            switch (HealthRangeClassifier.getPercentFatDesc((byte) age_then, (float) item.percentFat, item.isMale)) {
                case 0:
                case 2: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_yellow_mini); break;
                case 1: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_green_mini); break;
                case 3: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_red_mini); break;
                default: holder.percentFatIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.percentFatTV.setVisibility(View.VISIBLE);
        } else holder.percentFatTV.setVisibility(View.INVISIBLE);

        if (item.percentHydration != -1) {
            holder.percentHydrationTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), item.percentHydration));
            switch (HealthRangeClassifier.getPercentHydrationDesc((float) item.percentHydration, item.isMale)) {
                case 0:
                case 2: holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_yellow_mini); break;
                case 1: holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_green_mini); break;
                default: holder.percentHydrationIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.percentHydrationTV.setVisibility(View.VISIBLE);
        } else holder.percentHydrationTV.setVisibility(View.INVISIBLE);

        if (item.physiqueRating != -1) {
            holder.physiqueRatingTV.setText(String.format(Locale.getDefault(), "%d", item.physiqueRating));
            switch (item.physiqueRating) {
                case 1: case 2: case 3: holder.physiqueRatingIV.setBackgroundResource(R.drawable.rounded_red_mini); break;
                case 4: case 5: case 6: case 7: case 8: case 9: holder.physiqueRatingIV.setBackgroundResource(R.drawable.rounded_green_mini); break;
            }
            holder.physiqueRatingTV.setVisibility(View.VISIBLE);
        } else holder.physiqueRatingTV.setVisibility(View.INVISIBLE);

        if (item.visceralFatRating != -1) {
            holder.visceralFatRatingTV.setText(String.format(Locale.getDefault(), "%.2f", item.visceralFatRating));
            if ((item.visceralFatRating >= 1.0) && (item.visceralFatRating <= 12.5)) {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_green_mini);
            } else if ((item.visceralFatRating >= 12.5) && (item.visceralFatRating <= 59.0)) {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_red_mini);
            } else {
                holder.visceralFatRatingIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
            holder.visceralFatRatingTV.setVisibility(View.VISIBLE);
        } else holder.visceralFatRatingTV.setVisibility(View.INVISIBLE);

        if (item.metabolicAge != -1) {
            holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_age_mini);
            holder.metabolicAgeTV.setText(String.format(mContext.getString(R.string.weight_fragment_years_tag), item.metabolicAge));
            if (item.metabolicAge <= age_then) holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_green_mini);
            else if (item.metabolicAge <= (age_then) * 1.1) holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_yellow_mini);
            else holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_red_mini);
            holder.metabolicAgeTV.setVisibility(View.VISIBLE);
        } else {
            if (item.activeMet != -1) {
                holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_mini);
                holder.metabolicAgeTV.setText(String.format(mContext.getString(R.string.weight_fragment_kcal_tag), item.activeMet));
                holder.metabolicAgeTV.setVisibility(View.VISIBLE);
            } else holder.metabolicAgeTV.setVisibility(View.INVISIBLE);
        }

        if (item.basalMet != -1) {
            holder.basalMetTV.setText(String.format(mContext.getString(R.string.weight_fragment_kcal_tag), item.basalMet));
            holder.basalMetTV.setVisibility(View.VISIBLE);
            holder.basalMetIV.setVisibility(View.VISIBLE);
        } else {
            holder.basalMetTV.setVisibility(View.INVISIBLE);
            holder.basalMetIV.setVisibility(View.INVISIBLE);
        }

        // Segmental Analysis Rows
        if (item.trunkPercentFat != -1) {
            if (user.show_fat_mass) holder.trunkPercentFatTV.setText(user.printMass(mContext, item.weight * item.trunkPercentFat / 100));
            else holder.trunkPercentFatTV.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) item.trunkPercentFat));
            holder.trunkPercentFatTV.setVisibility(View.VISIBLE);
        } else holder.trunkPercentFatTV.setVisibility(View.INVISIBLE);

        if (item.trunkMuscleMass != -1) {
            holder.trunkMuscleMassTV.setText(user.printMass(mContext, item.trunkMuscleMass));
            holder.trunkMuscleMassTV.setVisibility(View.VISIBLE);
        } else holder.trunkMuscleMassTV.setVisibility(View.INVISIBLE);

        // Arms and Legs logic (same pattern as above, kept concise here)
        updateSegmentalView(holder.leftArmPercentFatTV, item.leftArmPercentFat, item.weight, user);
        updateSegmentalView(holder.rightArmPercentFatTV, item.rightArmPercentFat, item.weight, user);
        updateSegmentalView(holder.leftLegPercentFatTV, item.leftLegPercentFat, item.weight, user);
        updateSegmentalView(holder.rightLegPercentFatTV, item.rightLegPercentFat, item.weight, user);

        if (item.leftArmMuscleMass != -1) { holder.leftArmMuscleMassTV.setText(user.printMass(mContext, item.leftArmMuscleMass)); holder.leftArmMuscleMassTV.setVisibility(View.VISIBLE); } else holder.leftArmMuscleMassTV.setVisibility(View.INVISIBLE);
        if (item.rightArmMuscleMass != -1) { holder.rightArmMuscleMassTV.setText(user.printMass(mContext, item.rightArmMuscleMass)); holder.rightArmMuscleMassTV.setVisibility(View.VISIBLE); } else holder.rightArmMuscleMassTV.setVisibility(View.INVISIBLE);
        if (item.leftLegMuscleMass != -1) { holder.leftLegMuscleMassTV.setText(user.printMass(mContext, item.leftLegMuscleMass)); holder.leftLegMuscleMassTV.setVisibility(View.VISIBLE); } else holder.leftLegMuscleMassTV.setVisibility(View.INVISIBLE);
        if (item.rightLegMuscleMass != -1) { holder.rightLegMuscleMassTV.setText(user.printMass(mContext, item.rightLegMuscleMass)); holder.rightLegMuscleMassTV.setVisibility(View.VISIBLE); } else holder.rightLegMuscleMassTV.setVisibility(View.INVISIBLE);

        // Hiding entire rows if data is missing (Standard logic preserved)
        if ((item.boneMass == -1) && (item.muscleMass == -1)) holder.bmmmTR.setVisibility(View.GONE); else holder.bmmmTR.setVisibility(View.VISIBLE);
        if ((item.trunkPercentFat == -1) && (item.trunkMuscleMass == -1)) holder.tftmTR.setVisibility(View.GONE); else holder.tftmTR.setVisibility(View.VISIBLE);
        if ((item.leftArmPercentFat == -1) && (item.leftArmMuscleMass == -1)) holder.laflamTR.setVisibility(View.GONE); else holder.laflamTR.setVisibility(View.VISIBLE);
        if ((item.rightArmPercentFat == -1) && (item.rightArmMuscleMass == -1)) holder.raframTR.setVisibility(View.GONE); else holder.raframTR.setVisibility(View.VISIBLE);
        if ((item.leftArmPercentFat == -1) && (item.leftLegMuscleMass == -1)) holder.llfllmTR.setVisibility(View.GONE); else holder.llfllmTR.setVisibility(View.VISIBLE);
        if ((item.rightArmPercentFat == -1) && (item.rightLegMuscleMass == -1)) holder.rlfrlmTR.setVisibility(View.GONE); else holder.rlfrlmTR.setVisibility(View.VISIBLE);
        if ((item.percentFat == -1) && (item.percentHydration == -1)) holder.pfphTR.setVisibility(View.GONE); else holder.pfphTR.setVisibility(View.VISIBLE);
        if ((item.physiqueRating == -1) && (item.visceralFatRating == -1)) holder.prvfTR.setVisibility(View.GONE); else holder.prvfTR.setVisibility(View.VISIBLE);
        if ((item.metabolicAge == -1) && (item.activeMet == -1) && (item.basalMet == -1)) holder.maamTR.setVisibility(View.GONE); else holder.maamTR.setVisibility(View.VISIBLE);
    }

    private void updateSegmentalView(TextView tv, double val, double weight, User user) {
        if (val != -1) {
            if (user.show_fat_mass) tv.setText(user.printMass(mContext, weight * val / 100));
            else tv.setText(String.format(mContext.getString(R.string.weight_fragment_percent_tag), (float) val));
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() { return mDataset.size(); }
}
