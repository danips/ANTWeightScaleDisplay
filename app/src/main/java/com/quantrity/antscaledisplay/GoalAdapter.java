package com.quantrity.antscaledisplay;

import android.content.Context;
import android.content.res.ColorStateList;
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

import com.quantrity.antscaledisplay.databinding.RowGoalBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {
    private final ArrayList<Goal> dataset;
    private final Context context;
    private final GoalsFragment parent;
    private User user;
    private final Weight lastWeight;
    private final SimpleDateFormat dateFormatter;

    GoalAdapter(ArrayList<Goal> dataset, Context context, User user, Weight lastWeight,
                GoalsFragment parent) {
        this.dataset = new ArrayList<>(dataset);
        this.context = context;
        this.user = user;
        this.lastWeight = lastWeight;
        this.parent = parent;
        dateFormatter = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(context);
        dateFormatter.applyPattern(dateFormatter.toPattern().replaceAll("y", "yy")
                .replaceAll("y{4}", "yy"));
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private Goal goal;
        final ImageView metricIV;
        final TextView startDateTV;
        final TextView endDateTV;
        final TextView startValueTV;
        final TextView endValueTV;
        final TextView totalProgressTV;
        final TextView onTrackProgressTV;

        ViewHolder(RowGoalBinding binding) {
            super(binding.getRoot());
            metricIV = binding.rowGoalMetricIV;
            startDateTV = binding.rowGoalStartDateTV;
            endDateTV = binding.rowGoalEndDateTV;
            startValueTV = binding.rowGoalStartValueTV;
            endValueTV = binding.rowGoalEndValueTV;
            totalProgressTV = binding.rowGoalTotalProgressTV;
            onTrackProgressTV = binding.rowGoalOnTrackProgressTV;
            binding.getRoot().setOnCreateContextMenuListener(this);
            binding.getRoot().setOnClickListener(View::showContextMenu);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle(R.string.users_fragment_user_contextmenu_title);
            MenuItem item = menu.add(0, view.getId(), 0,
                    R.string.users_fragment_user_contextmenu_delete);
            item.setOnMenuItemClickListener(ignored -> {
                remove(goal);
                parent.deleteGoal(goal);
                return true;
            });
            item = menu.add(0, view.getId(), 0,
                    R.string.users_fragment_user_contextmenu_edit);
            item.setOnMenuItemClickListener(ignored -> {
                parent.editGoal(goal);
                return true;
            });
        }
    }

    public void add(int position, Goal item) {
        dataset.add(position, item);
        notifyItemInserted(position);
    }

    private void remove(Goal item) {
        int position = dataset.indexOf(item);
        dataset.remove(item);
        notifyItemRemoved(position);
    }

    void replaceAll(ArrayList<Goal> dataset, User user) {
        this.user = user;
        int oldSize = this.dataset.size();
        this.dataset.clear();
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);
        this.dataset.addAll(dataset);
        if (!dataset.isEmpty()) notifyItemRangeInserted(0, dataset.size());
    }

    public Goal get(Goal item) { return dataset.get(dataset.indexOf(item)); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowGoalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        long now = Calendar.getInstance().getTimeInMillis();
        Goal goal = dataset.get(position);
        holder.goal = goal;
        holder.startDateTV.setText(dateFormatter.format(goal.start_date));
        holder.endDateTV.setText(dateFormatter.format(goal.end_date));
        holder.metricIV.setImageResource(goal.type.getIconRes());
        holder.metricIV.setBackgroundTintList(ColorStateList.valueOf(goal.color));
        holder.onTrackProgressTV.setText("");

        Metric.Unit unit = goal.type.displayedUnit(goal.show_fat_mass);
        double current = goal.type.goalValue(lastWeight, goal.show_fat_mass);
        double expected = ((double) (lastWeight.date - goal.start_date)
                / (goal.end_date - goal.start_date))
                * (goal.end_value - goal.start_value) + goal.start_value;
        double totalProgress = current - goal.start_value;
        double onTrackProgress = current - expected;

        holder.startValueTV.setText(format(goal.start_value, unit, false));
        holder.endValueTV.setText(format(goal.end_value, unit, false));
        holder.totalProgressTV.setText(context.getString(R.string.format_total_sigma,
                totalProgress > 0 ? "+" : "", format(totalProgress, unit, true)));

        boolean active = now <= goal.end_date && now >= goal.start_date;
        if (active) {
            holder.onTrackProgressTV.setText(context.getString(R.string.format_delta,
                    onTrackProgress > 0 ? "+" : "", format(onTrackProgress, unit, true)));
        } else {
            holder.onTrackProgressTV.setText("");
        }

        int upColor = goal.start_value >= goal.end_value ? Color.RED : Color.GREEN;
        int downColor = goal.start_value >= goal.end_value ? Color.GREEN : Color.RED;
        holder.totalProgressTV.setTextColor(totalProgress <= 0 ? downColor : upColor);
        if (active) holder.onTrackProgressTV.setTextColor(onTrackProgress <= 0 ? downColor : upColor);
    }

    private String format(double value, Metric.Unit unit, boolean difference) {
        if (unit == Metric.Unit.MASS) {
            if (difference && user.mass_unit == User.MassUnit.ST && value < 0) {
                return "-" + user.printMass(context, Math.abs(value));
            }
            return user.printMass(context, value);
        }
        if (unit == Metric.Unit.PERCENT) {
            return String.format(context.getString(R.string.weight_fragment_percent_tag), value);
        }
        if (unit == Metric.Unit.YEARS) {
            return String.format(context.getString(R.string.weight_fragment_years_tag)
                    .replace("%1$d", "%1$.1f"), value);
        }
        if (unit == Metric.Unit.ENERGY) {
            return String.format(context.getString(R.string.weight_fragment_kcal_tag), value);
        }
        return String.format(Locale.getDefault(), "%.02f", value);
    }

    @Override
    public int getItemCount() { return dataset.size(); }
}
