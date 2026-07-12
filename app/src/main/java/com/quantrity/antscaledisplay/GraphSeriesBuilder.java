package com.quantrity.antscaledisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure graph point, rolling-average, visible-average, and goal-series calculations. */
final class GraphSeriesBuilder {
    static final class GoalSeries {
        final GraphPoint start;
        final GraphPoint end;
        final int color;

        GoalSeries(GraphPoint start, GraphPoint end, int color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }

    private GraphSeriesBuilder() {}

    static List<GraphPoint> rawPoints(List<Weight> weights, Metric metric, User user) {
        ArrayList<GraphPoint> points = new ArrayList<>();
        for (Weight weight : weights) {
            double value = metric.graphValue(weight, user);
            if (value != -1) points.add(new GraphPoint(weight.date, value));
        }
        Collections.sort(points, new Comparator<GraphPoint>() {
            @Override public int compare(GraphPoint left, GraphPoint right) {
                return Double.compare(left.x, right.x);
            }
        });
        return points;
    }

    static List<GraphPoint> rollingAverage(List<GraphPoint> points, double windowMillis) {
        ArrayList<GraphPoint> result = new ArrayList<>();
        if (points.isEmpty()) return result;
        double rolling = points.get(0).y;
        double previousTime = points.get(0).x;
        double lastSaved = previousTime;
        result.add(new GraphPoint(previousTime, rolling));
        for (int i = 1; i < points.size(); i++) {
            GraphPoint current = points.get(i);
            double coefficient = Math.exp(-(current.x - previousTime) / windowMillis);
            rolling = (1d - coefficient) * current.y + coefficient * rolling;
            if (current.x - lastSaved >= windowMillis) {
                result.add(new GraphPoint(current.x, rolling));
                lastSaved = current.x;
            }
            previousTime = current.x;
        }
        GraphPoint last = points.get(points.size() - 1);
        if (result.get(result.size() - 1).x != last.x) {
            result.add(new GraphPoint(last.x, rolling));
        }
        return result;
    }

    static double visibleAverage(List<GraphPoint> points, double start, double end) {
        double span = end - start;
        if (span <= 0 || points.size() < 2) return 0;
        double area = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            GraphPoint left = points.get(i);
            GraphPoint right = points.get(i + 1);
            double segmentStart = Math.max(start, left.x);
            double segmentEnd = Math.min(end, right.x);
            if (segmentEnd <= segmentStart) continue;
            double startValue = interpolate(left, right, segmentStart);
            double endValue = interpolate(left, right, segmentEnd);
            area += (startValue + endValue) * (segmentEnd - segmentStart) / 2d;
        }
        return area / span;
    }

    static List<GoalSeries> goalSeries(List<Goal> goals, String userUuid, Metric metric,
                                       boolean showFatMass) {
        ArrayList<GoalSeries> result = new ArrayList<>();
        for (Goal goal : goals) {
            if (!userUuid.equals(goal.uuid) || goal.type != metric) continue;
            if (metric.percentageMayBeMass() && goal.show_fat_mass != showFatMass) continue;
            result.add(new GoalSeries(
                    new GraphPoint(goal.start_date, goal.start_value),
                    new GraphPoint(goal.end_date, goal.end_value), goal.color));
        }
        return result;
    }

    private static double interpolate(GraphPoint left, GraphPoint right, double x) {
        if (right.x == left.x) return right.y;
        return left.y + (right.y - left.y) * (x - left.x) / (right.x - left.x);
    }
}
