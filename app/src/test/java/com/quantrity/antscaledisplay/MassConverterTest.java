package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MassConverterTest {
    private static final double TOLERANCE = 0.000001;

    @Test
    public void convertsKilogramsAndPoundsInBothDirections() {
        assertEquals(176.3698096, MassConverter.kilogramsToPounds(80), TOLERANCE);
        assertEquals(80, MassConverter.poundsToKilograms(176.3698096), TOLERANCE);
        assertEquals(0, MassConverter.kilogramsToPounds(0), TOLERANCE);
    }

    @Test
    public void convertsDisplayMassForEveryUnit() {
        assertEquals(80, MassConverter.toDisplayMass(80, User.MassUnit.KG), TOLERANCE);
        assertEquals(176.3698096,
                MassConverter.toDisplayMass(80, User.MassUnit.LB), TOLERANCE);
        assertEquals(176.3698096,
                MassConverter.toDisplayMass(80, User.MassUnit.ST), TOLERANCE);
        assertEquals(80,
                MassConverter.toKilograms(176.3698096, User.MassUnit.ST), TOLERANCE);
    }

    @Test
    public void splitsAndCombinesStoneAndPoundValues() {
        MassConverter.StonePounds value = MassConverter.toStonePounds(64.2);

        assertEquals(10, value.stones, TOLERANCE);
        assertEquals(1.536772204, value.pounds, TOLERANCE);
        assertEquals(64.2,
                MassConverter.stonePoundsToKilograms(value.stones, value.pounds), TOLERANCE);
    }

    @Test
    public void preservesNegativeStoneGoalDifferences() {
        MassConverter.StonePounds value = MassConverter.splitPounds(-15.5);

        assertEquals(-1, value.stones, TOLERANCE);
        assertEquals(-1.5, value.pounds, TOLERANCE);
        assertEquals(-15.5,
                value.stones * MassConverter.POUNDS_PER_STONE + value.pounds, TOLERANCE);
    }

    @Test
    public void convertsPercentageAndDisplayedFatMass() {
        assertEquals(16, MassConverter.percentageToDisplayMass(
                20, 80, User.MassUnit.KG), TOLERANCE);
        assertEquals(35.27396192, MassConverter.percentageToDisplayMass(
                20, 80, User.MassUnit.LB), TOLERANCE);
        assertEquals(20, MassConverter.displayMassToPercentage(
                16, 80, User.MassUnit.KG), TOLERANCE);
        assertEquals(20, MassConverter.displayMassToPercentage(
                35.27396192, 80, User.MassUnit.ST), TOLERANCE);
        assertTrue(Double.isInfinite(MassConverter.displayMassToPercentage(
                1, 0, User.MassUnit.KG)));
    }
}
