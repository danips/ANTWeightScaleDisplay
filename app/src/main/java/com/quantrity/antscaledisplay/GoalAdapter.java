package com.quantrity.antscaledisplay;


import android.content.Context;
import android.graphics.Color;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {
    private static final String TAG = "GoalAdapter";

    private ArrayList<Goal> mDataset;
    private final Context mContext;

    private User user;
    private final Weight last_weight;

    private final SimpleDateFormat dateFormatter;

    // Provide a suitable constructor (depends on the kind of dataset)
    GoalAdapter(ArrayList<Goal> myDataset, Context mContext, User user, Weight last_weight) {
        mDataset = myDataset;
        this.mContext = mContext;
        this.user = user;
        this.dateFormatter = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(mContext);
        this.dateFormatter.applyPattern(dateFormatter.toPattern().replaceAll("y", "yy").replaceAll("y{4}", "yy"));
        this.last_weight = last_weight;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        // each data item is just a string in this case
        private Goal goal;
        final ImageView metricIV;
        final TextView startDateTV;
        final TextView endDateTV;
        final TextView startValueTV;
        final TextView endValueTV;
        final TextView totalProgressTV;
        final TextView onTrackProgressTV;

        ViewHolder(View v) {
            super(v);

            metricIV =  v.findViewById(R.id.row_goal_metricIV);
            startDateTV = v.findViewById(R.id.row_goal_startDateTV);
            endDateTV = v.findViewById(R.id.row_goal_endDateTV);
            startValueTV = v.findViewById(R.id.row_goal_startValueTV);
            endValueTV = v.findViewById(R.id.row_goal_endValueTV);
            totalProgressTV = v.findViewById(R.id.row_goal_totalProgressTV);
            onTrackProgressTV = v.findViewById(R.id.row_goal_onTrackProgressTV);

            v.setOnCreateContextMenuListener(this);
            v.setOnClickListener(View::showContextMenu);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            //Log.v(TAG, contextMenu + "__" + view + ".." + contextMenuInfo);
            contextMenu.setHeaderTitle(R.string.users_fragment_user_contextmenu_title);
            MenuItem mi = contextMenu.add(0, view.getId(), 0, R.string.users_fragment_user_contextmenu_delete);//groupId, itemId, order, title
            mi.setOnMenuItemClickListener(menuItem -> {
                remove(goal);
                ((MainActivity)mContext).deleteGoal(goal);
                return true;
            });

            mi = contextMenu.add(0, view.getId(), 0, mContext.getString(R.string.users_fragment_user_contextmenu_edit));
            mi.setOnMenuItemClickListener(menuItem -> {
                ((MainActivity)mContext).openEditGoalFragment(goal);
                return true;
            });
        }
    }

    public void add(int position, Goal item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    private void remove(Goal item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    void replaceAll(ArrayList<Goal> myDataset, User user) {
        this.user = user;
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    public Goal get(Goal item) {
        return mDataset.get(mDataset.indexOf(item));
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Log.v("TAG", "onCreateViewHolder");
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_goal, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //Log.v("TAG", "onBindViewHolder");
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        long now = Calendar.getInstance().getTime().getTime();
        final Goal item = mDataset.get(position);

        holder.goal = item;
        holder.startDateTV.setText(dateFormatter.format(item.start_date));
        holder.endDateTV.setText(dateFormatter.format(item.end_date));
        holder.metricIV.setImageResource(Metric.getRes(item.type));
        holder.metricIV.setBackgroundColor(item.color);

        int upColor, downColor;
        if (item.start_value >= item.end_value)
        {
            upColor = Color.RED;
            downColor = Color.GREEN;
        }
        else
        {
            upColor = Color.GREEN;
            downColor = Color.RED;
        }

        double totalProgress = 0, onTrackProgress = 0;
        double mass = -1;
        double percent = -1;
        double expectedProgress;
        switch (item.type)
        {
            case BMI:
                holder.startValueTV.setText(String.format("%.02f", item.start_value));
                holder.endValueTV.setText(String.format("%.02f", item.end_value));
                double bmi = (last_weight.weight / Math.pow(last_weight.height / 100, 2));
                totalProgress = bmi - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = bmi - expectedProgress;
                holder.totalProgressTV.setText(String.format("%.02f", totalProgress));
                Log.v(TAG, now + " " + item.end_date + " " + item.start_date);
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format("%.02f", onTrackProgress));
                }
                break;

            case PHYSIQUERATING:
                holder.startValueTV.setText(String.format("%.02f", item.start_value));
                holder.endValueTV.setText(String.format("%.02f", item.end_value));
                totalProgress = last_weight.physiqueRating - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = last_weight.physiqueRating - expectedProgress;
                holder.totalProgressTV.setText(String.format("%.02f", totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format("%.02f", onTrackProgress));
                }
                break;

            case VISCERALFATRATING:
                holder.startValueTV.setText(String.format("%.02f", item.start_value));
                holder.endValueTV.setText(String.format("%.02f", item.end_value));
                totalProgress = last_weight.visceralFatRating - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = last_weight.visceralFatRating - expectedProgress;
                holder.totalProgressTV.setText(String.format("%.02f", totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format("%.02f", onTrackProgress));
                }
                break;

            case METABOLICAGE:
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag).replace("%1$d", "%1$.1f"), item.start_value));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag).replace("%1$d", "%1$.1f"), item.end_value));
                totalProgress = last_weight.metabolicAge - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = last_weight.metabolicAge - expectedProgress;
                holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag).replace("%1$d", "%1$.1f"), totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_years_tag).replace("%1$d", "%1$.1f"), onTrackProgress));
                }
                break;

            case ACTIVEMET:
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), item.start_value));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), item.end_value));
                totalProgress = last_weight.activeMet - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = last_weight.activeMet - expectedProgress;
                holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), onTrackProgress));
                }
                break;

            case BASALMET:
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), item.start_value));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), item.end_value));
                totalProgress = last_weight.basalMet - item.start_value;
                expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
                onTrackProgress = last_weight.basalMet - expectedProgress;
                holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_kcal_tag), onTrackProgress));
                }
                break;

            case PERCENTHYDRATION:
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), item.start_value));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), item.end_value));
                percent = last_weight.percentHydration;
                break;

            case PERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.percentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.percentFat;
                }
                break;

            case TRUNKPERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.trunkPercentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.trunkPercentFat;
                }
                break;

            case LEFTARMPERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.leftArmPercentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.leftArmPercentFat;
                }
                break;

            case RIGHTARMPERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.rightArmPercentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.rightArmPercentFat;
                }
                break;

            case LEFTLEGPERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.leftLegPercentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.leftLegPercentFat;
                }
                break;

            case RIGHTLEGPERCENTFAT:
                if (item.show_fat_mass) {
                    mass = last_weight.rightLegPercentFat * last_weight.weight / 100;
                }
                else
                {
                    percent = last_weight.rightLegPercentFat;
                }
                break;

            case BONEMASS:
                mass = last_weight.boneMass;
                break;

            case MUSCLEMASS:
                mass = last_weight.muscleMass;
                break;

            case TRUNKMUSCLEMASS:
                mass = last_weight.trunkMuscleMass;
                break;

            case LEFTARMMUSCLEMASS:
                mass = last_weight.leftArmMuscleMass;
                break;

            case RIGHTARMMUSCLEMASS:
                mass = last_weight.rightArmMuscleMass;
                break;

            case LEFTLEGMUSCLEMASS:
                mass = last_weight.leftLegMuscleMass;
                break;

            case RIGHTLEGMUSCLEMASS:
                mass = last_weight.rightLegMuscleMass;
                break;

            case WEIGHT:
            default:
                mass = last_weight.weight;
                break;
        }
        if (percent != -1)
        {
            totalProgress = percent - item.start_value;
            expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
            onTrackProgress = percent - expectedProgress;
            holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), item.start_value));
            holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), item.end_value));
            holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), totalProgress));
            if ((now <= item.end_date) && (now >= item.start_date)) {
                holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.weight_fragment_percent_tag), onTrackProgress));
            }
        }
        if (mass != -1)
        {
            totalProgress = mass - item.start_value;
            expectedProgress = ((double)(last_weight.date - item.start_date) / (item.end_date - item.start_date)) * (item.end_value - item.start_value) + item.start_value;
            onTrackProgress = mass - expectedProgress;
            if (user.mass_unit == User.MassUnit.LB) {
                holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_lb), totalProgress * 2.20462262));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_lb), onTrackProgress * 2.20462262));
                }
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_lb), item.start_value * 2.20462262));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_lb), item.end_value * 2.20462262));
            } else if (user.mass_unit == User.MassUnit.ST) {
                double lbs = item.start_value * 2.20462262;
                double divisor = (float)Math.floor(lbs / 14);
                double remainder = lbs % 14;
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_st), divisor, remainder));
                lbs = item.end_value * 2.20462262;
                divisor = (float)Math.floor(lbs / 14);
                remainder = lbs % 14;
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_st), divisor, remainder));
                String sign = (totalProgress > 0) ? "+" : "-";
                lbs = Math.abs(totalProgress * 2.20462262);
                divisor = (float)Math.floor(lbs / 14);
                remainder = lbs % 14;
                holder.totalProgressTV.setText(sign + String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_st), divisor, remainder));
                sign = (onTrackProgress > 0) ? "+" : "-";
                lbs = Math.abs(onTrackProgress * 2.20462262);
                divisor = (float)Math.floor(lbs / 14);
                remainder = lbs % 14;
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(sign + String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_st), divisor, remainder));
                }
            } else {
                holder.totalProgressTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_kg), totalProgress));
                if ((now <= item.end_date) && (now >= item.start_date)) {
                    holder.onTrackProgressTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_kg), onTrackProgress));
                }
                holder.startValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_kg), item.start_value));
                holder.endValueTV.setText(String.format(mContext.getResources().getString(R.string.edit_user_fragment_units_tag_kg), item.end_value));
            }
        }
        if (totalProgress <= 0)
        {
            holder.totalProgressTV.setTextColor(downColor);
            holder.totalProgressTV.setText("Σ " + holder.totalProgressTV.getText());
        }
        else
        {
            holder.totalProgressTV.setTextColor(upColor);
            holder.totalProgressTV.setText("Σ +" + holder.totalProgressTV.getText());
        }
        if (holder.onTrackProgressTV.getText().length() != 0) {
            if (onTrackProgress <= 0) {
                holder.onTrackProgressTV.setTextColor(downColor);
                holder.onTrackProgressTV.setText("Δ " + holder.onTrackProgressTV.getText());
            } else {
                holder.onTrackProgressTV.setTextColor(upColor);
                holder.onTrackProgressTV.setText("Δ +" + holder.onTrackProgressTV.getText());
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() { return mDataset.size(); }

}
