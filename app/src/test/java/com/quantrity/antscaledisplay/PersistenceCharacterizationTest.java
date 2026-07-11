package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;

public class PersistenceCharacterizationTest {
    @Test
    public void currentAndLegacyUsersSurviveRoundTrip() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("users.json"));

        User current = new User(fixture.getJSONObject(0));
        User currentReloaded = new User(serialize(current));
        assertEquals("user-current-001", currentReloaded.uuid);
        assertEquals(User.MassUnit.KG, currentReloaded.mass_unit);
        assertEquals(168, currentReloaded.height_cm);
        assertTrue(currentReloaded.autoupload);
        assertTrue(currentReloaded.show_fat_mass);
        assertEquals("oauth1-token-placeholder", currentReloaded.garminOauth1Token);
        assertEquals("oauth2-access-placeholder", currentReloaded.garminOauth2Token);

        User legacy = new User(fixture.getJSONObject(1));
        assertEquals(User.MassUnit.LB, legacy.mass_unit);
        assertEquals(180, legacy.height_cm);
        assertTrue(legacy.autoupload);
        assertFalse(legacy.show_fat_mass);

        User legacyReloaded = new User(serialize(legacy));
        assertEquals("user-legacy-002", legacyReloaded.uuid);
        assertEquals(User.MassUnit.LB, legacyReloaded.mass_unit);
        assertEquals(5, legacyReloaded.height_ft);
        assertEquals(11, legacyReloaded.height_in);
        assertTrue(legacyReloaded.isLifetimeAthlete);
    }

    @Test
    public void currentAndLegacyWeightsSurviveRoundTrip() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("history.json"));

        Weight current = new Weight(fixture.getJSONObject(0));
        Weight currentReloaded = new Weight(serialize(current));
        assertTrue(current.equals(currentReloaded));
        assertEquals(25.1, currentReloaded.trunkPercentFat, 0.0001);
        assertEquals(45.1, currentReloaded.muscleMass, 0.0001);

        Weight legacy = new Weight(fixture.getJSONObject(1));
        assertEquals(1750.0, legacy.basalMet, 0.0001);
        assertEquals(-1.0, legacy.activeMet, 0.0001);

        Weight legacyReloaded = new Weight(serialize(legacy));
        assertTrue(legacy.equals(legacyReloaded));
    }

    @Test
    public void goalsSurviveRoundTrip() throws Exception {
        JSONArray fixture = new JSONArray(FixtureLoader.load("goals.json"));
        for (int i = 0; i < fixture.length(); i++) {
            Goal original = new Goal(fixture.getJSONObject(i));
            Goal reloaded = new Goal(serialize(original));
            assertEquals(original.uuid, reloaded.uuid);
            assertEquals(original.start_date, reloaded.start_date);
            assertEquals(original.end_date, reloaded.end_date);
            assertEquals(original.start_value, reloaded.start_value, 0.0001);
            assertEquals(original.end_value, reloaded.end_value, 0.0001);
            assertEquals(original.type, reloaded.type);
            assertEquals(original.color, reloaded.color);
            assertEquals(original.show_fat_mass, reloaded.show_fat_mass);
        }
    }

    private static JSONObject serialize(Object value) throws Exception {
        Method method = value.getClass().getDeclaredMethod("serializeToObj");
        method.setAccessible(true);
        return (JSONObject) method.invoke(value);
    }
}
