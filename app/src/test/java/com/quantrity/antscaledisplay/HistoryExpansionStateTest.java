package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class HistoryExpansionStateTest {
    @Test
    public void deletionBeforeOrAfterExpandedRowDoesNotMoveExpansion() {
        HistoryExpansionState state = new HistoryExpansionState();
        Weight before = weight("user-a", 300);
        Weight expanded = weight("user-a", 200);
        Weight after = weight("user-a", 100);
        assertTrue(state.toggle(expanded));

        state.retainAll(Arrays.asList(expanded, after));
        assertTrue(state.isExpanded(expanded));
        assertFalse(state.isExpanded(after));

        state.retainAll(Collections.singletonList(expanded));
        assertTrue(state.isExpanded(expanded));
        assertEquals(1, state.expandedCount());

        state.retainAll(Collections.singletonList(before));
        assertFalse(state.isExpanded(expanded));
        assertEquals(0, state.expandedCount());
    }

    @Test
    public void insertReorderAndReplacementPreserveTheSameIdentity() {
        HistoryExpansionState state = new HistoryExpansionState();
        Weight expanded = weight("user-a", 200);
        Weight other = weight("user-a", 100);
        state.toggle(expanded);

        Weight replacement = weight("user-a", 200);
        state.retainAll(Arrays.asList(other, weight("user-a", 300), replacement));

        assertTrue(state.isExpanded(replacement));
        assertFalse(state.isExpanded(other));
        assertFalse(state.toggle(replacement));
        assertEquals(0, state.expandedCount());
    }

    @Test
    public void duplicateDatesAcrossUsersHaveIndependentExpansion() {
        HistoryExpansionState state = new HistoryExpansionState();
        Weight firstUser = weight("user-a", 200);
        Weight secondUser = weight("user-b", 200);

        state.toggle(firstUser);
        state.retainAll(Arrays.asList(firstUser, secondUser));

        assertTrue(state.isExpanded(firstUser));
        assertFalse(state.isExpanded(secondUser));

        state.retainAll(Collections.singletonList(secondUser));
        assertFalse(state.isExpanded(secondUser));
        assertEquals(0, state.expandedCount());
    }

    @Test
    public void rowsWithoutDetailsCannotRemainExpanded() {
        HistoryExpansionState state = new HistoryExpansionState();
        Weight weight = weight("user-a", 200);
        state.toggle(weight);
        weight.boneMass = -1;

        state.retainAll(Collections.singletonList(weight));

        assertFalse(state.isExpanded(weight));
        assertEquals(0, state.expandedCount());
        assertFalse(state.toggle(weight));
    }

    private static Weight weight(String userUuid, long date) {
        Weight weight = new Weight();
        weight.uuid = userUuid;
        weight.date = date;
        weight.weight = 75;
        weight.boneMass = 3;
        return weight;
    }
}
