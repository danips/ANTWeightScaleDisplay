package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.EnumSet;

public class EditableWeightMetricTest {
    @Test
    public void everyEditableMetricMapsToOneWeightValue() {
        Weight weight = new Weight();
        EnumSet<Metric> mapped = EnumSet.noneOf(Metric.class);
        double value = 10;

        for (EditableWeightMetric field : EditableWeightMetric.values()) {
            field.set(weight, value);
            double expected = field == EditableWeightMetric.PHYSIQUE_RATING
                    || field == EditableWeightMetric.METABOLIC_AGE ? (int) value : value;
            assertEquals(expected, field.value(weight), 0.000001);
            assertTrue("Duplicate mapping for " + field.metric, mapped.add(field.metric));
            value += 0.25;
        }

        assertEquals(Metric.goalMetrics().size() - 1, mapped.size());
        assertFalse(mapped.contains(Metric.BMI));
    }

    @Test
    public void missingValuesFormatAsBlankAndRemainMissing() {
        User user = user(User.MassUnit.KG, false);
        Weight weight = new Weight();

        for (EditableWeightMetric field : EditableWeightMetric.values()) {
            assertEquals("", field.displayText(weight, user));
            field.set(weight, -1);
            assertEquals(-1, field.value(weight), 0.000001);
            assertEquals(-1, field.toCanonicalValue(-1, 80,
                    user(User.MassUnit.LB, true)), 0.000001);
        }
    }

    @Test
    public void massValuesRoundTripForEveryUnit() {
        Weight weight = new Weight();
        weight.weight = 80;

        for (User.MassUnit unit : User.MassUnit.values()) {
            User user = user(unit, false);
            double displayed = MassConverter.toDisplayMass(60, unit);
            assertEquals(60, EditableWeightMetric.MUSCLE_MASS.toCanonicalValue(
                    displayed, weight.weight, user), 0.000001);
        }
    }

    @Test
    public void fatMassAndPercentageRoundTrip() {
        Weight weight = new Weight();
        weight.weight = 80;

        for (User.MassUnit unit : User.MassUnit.values()) {
            User user = user(unit, true);
            double displayed = MassConverter.percentageToDisplayMass(20, weight.weight, unit);
            assertEquals(20, EditableWeightMetric.PERCENT_FAT.toCanonicalValue(
                    displayed, weight.weight, user), 0.000001);

            user.show_fat_mass = false;
            assertEquals(20, EditableWeightMetric.PERCENT_FAT.toCanonicalValue(
                    20, weight.weight, user), 0.000001);
        }
    }

    @Test
    public void unitAndInputMetadataMatchesMetricDefinitions() {
        User kilograms = user(User.MassUnit.KG, false);
        User pounds = user(User.MassUnit.ST, true);

        assertEquals(R.string.weight_edit_fragment_kg_tag,
                EditableWeightMetric.WEIGHT.unitResource(kilograms));
        assertEquals(R.string.weight_edit_fragment_lb_tag,
                EditableWeightMetric.WEIGHT.unitResource(pounds));
        assertEquals(R.string.weight_edit_fragment_percent_tag,
                EditableWeightMetric.PERCENT_FAT.unitResource(kilograms));
        assertEquals(R.string.weight_edit_fragment_lb_tag,
                EditableWeightMetric.PERCENT_FAT.unitResource(pounds));
        assertEquals(R.string.weight_edit_fragment_years_tag,
                EditableWeightMetric.METABOLIC_AGE.unitResource(kilograms));
        assertEquals(R.string.weight_edit_fragment_kcal_tag,
                EditableWeightMetric.BASAL_MET.unitResource(kilograms));
        assertTrue(EditableWeightMetric.VISCERAL_FAT.acceptsDecimalInput());
        assertFalse(EditableWeightMetric.PHYSIQUE_RATING.acceptsDecimalInput());
        assertEquals(0, EditableWeightMetric.ACTIVE_MET.invalidFallback(), 0);
        assertEquals(0, EditableWeightMetric.BASAL_MET.invalidFallback(), 0);
        assertEquals(-1, EditableWeightMetric.MUSCLE_MASS.invalidFallback(), 0);
    }

    private static User user(User.MassUnit unit, boolean showFatMass) {
        User user = new User();
        user.mass_unit = unit;
        user.show_fat_mass = showFatMass;
        return user;
    }
}
