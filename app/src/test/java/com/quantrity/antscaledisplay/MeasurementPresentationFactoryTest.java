package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public class MeasurementPresentationFactoryTest {
    private final MeasurementPresentationFactory factory =
            new MeasurementPresentationFactory(new TestStrings());

    @Test
    public void presentsEveryGraphMetricAndItsMissingValue() {
        User user = user();
        Weight populated = populatedWeight();
        Weight missing = new Weight();

        for (Metric metric : Metric.goalMetrics()) {
            MeasurementPresentationFactory.MetricDisplay display = factory.metric(
                    metric, user, populated, null, 35, true);
            assertTrue("Expected " + metric + " to be available", display.available);
            assertFalse("Expected formatted text for " + metric, display.primaryText.isEmpty());
            assertEquals(metric.value(populated), display.currentValue, 0.000001);

            display = factory.metric(metric, user, missing, null, 35, true);
            assertFalse("Expected " + metric + " to be missing", display.available);
            assertEquals("", display.primaryText);
        }
    }

    @Test
    public void honorsFatPercentageAndMassPreference() {
        User user = user();
        Weight weight = populatedWeight();

        MeasurementPresentationFactory.MetricDisplay percentage = factory.metric(
                Metric.PERCENTFAT, user, weight, null, 35, true);
        assertEquals("20.0 %", percentage.primaryText);
        assertEquals("Overfat", percentage.secondaryText);

        user.show_fat_mass = true;
        MeasurementPresentationFactory.MetricDisplay mass = factory.metric(
                Metric.PERCENTFAT, user, weight, null, 35, true);
        assertEquals("16.0 kg", mass.primaryText);
        assertEquals("Overfat", mass.secondaryText);
    }

    @Test
    public void currentBmiCanUseTheUsersCurrentHeight() {
        User user = user();
        Weight weight = populatedWeight();
        weight.height = 150;

        MeasurementPresentationFactory.MetricDisplay bmi = factory.bmi(
                user, weight, 200, 35, true);

        assertEquals(20, bmi.currentValue, 0.000001);
        assertEquals("20.00", bmi.primaryText);
    }

    @Test
    public void preservesNormalAndCompactStatusDifferences() {
        User user = user();
        Weight weight = populatedWeight();
        weight.boneMass = 1;
        weight.physiqueRating = 2;
        weight.metabolicAge = 37;

        MeasurementPresentationFactory.MetricDisplay bone = factory.metric(
                Metric.BONEMASS, user, weight, null, 35, true);
        assertEquals(MeasurementPresentationFactory.Status.WARNING, bone.status);
        assertEquals(MeasurementPresentationFactory.Status.DANGER, bone.compactStatus);

        MeasurementPresentationFactory.MetricDisplay physique = factory.metric(
                Metric.PHYSIQUERATING, user, weight, null, 35, true);
        assertEquals(MeasurementPresentationFactory.Status.WARNING, physique.status);
        assertEquals(MeasurementPresentationFactory.Status.DANGER, physique.compactStatus);

        MeasurementPresentationFactory.MetricDisplay age = factory.metric(
                Metric.METABOLICAGE, user, weight, null, 35, true);
        assertEquals(MeasurementPresentationFactory.Status.DANGER, age.status);
        assertEquals(MeasurementPresentationFactory.Status.WARNING, age.compactStatus);
    }

    @Test
    public void presentsEveryBodySegmentWithCorrectMetricMapping() {
        User user = user();
        Weight weight = populatedWeight();
        Weight previous = populatedWeight();
        previous.trunkPercentFat = 18;
        previous.leftArmPercentFat = 17;
        previous.rightArmPercentFat = 16;
        previous.leftLegPercentFat = 20;
        previous.rightLegPercentFat = 21;

        for (BodySegment segment : BodySegment.values()) {
            MeasurementPresentationFactory.SegmentDisplay display =
                    factory.segment(segment, user, weight, previous);
            assertTrue(display.available);
            assertEquals(segment.fatMetric.value(weight), display.currentValue, 0.000001);
            assertEquals(segment.fatMetric.value(previous), display.previousValue, 0.000001);
            assertTrue(display.primaryText.endsWith("%"));
            assertTrue(display.secondaryText.endsWith("kg"));
        }

        assertSegment(BodySegment.TRUNK, Metric.TRUNKPERCENTFAT, Metric.TRUNKMUSCLEMASS);
        assertSegment(BodySegment.LEFT_ARM,
                Metric.LEFTARMPERCENTFAT, Metric.LEFTARMMUSCLEMASS);
        assertSegment(BodySegment.RIGHT_ARM,
                Metric.RIGHTARMPERCENTFAT, Metric.RIGHTARMMUSCLEMASS);
        assertSegment(BodySegment.LEFT_LEG,
                Metric.LEFTLEGPERCENTFAT, Metric.LEFTLEGMUSCLEMASS);
        assertSegment(BodySegment.RIGHT_LEG,
                Metric.RIGHTLEGPERCENTFAT, Metric.RIGHTLEGMUSCLEMASS);
    }

    @Test
    public void graphAvailabilityIsDerivedFromMetricDefinitions() {
        Weight weight = new Weight();
        weight.weight = 80;
        weight.percentFat = 20;
        weight.leftLegMuscleMass = 10;

        Set<Metric> available = MeasurementPresentationFactory.availableMetrics(
                Collections.singletonList(weight));

        assertTrue(available.contains(Metric.WEIGHT));
        assertTrue(available.contains(Metric.BMI));
        assertTrue(available.contains(Metric.PERCENTFAT));
        assertTrue(available.contains(Metric.LEFTLEGMUSCLEMASS));
        assertFalse(available.contains(Metric.PERCENTHYDRATION));
        assertTrue(MeasurementPresentationFactory.availableMetrics(
                Arrays.asList(new Weight(), weight)).contains(Metric.PERCENTFAT));
        assertTrue(MeasurementPresentationFactory.availableMetrics(null).isEmpty());
    }

    private static User user() {
        User user = new User();
        user.mass_unit = User.MassUnit.KG;
        user.height_cm = 180;
        user.age = 35;
        user.isMale = true;
        return user;
    }

    private static void assertSegment(BodySegment segment, Metric fat, Metric muscle) {
        assertEquals(fat, segment.fatMetric);
        assertEquals(muscle, segment.muscleMetric);
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

    private static final class TestStrings implements MeasurementPresentationFactory.Strings {
        @Override
        public String get(int resourceId) {
            if (resourceId == R.string.fat_percent_value_0) return "Underfat";
            if (resourceId == R.string.fat_percent_value_1) return "Healthy";
            if (resourceId == R.string.fat_percent_value_2) return "Overfat";
            if (resourceId == R.string.fat_percent_value_3) return "Obese";
            if (resourceId == R.string.visceral_fat_sub13) return "Healthy";
            if (resourceId == R.string.visceral_fat_plus13) return "Excess";
            if (resourceId >= R.string.physique_rating_1
                    && resourceId <= R.string.physique_rating_9) return "Physique";
            if (resourceId == R.string.bmi_value_0) return "Underweight";
            if (resourceId == R.string.bmi_value_1) return "Normal";
            if (resourceId == R.string.bmi_value_2) return "Overweight";
            if (resourceId == R.string.bmi_value_3) return "Obese";
            return "String " + resourceId;
        }

        @Override
        public String format(int resourceId, Object... arguments) {
            String pattern;
            if (resourceId == R.string.edit_user_fragment_units_tag_kg) pattern = "%1$.1f kg";
            else if (resourceId == R.string.edit_user_fragment_units_tag_lb) pattern = "%1$.1f lb";
            else if (resourceId == R.string.edit_user_fragment_units_tag_st) {
                pattern = "%1$.0f st %2$.1f lb";
            } else if (resourceId == R.string.weight_fragment_percent_tag) pattern = "%1$.1f %%";
            else if (resourceId == R.string.weight_fragment_years_tag) pattern = "%1$d years";
            else if (resourceId == R.string.weight_fragment_kcal_tag) pattern = "%1$.0f kcal";
            else throw new AssertionError("Unexpected format resource " + resourceId);
            return String.format(Locale.US, pattern, arguments);
        }
    }
}
