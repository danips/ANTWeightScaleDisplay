package com.quantrity.antscaledisplay;

/** Pure goal progress calculation with explicit unavailable and inactive states. */
final class GoalProgress {
    final boolean hasCurrentValue;
    final boolean hasOnTrackValue;
    final double total;
    final double onTrack;

    private GoalProgress(boolean hasCurrentValue, boolean hasOnTrackValue,
                         double total, double onTrack) {
        this.hasCurrentValue = hasCurrentValue;
        this.hasOnTrackValue = hasOnTrackValue;
        this.total = total;
        this.onTrack = onTrack;
    }

    static GoalProgress calculate(Goal goal, Weight latestWeight, long now) {
        double current = goal.type.goalValue(latestWeight, goal.show_fat_mass);
        if (latestWeight == null || current == -1 || !Double.isFinite(current)) {
            return new GoalProgress(false, false, 0, 0);
        }

        double total = current - goal.start_value;
        boolean active = goal.end_date > goal.start_date
                && now >= goal.start_date && now <= goal.end_date;
        if (!active) return new GoalProgress(true, false, total, 0);

        double elapsedFraction = (double) (latestWeight.date - goal.start_date)
                / (goal.end_date - goal.start_date);
        double expected = elapsedFraction * (goal.end_value - goal.start_value)
                + goal.start_value;
        return new GoalProgress(true, true, total, current - expected);
    }
}
