package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class NavigationDestinationTest {
    @Test public void everyDestinationHasAUniqueStableViewIdAndTitle() {
        Set<Integer> ids = new HashSet<>();
        for (NavigationDestination destination : NavigationDestination.values()) {
            assertEquals(destination, NavigationDestination.fromViewId(destination.viewId));
            ids.add(destination.viewId);
            org.junit.Assert.assertNotEquals(0, destination.titleResource);
        }
        assertEquals(NavigationDestination.values().length, ids.size());
        assertNull(NavigationDestination.fromViewId(-1));
    }

    @Test public void destinationMapsToExpectedFragmentClass() {
        assertEquals(WeightFragment.class, NavigationDestination.WEIGHT.fragmentClass);
        assertEquals(GoalsFragment.class, NavigationDestination.GOALS.fragmentClass);
        assertEquals(GraphsFragment.class, NavigationDestination.GRAPHS.fragmentClass);
        assertEquals(HistoryFragment.class, NavigationDestination.HISTORY.fragmentClass);
        assertEquals(UsersFragment.class, NavigationDestination.USERS.fragmentClass);
    }

    @Test public void editorFragmentsKeepTheirParentDestinationSelected() {
        assertEquals(NavigationDestination.WEIGHT,
                NavigationDestination.forFragment(new EditWeightFragment()));
        assertEquals(NavigationDestination.GOALS,
                NavigationDestination.forFragment(new EditGoalFragment()));
        assertEquals(NavigationDestination.USERS,
                NavigationDestination.forFragment(new EditUserFragment()));
    }
}
