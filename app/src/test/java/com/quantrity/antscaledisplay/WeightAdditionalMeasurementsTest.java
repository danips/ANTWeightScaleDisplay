package com.quantrity.antscaledisplay;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeightAdditionalMeasurementsTest {
    @Test
    public void weightOnlyHasNoAdditionalMeasurements() {
        Weight weight = new Weight();
        weight.weight = 72.5;
        weight.height = 180;

        assertFalse(weight.hasAdditionalMeasurements());
    }

    @Test
    public void bodyCompositionValueIsAnAdditionalMeasurement() {
        Weight weight = new Weight();
        weight.weight = 72.5;
        weight.percentFat = 18.4;

        assertTrue(weight.hasAdditionalMeasurements());
    }

    @Test
    public void segmentalOrMetabolicValueIsAnAdditionalMeasurement() {
        Weight segmental = new Weight();
        segmental.leftArmMuscleMass = 3.2;

        Weight metabolic = new Weight();
        metabolic.basalMet = 1650;

        assertTrue(segmental.hasAdditionalMeasurements());
        assertTrue(metabolic.hasAdditionalMeasurements());
    }
}
