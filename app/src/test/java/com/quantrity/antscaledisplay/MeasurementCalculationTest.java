package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class MeasurementCalculationTest {
    @Test
    public void ageChangesAfterBirthday() {
        Calendar birth = utcDate(1990, Calendar.JULY, 20);
        Calendar beforeBirthday = utcDate(2026, Calendar.JULY, 19);
        Calendar onBirthday = utcDate(2026, Calendar.JULY, 20);

        assertEquals(35, User.calcAge(birth.getTimeInMillis(), beforeBirthday.getTimeInMillis()));
        assertEquals(36, User.calcAge(birth.getTimeInMillis(), onBirthday.getTimeInMillis()));
    }

    @Test
    public void fatPercentageConvertsToMassForConfiguredUnits() {
        User user = new User();
        user.show_fat_mass = true;
        Weight weight = new Weight();
        weight.weight = 80;
        weight.percentFat = 20;
        user.mass_unit = User.MassUnit.KG;
        assertEquals(16.0, Metric.PERCENTFAT.graphValue(weight, user), 0.0001);

        user.mass_unit = User.MassUnit.LB;
        assertEquals(35.27396192, Metric.PERCENTFAT.graphValue(weight, user), 0.0001);

        user.mass_unit = User.MassUnit.ST;
        assertEquals(35.27396192, Metric.PERCENTFAT.graphValue(weight, user), 0.0001);
    }

    @Test
    public void percentageRemainsPercentageWhenFatMassDisplayIsDisabled() {
        User user = new User();
        user.show_fat_mass = false;
        Weight weight = new Weight();
        weight.weight = 80;
        weight.percentFat = 20;

        for (User.MassUnit unit : User.MassUnit.values()) {
            user.mass_unit = unit;
            assertEquals(20, Metric.PERCENTFAT.graphValue(weight, user), 0.0001);
        }
    }

    @Test
    public void enteredMassConvertsBackToCanonicalValues() {
        assertEquals(80, MassConverter.toKilograms(80, User.MassUnit.KG), 0.0001);
        assertEquals(20, MassConverter.displayMassToPercentage(
                16, 80, User.MassUnit.KG), 0.0001);

        for (User.MassUnit unit : new User.MassUnit[]{User.MassUnit.LB, User.MassUnit.ST}) {
            assertEquals(80, MassConverter.toKilograms(176.3698096, unit), 0.0001);
            assertEquals(20, MassConverter.displayMassToPercentage(
                    35.27396192, 80, unit), 0.0001);
        }
    }

    @Test
    public void healthClassificationsPreserveBoundaryBehavior() {
        assertEquals(1, HealthRangeClassifier.getPercentHydrationDesc(50, true));
        assertEquals(1, HealthRangeClassifier.getPercentHydrationDesc(65, true));
        assertEquals(3, HealthRangeClassifier.getBMIDesc((byte) 30, 31, true));
        assertEquals(2, HealthRangeClassifier.getPercentFatDesc((byte) 35, 20, true));
        assertEquals(0, HealthRangeClassifier.getBoneMassDesc(70, 2, true));
    }

    private static Calendar utcDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day);
        return calendar;
    }
}
