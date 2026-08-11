package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntWeightSelectionPolicyTest {
    @Test
    public void measurementMatchesOnlyTheSameNonNullProfileUuid() {
        User measurementUser = user("user-a");

        assertTrue(AntWeightSelectionPolicy.matches(measurementUser, user("user-a")));
        assertFalse(AntWeightSelectionPolicy.matches(measurementUser, user("user-b")));
        assertFalse(AntWeightSelectionPolicy.matches(measurementUser, null));
        assertFalse(AntWeightSelectionPolicy.matches(new User(), new User()));
    }

    @Test
    public void deliveredCompletionIsDiscardedOnlyAfterLeavingItsProfile() {
        User measurementUser = user("user-a");

        assertTrue(AntWeightSelectionPolicy.shouldDiscard(
                false, true, measurementUser, user("user-b")));
        assertFalse(AntWeightSelectionPolicy.shouldDiscard(
                true, true, measurementUser, user("user-b")));
        assertFalse(AntWeightSelectionPolicy.shouldDiscard(
                false, false, measurementUser, user("user-b")));
        assertFalse(AntWeightSelectionPolicy.shouldDiscard(
                false, true, measurementUser, user("user-a")));
    }

    private static User user(String uuid) {
        User user = new User();
        user.uuid = uuid;
        return user;
    }
}
