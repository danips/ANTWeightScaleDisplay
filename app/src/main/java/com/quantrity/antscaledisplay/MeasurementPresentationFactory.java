package com.quantrity.antscaledisplay;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds UI-independent display values for measurement screens. */
final class MeasurementPresentationFactory {
    interface Strings {
        String get(int resourceId);
        String format(int resourceId, Object... arguments);
    }

    enum Status {
        NEUTRAL,
        WARNING,
        HEALTHY,
        DANGER
    }

    static final class MetricDisplay {
        final Metric metric;
        final boolean available;
        final String primaryText;
        final String secondaryText;
        final Status status;
        final Status compactStatus;
        final double currentValue;
        final double previousValue;

        MetricDisplay(Metric metric, boolean available, String primaryText, String secondaryText,
                      Status status, Status compactStatus, double currentValue,
                      double previousValue) {
            this.metric = metric;
            this.available = available;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.status = status;
            this.compactStatus = compactStatus;
            this.currentValue = currentValue;
            this.previousValue = previousValue;
        }
    }

    static final class SegmentDisplay {
        final BodySegment segment;
        final boolean available;
        final String primaryText;
        final String secondaryText;
        final double currentValue;
        final double previousValue;

        SegmentDisplay(BodySegment segment, boolean available, String primaryText,
                       String secondaryText, double currentValue, double previousValue) {
            this.segment = segment;
            this.available = available;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.currentValue = currentValue;
            this.previousValue = previousValue;
        }
    }

    private final Strings strings;

    MeasurementPresentationFactory(Strings strings) {
        this.strings = strings;
    }

    MetricDisplay metric(Metric metric, User user, Weight weight, Weight previous,
                         int age, boolean male) {
        double value = metric.value(weight);
        double previousValue = previous == null ? -1 : metric.value(previous);
        return metric(metric, user, weight, value, previousValue, age, male);
    }

    MetricDisplay bmi(User user, Weight weight, int heightCentimeters, int age, boolean male) {
        double value = weight.weight == -1 || heightCentimeters <= 0
                ? -1 : weight.weight / Math.pow(heightCentimeters / 100.0, 2);
        return metric(Metric.BMI, user, weight, value, -1, age, male);
    }

    private MetricDisplay metric(Metric metric, User user, Weight weight, double value,
                                 double previousValue, int age, boolean male) {
        if (value == -1) {
            return new MetricDisplay(metric, false, "", "", Status.NEUTRAL,
                    Status.NEUTRAL, value, previousValue);
        }

        String primary = format(metric, user, weight, value);
        Status status = status(metric, weight, value, age, male, false);
        Status compactStatus = status(metric, weight, value, age, male, true);
        String secondary = secondary(metric, user, weight, value, age, male);
        return new MetricDisplay(metric, true, primary, secondary, status, compactStatus,
                value, previousValue);
    }

    SegmentDisplay segment(BodySegment segment, User user, Weight weight, Weight previous) {
        double percent = segment.fatMetric.value(weight);
        double muscle = segment.muscleMetric.value(weight);
        boolean available = percent != -1;
        String primary = "";
        String secondary = "";
        if (available) {
            primary = user.show_fat_mass
                    ? mass(user, weight.weight * percent / 100.0)
                    : strings.format(R.string.weight_fragment_percent_tag, percent);
            if (muscle != -1) secondary = mass(user, muscle);
        }

        double current = user.show_fat_mass ? muscle : percent;
        double previousValue = -1;
        if (previous != null) {
            previousValue = user.show_fat_mass
                    ? segment.muscleMetric.value(previous)
                    : segment.fatMetric.value(previous);
        }
        return new SegmentDisplay(segment, available, primary, secondary, current, previousValue);
    }

    static Set<Metric> availableMetrics(List<Weight> weights) {
        if (weights == null || weights.isEmpty()) return Collections.emptySet();
        EnumSet<Metric> available = EnumSet.noneOf(Metric.class);
        for (Metric metric : Metric.goalMetrics()) {
            Metric source = metric == Metric.BMI ? Metric.WEIGHT : metric;
            for (Weight weight : weights) {
                if (source.value(weight) != -1) {
                    available.add(metric);
                    break;
                }
            }
        }
        return available;
    }

