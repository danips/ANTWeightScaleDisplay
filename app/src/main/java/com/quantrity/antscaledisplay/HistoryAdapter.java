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

class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    //private static final String TAG = "HistoryAdapter";

    private final ArrayList<Weight> mDataset;
    private final Context mContext;
    private final HistoryFragment parent;
    private User user;
    private final SimpleDateFormat dateFormatter;
    private final MeasurementPresentationFactory presentationFactory;

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
        this.presentationFactory = new MeasurementPresentationFactory(
                new MeasurementPresentationFactory.Strings() {
                    @Override public String get(int resourceId) {
                        return HistoryAdapter.this.mContext.getString(resourceId);
                    }

                    @Override public String format(int resourceId, Object... arguments) {
                        return HistoryAdapter.this.mContext.getString(resourceId, arguments);
                    }
                });
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

        MeasurementPresentationFactory.MetricDisplay bmi = presentationFactory.metric(
                Metric.BMI, user, item, null, age_then, item.isMale);
        MeasurementPresentationFactory.MetricDisplay weight = presentationFactory.metric(
                Metric.WEIGHT, user, item, null, age_then, item.isMale);
        holder.bmiTV.setText(MessageFormat.format("{0} {1}",
                mContext.getString(R.string.weight_fragment_bmi_tag), bmi.primaryText));
        holder.weightTV.setText(weight.primaryText);

        // Note: The visibility logic below applies to the internal TableRows.
        // Even if these are set to VISIBLE, they won't show up if detailsContainer is GONE.

        bindCompact(holder.boneMassTV, holder.boneMassIV, presentationFactory.metric(
                Metric.BONEMASS, user, item, null, age_then, item.isMale));
        bindCompact(holder.muscleMassTV, null, presentationFactory.metric(
                Metric.MUSCLEMASS, user, item, null, age_then, item.isMale));

        // BMI Color Logic
        Shader textShader;
        int c1, c2;
        switch (bmi.compactStatus) {
            case WARNING:
                c1 = Color.parseColor("#f3ae1b");
                c2 = Color.parseColor("#bb6008");
                break;
            case HEALTHY:
                c1 = Color.parseColor("#70c656");
                c2 = Color.parseColor("#53933f");
                break;
            case DANGER:
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

        bindCompact(holder.percentFatTV, holder.percentFatIV, presentationFactory.metric(
                Metric.PERCENTFAT, user, item, null, age_then, item.isMale));
        bindCompact(holder.percentHydrationTV, holder.percentHydrationIV,
                presentationFactory.metric(Metric.PERCENTHYDRATION, user, item, null,
                        age_then, item.isMale));
        bindCompact(holder.physiqueRatingTV, holder.physiqueRatingIV,
                presentationFactory.metric(Metric.PHYSIQUERATING, user, item, null,
                        age_then, item.isMale));
        bindCompact(holder.visceralFatRatingTV, holder.visceralFatRatingIV,
                presentationFactory.metric(Metric.VISCERALFATRATING, user, item, null,
                        age_then, item.isMale));

        MeasurementPresentationFactory.MetricDisplay metabolicAge = presentationFactory.metric(
                Metric.METABOLICAGE, user, item, null, age_then, item.isMale);
        if (metabolicAge.available) {
            holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_age_mini);
            bindCompact(holder.metabolicAgeTV, holder.metabolicAgeIV, metabolicAge);
        } else {
            MeasurementPresentationFactory.MetricDisplay activeMet = presentationFactory.metric(
                    Metric.ACTIVEMET, user, item, null, age_then, item.isMale);
            if (activeMet.available) {
                holder.metabolicAgeIV.setImageResource(R.drawable.ic_metabolic_mini);
                bindCompact(holder.metabolicAgeTV, holder.metabolicAgeIV, activeMet);
            } else {
                holder.metabolicAgeTV.setText("");
                holder.metabolicAgeTV.setVisibility(View.INVISIBLE);
                holder.metabolicAgeIV.setBackgroundResource(R.drawable.rounded_blue_mini);
            }
        }

        MeasurementPresentationFactory.MetricDisplay basalMet = presentationFactory.metric(
                Metric.BASALMET, user, item, null, age_then, item.isMale);
        bindCompact(holder.basalMetTV, holder.basalMetIV, basalMet);
        holder.basalMetIV.setVisibility(basalMet.available ? View.VISIBLE : View.INVISIBLE);

        for (BodySegment segment : BodySegment.values()) {
            MeasurementPresentationFactory.MetricDisplay fat = presentationFactory.metric(
                    segment.fatMetric, user, item, null, age_then, item.isMale);
            MeasurementPresentationFactory.MetricDisplay muscle = presentationFactory.metric(
                    segment.muscleMetric, user, item, null, age_then, item.isMale);
            bindCompact(segmentFatView(holder, segment), null, fat);
            bindCompact(segmentMuscleView(holder, segment), null, muscle);
            segmentRow(holder, segment).setVisibility(
                    fat.available || muscle.available ? View.VISIBLE : View.GONE);
        }

        holder.bmmmTR.setVisibility(item.boneMass != -1 || item.muscleMass != -1
                ? View.VISIBLE : View.GONE);
        holder.pfphTR.setVisibility(item.percentFat != -1 || item.percentHydration != -1
                ? View.VISIBLE : View.GONE);
        holder.prvfTR.setVisibility(item.physiqueRating != -1 || item.visceralFatRating != -1
                ? View.VISIBLE : View.GONE);
        holder.maamTR.setVisibility(item.metabolicAge != -1 || item.activeMet != -1
                || item.basalMet != -1 ? View.VISIBLE : View.GONE);
    }

    private void bindCompact(TextView valueView, ImageView iconView,
                             MeasurementPresentationFactory.MetricDisplay display) {
        valueView.setText(display.available ? display.primaryText : "");
        valueView.setVisibility(display.available ? View.VISIBLE : View.INVISIBLE);
        if (iconView != null) setCompactStatus(iconView, display.compactStatus);
    }

    private static void setCompactStatus(ImageView icon,
                                         MeasurementPresentationFactory.Status status) {
        if (status == MeasurementPresentationFactory.Status.HEALTHY) {
            icon.setBackgroundResource(R.drawable.rounded_green_mini);
        } else if (status == MeasurementPresentationFactory.Status.WARNING) {
            icon.setBackgroundResource(R.drawable.rounded_yellow_mini);
        } else if (status == MeasurementPresentationFactory.Status.DANGER) {
            icon.setBackgroundResource(R.drawable.rounded_red_mini);
        } else {
            icon.setBackgroundResource(R.drawable.rounded_blue_mini);
        }
    }

    private static TextView segmentFatView(ViewHolder holder, BodySegment segment) {
        switch (segment) {
            case TRUNK: return holder.trunkPercentFatTV;
            case LEFT_ARM: return holder.leftArmPercentFatTV;
            case RIGHT_ARM: return holder.rightArmPercentFatTV;
            case LEFT_LEG: return holder.leftLegPercentFatTV;
            case RIGHT_LEG: return holder.rightLegPercentFatTV;
            default: throw new IllegalArgumentException("Unknown segment " + segment);
        }
    }

    private static TextView segmentMuscleView(ViewHolder holder, BodySegment segment) {
        switch (segment) {
            case TRUNK: return holder.trunkMuscleMassTV;
            case LEFT_ARM: return holder.leftArmMuscleMassTV;
            case RIGHT_ARM: return holder.rightArmMuscleMassTV;
            case LEFT_LEG: return holder.leftLegMuscleMassTV;
            case RIGHT_LEG: return holder.rightLegMuscleMassTV;
            default: throw new IllegalArgumentException("Unknown segment " + segment);
        }
    }

    private static TableRow segmentRow(ViewHolder holder, BodySegment segment) {
        switch (segment) {
            case TRUNK: return holder.tftmTR;
            case LEFT_ARM: return holder.laflamTR;
            case RIGHT_ARM: return holder.raframTR;
            case LEFT_LEG: return holder.llfllmTR;
            case RIGHT_LEG: return holder.rlfrlmTR;
            default: throw new IllegalArgumentException("Unknown segment " + segment);
        }
    }

    @Override
    public int getItemCount() { return mDataset.size(); }
}
