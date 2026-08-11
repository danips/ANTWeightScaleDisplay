package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.json.JSONArray;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UserCopyTest {
    @Test
    public void copyPreservesEveryInstanceFieldAndSerializedValue() throws Exception {
        User original = new User(
                new JSONArray(FixtureLoader.load("users.json")).getJSONObject(0));

        User copy = original.copy();

        assertNotSame(original, copy);
        for (Field field : User.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true);
            assertEquals("Field omitted from User.copy(): " + field.getName(),
                    field.get(original), field.get(copy));
        }
        assertEquals(original.serializeToObj().toString(), copy.serializeToObj().toString());
    }

    @Test
    public void copyCanBeChangedWithoutMutatingOriginal() throws Exception {
        User original = new User(
                new JSONArray(FixtureLoader.load("users.json")).getJSONObject(0));

        User copy = original.copy();
        String originalName = original.name;
        String originalToken = original.garminOauth2Token;
        copy.name = "Changed";
        copy.garminOauth2Token = "changed-token";

        assertEquals(originalName, original.name);
        assertEquals(originalToken, original.garminOauth2Token);
    }
}
