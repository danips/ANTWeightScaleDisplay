package com.quantrity.antscaledisplay;

import androidx.fragment.app.Fragment;

/** Stable top-level navigation mapping independent of localized menu labels. */
enum NavigationDestination {
    WEIGHT(R.id.nav_weight, R.string.lateral_menu_option_weight, WeightFragment.class),
    GOALS(R.id.nav_goals, R.string.lateral_menu_option_goals, GoalsFragment.class),
    GRAPHS(R.id.nav_graphs, R.string.lateral_menu_option_graphs, GraphsFragment.class),
    HISTORY(R.id.nav_history, R.string.lateral_menu_option_history, HistoryFragment.class),
    USERS(R.id.nav_users, R.string.lateral_menu_option_users, UsersFragment.class);

    final int menuId;
    final int titleResource;
    final Class<? extends Fragment> fragmentClass;

    NavigationDestination(int menuId, int titleResource,
                          Class<? extends Fragment> fragmentClass) {
        this.menuId = menuId;
        this.titleResource = titleResource;
        this.fragmentClass = fragmentClass;
    }

    static NavigationDestination fromMenuId(int menuId) {
        for (NavigationDestination destination : values()) {
            if (destination.menuId == menuId) return destination;
        }
        return null;
    }

    Fragment createFragment() {
        switch (this) {
            case GOALS: return new GoalsFragment();
            case GRAPHS: return new GraphsFragment();
            case HISTORY: return new HistoryFragment();
            case USERS: return new UsersFragment();
            default: return new WeightFragment();
        }
    }

    boolean matches(Fragment fragment) {
        return fragment != null && fragmentClass.isInstance(fragment);
    }
}