    private String format(Metric metric, User user, Weight weight, double value) {
        Metric.Unit unit = metric.displayedUnit(user.show_fat_mass);
        if (unit == Metric.Unit.MASS) {
            double kilograms = metric.percentageMayBeMass()
                    ? weight.weight * value / 100.0 : value;
            return mass(user, kilograms);
        }
        if (unit == Metric.Unit.PERCENT) {
            return strings.format(R.string.weight_fragment_percent_tag, value);
        }
        if (unit == Metric.Unit.YEARS) {
            return strings.format(R.string.weight_fragment_years_tag, (int) value);
        }
        if (unit == Metric.Unit.ENERGY) {
            return strings.format(R.string.weight_fragment_kcal_tag, value);
        }
        if (metric == Metric.PHYSIQUERATING) return Integer.toString((int) value);
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private String mass(User user, double kilograms) {
        if (user.mass_unit == User.MassUnit.LB) {
            return strings.format(R.string.edit_user_fragment_units_tag_lb,
                    MassConverter.kilogramsToPounds(kilograms));
        }
        if (user.mass_unit == User.MassUnit.ST) {
            MassConverter.StonePounds value = MassConverter.toStonePounds(kilograms);
            if (value.stones == 0) {
                return strings.format(R.string.edit_user_fragment_units_tag_lb,
                        MassConverter.kilogramsToPounds(kilograms));
            }
            return strings.format(R.string.edit_user_fragment_units_tag_st,
                    value.stones, value.pounds);
        }
        return strings.format(R.string.edit_user_fragment_units_tag_kg, kilograms);
    }

    private String secondary(Metric metric, User user, Weight weight, double value,
                             int age, boolean male) {
        if (metric == Metric.PERCENTFAT) {
            int resource = fatDescription(HealthRangeClassifier.getPercentFatDesc(
                    (byte) age, (float) value, male));
            if (resource != 0) return strings.get(resource);
            return user.show_fat_mass
                    ? strings.format(R.string.weight_fragment_percent_tag, value)
                    : mass(user, weight.weight * value / 100.0);
        }
        if (metric == Metric.PERCENTHYDRATION) {
            return mass(user, weight.weight * value / 100.0);
        }
        if (metric == Metric.VISCERALFATRATING) {
            return strings.get(value < 13
                    ? R.string.visceral_fat_sub13 : R.string.visceral_fat_plus13);
        }
        if (metric == Metric.PHYSIQUERATING) {
            int resource = physiqueDescription((int) value);
            return resource == 0 ? "" : strings.get(resource);
        }
        if (metric == Metric.BMI) {
            int resource = bmiDescription(HealthRangeClassifier.getBMIDesc(
                    (byte) age, (float) value, male));
            return resource == 0 ? "" : strings.get(resource);
        }
        return "";
    }

    private static Status status(Metric metric, Weight weight, double value, int age,
                                 boolean male, boolean compact) {
        int classification;
        switch (metric) {
            case BMI:
                classification = HealthRangeClassifier.getBMIDesc((byte) age, (float) value, male);
                return rangeStatus(classification);
            case PERCENTFAT:
                classification = HealthRangeClassifier.getPercentFatDesc(
                        (byte) age, (float) value, male);
                return rangeStatus(classification);
            case PERCENTHYDRATION:
                classification = HealthRangeClassifier.getPercentHydrationDesc((float) value, male);
                return rangeStatus(classification);
            case BONEMASS:
                classification = HealthRangeClassifier.getBoneMassDesc(
                        (float) weight.weight, (float) value, male);
                if (classification == 1) return Status.HEALTHY;
                return compact ? Status.DANGER : Status.WARNING;
            case PHYSIQUERATING:
                int rating = (int) value;
                if (rating >= 1 && rating <= 3) {
                    return compact ? Status.DANGER : Status.WARNING;
                }
                if (!compact && rating == 7) return Status.WARNING;
                return rating >= 4 && rating <= 9 ? Status.HEALTHY : Status.NEUTRAL;
            case VISCERALFATRATING:
                if (!compact) return value < 13 ? Status.HEALTHY : Status.DANGER;
                if (value >= 1 && value <= 12.5) return Status.HEALTHY;
                if (value >= 12.5 && value <= 59) return Status.DANGER;
                return Status.NEUTRAL;
            case METABOLICAGE:
                if (value <= age) return Status.HEALTHY;
                if (compact && value <= age * 1.1) return Status.WARNING;
                return Status.DANGER;
            default:
                return Status.NEUTRAL;
        }
    }

    private static Status rangeStatus(int classification) {
        if (classification == 1) return Status.HEALTHY;
        if (classification == 3) return Status.DANGER;
        if (classification == 0 || classification == 2) return Status.WARNING;
        return Status.NEUTRAL;
    }

    private static int fatDescription(int classification) {
        switch (classification) {
            case 0: return R.string.fat_percent_value_0;
            case 1: return R.string.fat_percent_value_1;
            case 2: return R.string.fat_percent_value_2;
            case 3: return R.string.fat_percent_value_3;
            default: return 0;
        }
    }

    private static int bmiDescription(int classification) {
        switch (classification) {
            case 0: return R.string.bmi_value_0;
            case 1: return R.string.bmi_value_1;
            case 2: return R.string.bmi_value_2;
            case 3: return R.string.bmi_value_3;
            default: return 0;
        }
    }

    private static int physiqueDescription(int rating) {
        switch (rating) {
            case 1: return R.string.physique_rating_1;
            case 2: return R.string.physique_rating_2;
            case 3: return R.string.physique_rating_3;
            case 4: return R.string.physique_rating_4;
            case 5: return R.string.physique_rating_5;
            case 6: return R.string.physique_rating_6;
            case 7: return R.string.physique_rating_7;
            case 8: return R.string.physique_rating_8;
            case 9: return R.string.physique_rating_9;
            default: return 0;
        }
    }
}
