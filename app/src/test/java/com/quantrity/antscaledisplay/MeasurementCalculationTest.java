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
        user.mass_unit = User.MassUnit.KG;
        assertEquals(16.0, user.calc_mass2(20, 80, true), 0.0001);

        user.mass_unit = User.MassUnit.LB;
        assertEquals(35.27396192, user.calc_mass2(20, 80, true), 0.0001);
    }

    @Test
    public void healthClassificationsPreserveBoundaryBehavior() {
        assertEquals(1, RequestWeight.getPercentHydrationDesc(50, true));
        assertEquals(1, RequestWeight.getPercentHydrationDesc(65, true));
        assertEquals(3, RequestWeight.getBMIDesc((byte) 30, 31, true));
        assertEquals(2, RequestWeight.getPercentFatDesc((byte) 35, 20, true));
        assertEquals(0, RequestWeight.getBoneMassDesc(70, 2, true));
    }

    private static Calendar utcDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day);
        return calendar;
    }
}
