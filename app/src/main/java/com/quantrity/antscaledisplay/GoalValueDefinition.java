package com.quantrity.antscaledisplay;

import java.util.Locale;

/** Android-independent definition of how a goal value is displayed and stored. */
final class GoalValueDefinition {
    enum Mode { UNITLESS, SINGLE_UNIT, STONE_POUNDS }

    final Mode mode;
    final int decimals;
    final int primaryUnitResource;
    final int secondaryUnitResource;
    private final User.MassUnit massUnit;
    private final boolean mass;

    private GoalValueDefinition(Mode mode, int decimals, int primaryUnitResource,
                                int secondaryUnitResource, User.MassUnit massUnit, boolean mass) {
        this.mode = mode;
        this.decimals = decimals;
        this.primaryUnitResource = primaryUnitResource;
        this.secondaryUnitResource = secondaryUnitResource;
        this.massUnit = massUnit;
        this.mass = mass;
    }

    static GoalValueDefinition forMetric(Metric metric, User.MassUnit massUnit,
                                         boolean showFatMass) {
        Metric.Unit unit = metric.displayedUnit(showFatMass);
        if (unit == Metric.Unit.MASS) {
            if (massUnit == User.MassUnit.ST) {
                return new GoalValueDefinition(Mode.STONE_POUNDS, 1,
                        R.string.weight_edit_fragment_st_tag,
                        R.string.weight_edit_fragment_lb_tag, massUnit, true);
            }
            return new GoalValueDefinition(Mode.SINGLE_UNIT, 1,
                    massUnit == User.MassUnit.LB
                            ? R.string.weight_edit_fragment_lb_tag
                            : R.string.weight_edit_fragment_kg_tag,
                    0, massUnit, true);
        }
        if (unit == Metric.Unit.NONE) {
            return new GoalValueDefinition(Mode.UNITLESS,
                    metric == Metric.PHYSIQUERATING ? 0 : 1, 0, 0, massUnit, false);
        }
        int resource = unit == Metric.Unit.PERCENT
                ? R.string.weight_edit_fragment_percent_tag
                : unit == Metric.Unit.YEARS
                ? R.string.weight_edit_fragment_years_tag
                : R.string.weight_edit_fragment_kcal_tag;
        int decimals = unit == Metric.Unit.PERCENT ? 1 : 0;
        return new GoalValueDefinition(Mode.SINGLE_UNIT, decimals, resource, 0, massUnit, false);
    }

    String format(double canonicalValue) {
        double displayed = mass ? MassConverter.toDisplayMass(canonicalValue, massUnit)
                : canonicalValue;
        return String.format(Locale.getDefault(), "%1$." + decimals + "f", displayed);
    }

    String formatStones(double canonicalValue) {
        return String.format(Locale.getDefault(), "%.0f",
                MassConverter.toStonePounds(canonicalValue).stones);
    }

    String formatPounds(double canonicalValue) {
        return String.format(Locale.getDefault(), "%.1f",
                MassConverter.toStonePounds(canonicalValue).pounds);
    }

    double toCanonical(double displayedValue) {
        return mass ? MassConverter.toKilograms(displayedValue, massUnit) : displayedValue;
    }

    double toCanonical(double stones, double pounds) {
        return MassConverter.stonePoundsToKilograms(stones, pounds);
    }
}
