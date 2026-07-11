package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@RunWith(Parameterized.class)
public class MetricDefinitionTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> metrics() {
        Weight weight = populatedWeight();
        List<Object[]> cases = new ArrayList<>();
        for (Metric metric : Metric.goalMetrics()) {
            cases.add(new Object[]{metric, expected(metric, weight)});
        }
        return cases;
    }

    private final Metric metric;
    private final double expected;
    private Weight populated;

    public MetricDefinitionTest(Metric metric, double expected) {
        this.metric = metric;
        this.expected = expected;
    }

    @Before
    public void setUp() {
        populated = populatedWeight();
    }

    @Test
    public void extractsItsValue() {
        assertEquals(expected, metric.value(populated), 0.000001);
    }

    @Test
    public void reportsMissingValue() {
        assertEquals(-1, metric.value(new Weight()), 0);
    }

    @Test
    public void hasCompleteUiAndFormattingMetadata() {
        assertNotEquals(0, metric.getGraphId());
        assertNotEquals(0, metric.getIconRes());
        assertNotEquals(0, metric.getLabelRes());
        assertNotEquals(0, metric.getGraphColor());
        assertNotEquals(null, metric.getUnit());
        assertEquals(metric, Metric.fromGraphId(metric.getGraphId()));
        assertEquals(metric, Metric.fromGoalPosition(metric.getMetricCode() - 1));
    }

    @Test
    public void percentageMassRulesAreConsistent() {
        Metric.Unit expectedMassUnit = metric.percentageMayBeMass()
                ? Metric.Unit.MASS : metric.getUnit();
        assertEquals(expectedMassUnit, metric.displayedUnit(true));
        assertEquals(metric.getUnit(), metric.displayedUnit(false));
        double expectedGoal = metric.percentageMayBeMass()
                ? expected * populated.weight / 100 : expected;
        assertEquals(expectedGoal, metric.goalValue(populated, true), 0.000001);
    }

    @Test
    public void csvFormattingUsesTheDefinition() {
        DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        format.applyPattern("#.##");
        User user = new User();
        user.show_fat_mass = false;
        assertEquals(format.format(expected), MetricFormatter.csv(format, user, populated, metric));
        user.show_fat_mass = true;
        double massValue = metric.percentageMayBeMass()
                ? expected * populated.weight / 100 : expected;
        assertEquals(format.format(massValue), MetricFormatter.csv(format, user, populated, metric));
    }

    private static Weight populatedWeight() {
        Weight weight = new Weight();
        weight.height = 180;
        weight.weight = 80;
        weight.percentFat = 20;
        weight.percentHydration = 55;
        weight.boneMass = 3.2;
        weight.muscleMass = 60;
        weight.physiqueRating = 6;
        weight.visceralFatRating = 7.5;
        weight.metabolicAge = 31;
        weight.activeMet = 2400;
        weight.basalMet = 1700;
        weight.trunkPercentFat = 19;
        weight.trunkMuscleMass = 30;
        weight.leftArmPercentFat = 18;
        weight.leftArmMuscleMass = 4.1;
        weight.rightArmPercentFat = 17;
        weight.rightArmMuscleMass = 4.2;
        weight.leftLegPercentFat = 21;
        weight.leftLegMuscleMass = 10.1;
        weight.rightLegPercentFat = 22;
        weight.rightLegMuscleMass = 10.2;
        return weight;
    }

    private static double expected(Metric metric, Weight weight) {
        switch (metric) {
            case BMI: return 80 / Math.pow(1.8, 2);
            case WEIGHT: return 80;
            case PERCENTFAT: return 20;
            case PERCENTHYDRATION: return 55;
            case BONEMASS: return 3.2;
            case MUSCLEMASS: return 60;
            case PHYSIQUERATING: return 6;
            case VISCERALFATRATING: return 7.5;
            case METABOLICAGE: return 31;
            case ACTIVEMET: return 2400;
            case BASALMET: return 1700;
            case TRUNKPERCENTFAT: return 19;
            case TRUNKMUSCLEMASS: return 30;
            case LEFTARMPERCENTFAT: return 18;
            case LEFTARMMUSCLEMASS: return 4.1;
            case RIGHTARMPERCENTFAT: return 17;
            case RIGHTARMMUSCLEMASS: return 4.2;
            case LEFTLEGPERCENTFAT: return 21;
            case LEFTLEGMUSCLEMASS: return 10.1;
            case RIGHTLEGPERCENTFAT: return 22;
            case RIGHTLEGMUSCLEMASS: return 10.2;
            default: throw new AssertionError(metric);
        }
    }
}
