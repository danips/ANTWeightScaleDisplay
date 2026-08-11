package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GoalProgressTest {
    @Test
    public void noMeasurementHasExplicitUnavailableState() {
        GoalProgress progress = GoalProgress.calculate(goal(1000, 2000), null, 1500);

        assertFalse(progress.hasCurrentValue);
        assertFalse(progress.hasOnTrackValue);
    }

    @Test
    public void measurementWithoutGoalMetricIsUnavailable() {
        Goal goal = goal(1000, 2000);
        goal.type = Metric.PERCENTFAT;
        Weight weight = weight(1500, 90);

        GoalProgress progress = GoalProgress.calculate(goal, weight, 1500);

        assertFalse(progress.hasCurrentValue);
        assertFalse(progress.hasOnTrackValue);
    }

    @Test
    public void selectedProfilesUseTheirOwnLatestValuesAndDates() {
        Goal goal = goal(1000, 2000);
        Weight firstProfile = weight(1500, 90);
        Weight secondProfile = weight(1750, 70);

        GoalProgress first = GoalProgress.calculate(goal, firstProfile, 1500);
        GoalProgress second = GoalProgress.calculate(goal, secondProfile, 1500);

        assertEquals(-10, first.total, 0);
        assertEquals(0, first.onTrack, 0);
        assertEquals(-30, second.total, 0);
        assertEquals(-15, second.onTrack, 0);
    }

    @Test
    public void inactiveGoalKeepsTotalButOmitsOnTrackValue() {
        GoalProgress progress = GoalProgress.calculate(
                goal(1000, 2000), weight(1500, 90), 2001);

        assertTrue(progress.hasCurrentValue);
        assertFalse(progress.hasOnTrackValue);
        assertEquals(-10, progress.total, 0);
    }

    @Test
    public void equalOrInvertedDatesNeverProduceOnTrackValue() {
        GoalProgress equal = GoalProgress.calculate(
                goal(1000, 1000), weight(1000, 90), 1000);
        GoalProgress inverted = GoalProgress.calculate(
                goal(2000, 1000), weight(1500, 90), 1500);

        assertTrue(equal.hasCurrentValue);
        assertFalse(equal.hasOnTrackValue);
        assertEquals(-10, equal.total, 0);
        assertTrue(inverted.hasCurrentValue);
        assertFalse(inverted.hasOnTrackValue);
        assertEquals(-10, inverted.total, 0);
    }

    private static Goal goal(long start, long end) {
        Goal goal = new Goal();
        goal.type = Metric.WEIGHT;
        goal.start_date = start;
        goal.end_date = end;
        goal.start_value = 100;
        goal.end_value = 80;
        return goal;
    }

    private static Weight weight(long date, double value) {
        Weight weight = new Weight();
        weight.date = date;
        weight.weight = value;
        return weight;
    }
}
