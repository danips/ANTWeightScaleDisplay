package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class GraphCalculationsTest {
    private static final double DAY = GraphPeriod.DAY_MILLIS;
    private static final double EPSILON = 0.0001;

    @Test public void everyPeriodMenuIdMapsThroughTheDefinition() {
        for (GraphPeriod period : GraphPeriod.values()) {
            assertEquals(period, GraphPeriod.fromMenuId(period.menuId));
            assertTrue(GraphPeriod.isPeriodMenuId(period.menuId));
        }
        assertEquals(GraphPeriod.MONTH, GraphPeriod.fromMenuId(-1));
        assertFalse(GraphPeriod.isPeriodMenuId(-1));
    }

    @Test public void periodsDefineViewportButDoNotFilterRawPoints() {
        User user = user(User.MassUnit.KG, false);
        List<GraphPoint> points = GraphSeriesBuilder.rawPoints(Arrays.asList(
                weight(100, 70, -1), weight(0, 80, -1)), Metric.WEIGHT, user);
        assertEquals(2, points.size());
        assertEquals(0, points.get(0).x, 0);
        assertEquals(100, points.get(1).x, 0);
        assertEquals(7, GraphPeriod.WEEK.windowDays(0, 100), 0);
    }

    @Test public void rawPointsSkipMissingMetricsAndConvertFatMass() {
        User user = user(User.MassUnit.LB, true);
        List<GraphPoint> points = GraphSeriesBuilder.rawPoints(Arrays.asList(
                weight(2, 100, 20), weight(1, 100, -1)), Metric.PERCENTFAT, user);
        assertEquals(1, points.size());
        assertEquals(MassConverter.kilogramsToPounds(20), points.get(0).y, EPSILON);
    }

    @Test public void rollingAverageHandlesEmptyOnePointAndSparseSeries() {
        assertTrue(GraphSeriesBuilder.rollingAverage(Collections.emptyList(), DAY).isEmpty());
        List<GraphPoint> one = GraphSeriesBuilder.rollingAverage(
                Collections.singletonList(new GraphPoint(0, 10)), DAY);
        assertEquals(1, one.size());
        assertEquals(10, one.get(0).y, 0);

        List<GraphPoint> rolling = GraphSeriesBuilder.rollingAverage(Arrays.asList(
                new GraphPoint(0, 10), new GraphPoint(DAY, 20),
                new GraphPoint(3 * DAY, 40)), DAY);
        assertEquals(3, rolling.size());
        double second = (1 - Math.exp(-1)) * 20 + Math.exp(-1) * 10;
        assertEquals(second, rolling.get(1).y, EPSILON);
    }

    @Test public void visibleAverageUsesTrapezoidsAndInterpolatedBoundaries() {
        List<GraphPoint> points = Arrays.asList(
                new GraphPoint(0, 0), new GraphPoint(10, 10), new GraphPoint(20, 0));
        assertEquals(5, GraphSeriesBuilder.visibleAverage(points, 0, 20), EPSILON);
        assertEquals(7.5, GraphSeriesBuilder.visibleAverage(points, 5, 15), EPSILON);
        assertEquals(2.5, GraphSeriesBuilder.visibleAverage(points, -10, 10), EPSILON);
    }

    @Test public void goalSeriesFiltersUserMetricAndFatMassPreference() {
        Goal matching = goal("a", Metric.PERCENTFAT, true, 1);
        Goal wrongPreference = goal("a", Metric.PERCENTFAT, false, 2);
        Goal wrongUser = goal("b", Metric.PERCENTFAT, true, 3);
        Goal wrongMetric = goal("a", Metric.WEIGHT, true, 4);
        List<GraphSeriesBuilder.GoalSeries> result = GraphSeriesBuilder.goalSeries(
                Arrays.asList(matching, wrongPreference, wrongUser, wrongMetric),
                "a", Metric.PERCENTFAT, true);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).color);
    }

    @Test public void periodAvailabilityPreservesExistingMenuThresholds() {
        assertFalse(GraphPeriod.TWO_WEEKS.isAvailable(30L * (long) DAY));
        assertTrue(GraphPeriod.TWO_WEEKS.isAvailable(31L * (long) DAY));
        assertFalse(GraphPeriod.MONTH.isAvailable(180L * (long) DAY));
        assertTrue(GraphPeriod.SIX_WEEKS.isAvailable(366L * (long) DAY));
    }

    private static User user(User.MassUnit unit, boolean showFatMass) {
        User user = new User();
        user.mass_unit = unit;
        user.show_fat_mass = showFatMass;
        return user;
    }

    private static Weight weight(long date, double mass, double fat) {
        Weight weight = new Weight();
        weight.date = date;
        weight.weight = mass;
        weight.percentFat = fat;
        return weight;
    }

    private static Goal goal(String uuid, Metric metric, boolean fatMass, int color) {
        Goal goal = new Goal();
        goal.uuid = uuid;
        goal.type = metric;
        goal.show_fat_mass = fatMass;
        goal.start_date = 10;
        goal.end_date = 20;
        goal.start_value = 1;
        goal.end_value = 2;
        goal.color = color;
        return goal;
    }
}
