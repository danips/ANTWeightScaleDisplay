package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GoalValueDefinitionTest {
    private static final double EPSILON = 0.000001;

    @Test public void everyGoalMetricHasADefinition() {
        for (Metric metric : Metric.goalMetrics()) {
            GoalValueDefinition definition = GoalValueDefinition.forMetric(
                    metric, User.MassUnit.KG, false);
            assertEquals(metric.getUnit() == Metric.Unit.NONE
                            ? GoalValueDefinition.Mode.UNITLESS
                            : GoalValueDefinition.Mode.SINGLE_UNIT,
                    definition.mode);
        }
    }

    @Test public void unitlessPercentageEnergyAndYearsKeepCanonicalValues() {
        assertRoundTrip(Metric.BMI, User.MassUnit.KG, false, 23.4);
        assertRoundTrip(Metric.PERCENTHYDRATION, User.MassUnit.KG, false, 55.6);
        assertRoundTrip(Metric.ACTIVEMET, User.MassUnit.KG, false, 2150);
        assertRoundTrip(Metric.METABOLICAGE, User.MassUnit.KG, false, 42);
    }

    @Test public void kilogramsAndPoundsRoundTripToCanonicalKilograms() {
        assertRoundTrip(Metric.WEIGHT, User.MassUnit.KG, false, 81.25);
        assertRoundTrip(Metric.WEIGHT, User.MassUnit.LB, false, 81.25);
    }

    @Test public void stonesAndPoundsRoundTripToCanonicalKilograms() {
        GoalValueDefinition definition = GoalValueDefinition.forMetric(
                Metric.WEIGHT, User.MassUnit.ST, false);
        assertEquals(GoalValueDefinition.Mode.STONE_POUNDS, definition.mode);
        MassConverter.StonePounds displayed = MassConverter.toStonePounds(81.25);
        assertEquals(81.25, definition.toCanonical(displayed.stones, displayed.pounds), EPSILON);
    }

    @Test public void fatPercentageCanBePercentageOrCanonicalMass() {
        GoalValueDefinition percent = GoalValueDefinition.forMetric(
                Metric.PERCENTFAT, User.MassUnit.LB, false);
        GoalValueDefinition mass = GoalValueDefinition.forMetric(
                Metric.PERCENTFAT, User.MassUnit.LB, true);
        assertEquals(R.string.weight_edit_fragment_percent_tag, percent.primaryUnitResource);
        assertEquals(GoalValueDefinition.Mode.SINGLE_UNIT, mass.mode);
        assertEquals(R.string.weight_edit_fragment_lb_tag, mass.primaryUnitResource);
        assertEquals(12.5, percent.toCanonical(12.5), EPSILON);
        assertEquals(12.5, mass.toCanonical(MassConverter.kilogramsToPounds(12.5)), EPSILON);
    }

    @Test public void precisionMatchesExistingGoalFormatting() {
        assertEquals(0, definition(Metric.PHYSIQUERATING).decimals);
        assertEquals(1, definition(Metric.VISCERALFATRATING).decimals);
        assertEquals(0, definition(Metric.METABOLICAGE).decimals);
        assertEquals(0, definition(Metric.BASALMET).decimals);
        assertEquals(1, definition(Metric.PERCENTHYDRATION).decimals);
        assertEquals(1, definition(Metric.WEIGHT).decimals);
    }

    private static GoalValueDefinition definition(Metric metric) {
        return GoalValueDefinition.forMetric(metric, User.MassUnit.KG, false);
    }

    private static void assertRoundTrip(Metric metric, User.MassUnit unit,
                                        boolean fatMass, double canonical) {
        GoalValueDefinition definition = GoalValueDefinition.forMetric(metric, unit, fatMass);
        double displayed = metric.displayedUnit(fatMass) == Metric.Unit.MASS
                ? MassConverter.toDisplayMass(canonical, unit) : canonical;
        assertEquals(canonical, definition.toCanonical(displayed), EPSILON);
    }
}
