package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class MeasurementTextFormatterTest {
    private final MeasurementTextFormatter formatter = new MeasurementTextFormatter();
    private final MeasurementTextFormatter.Strings strings = new TestStrings();

    @Test
    public void formatsMetricEmailWithoutActivity() {
        User user = user();
        Weight weight = weight();

        MeasurementTextFormatter.EmailMessage message = formatter.email(strings, user, weight);

        assertEquals("person@example.com", message.recipient);
        assertTrue(message.subject.startsWith("Ada Weight "));
        assertTrue(message.body.contains("User: Ada\n"));
        assertTrue(message.body.contains("Height: 168 cm\n"));
        assertTrue(message.body.contains("Weight: 64.2 kg\n"));
        assertTrue(message.body.contains("Percent Fat: 26.3 %\n"));
        assertTrue(message.body.contains("Trunk Percent Fat: 24.0 %\n"));
        assertTrue(message.body.contains("Muscle Mass: 45.1 kg\n"));
        assertFalse(message.body.contains("Bone Mass:"));
    }

    @Test
    public void honorsFatMassAndPoundUnitsForOverallFatOnly() {
        User user = user();
        user.mass_unit = User.MassUnit.LB;
        user.show_fat_mass = true;

        String body = formatter.email(strings, user, weight()).body;

        assertTrue(body.contains("Weight: 141.5 lb\n"));
        assertTrue(body.contains("Percent Fat: 37.2 lb\n"));
        assertTrue(body.contains("Trunk Percent Fat: 24.0 %\n"));
    }

    @Test
    public void formatsMassInStonesAndFallsBackToPoundsBelowOneStone() {
        User user = user();
        user.mass_unit = User.MassUnit.ST;

        String body = formatter.email(strings, user, weight()).body;

        assertTrue(body.contains("Weight: 10 st 1.5 lb\n"));
        assertTrue(body.contains("Muscle Mass: 7 st 1.4 lb\n"));

        Weight lightWeight = weight();
        lightWeight.weight = 5;
        lightWeight.muscleMass = -1;
        body = formatter.email(strings, user, lightWeight).body;

        assertTrue(body.contains("Weight: 11.0 lb\n"));
    }

    private static User user() {
        User user = new User();
        user.name = "Ada";
        user.email_to = "person@example.com";
        user.height_cm = 168;
        user.usesCm = true;
        user.mass_unit = User.MassUnit.KG;
        return user;
    }

    private static Weight weight() {
        Weight weight = new Weight();
        weight.date = 1_783_728_000_000L;
        weight.height = 168;
        weight.weight = 64.2;
        weight.percentFat = 26.3;
        weight.trunkPercentFat = 24;
        weight.muscleMass = 45.1;
        return weight;
    }

    private static class TestStrings implements MeasurementTextFormatter.Strings {
        @Override
        public String get(int resourceId) {
            if (resourceId == R.string.lateral_menu_option_weight) return "Weight";
            if (resourceId == R.string.edit_user_fragment_user) return "User";
            if (resourceId == R.string.edit_user_fragment_height) return "Height";
            if (resourceId == R.string.edit_user_fragment_units_tag_cm) return "cm";
            if (resourceId == R.string.edit_user_fragment_units_tag_ft) return "'";
            if (resourceId == R.string.edit_user_fragment_units_tag_in) return "\"";
            if (resourceId == R.string.weight_fragment_icon_desc_weight) return "Weight";
            if (resourceId == R.string.weight_fragment_icon_desc_percentFat) return "Percent Fat";
            if (resourceId == R.string.weight_fragment_icon_desc_muscleMass) return "Muscle Mass";
            if (resourceId == R.string.weight_fragment_icon_desc_boneMass) return "Bone Mass";
            if (resourceId == R.string.graphs_fragment_measurement_trunk_percent_fat) {
                return "Trunk Percent Fat";
            }
            return "Metric " + resourceId;
        }

        @Override
        public String format(int resourceId, Object... arguments) {
            String pattern;
            if (resourceId == R.string.edit_user_fragment_units_tag_kg) pattern = "%1$.1f kg";
            else if (resourceId == R.string.edit_user_fragment_units_tag_lb) pattern = "%1$.1f lb";
            else if (resourceId == R.string.edit_user_fragment_units_tag_st) pattern = "%1$.0f st %2$.1f lb";
            else if (resourceId == R.string.weight_fragment_percent_tag) pattern = "%1$.1f %%";
            else if (resourceId == R.string.weight_fragment_years_tag) pattern = "%1$d years";
            else if (resourceId == R.string.weight_fragment_kcal_tag) pattern = "%1$.0f kcal";
            else throw new AssertionError("Unexpected format resource " + resourceId);
            return String.format(Locale.US, pattern, arguments);
        }
    }
}
