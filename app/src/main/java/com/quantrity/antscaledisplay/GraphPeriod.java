package com.quantrity.antscaledisplay;

/** Definition of every graph viewport period. Periods zoom data; they do not filter it. */
enum GraphPeriod {
    WEEK(R.id.graph_time_week, 7f, 0),
    TWO_WEEKS(R.id.graph_time_two_weeks, 14f, 30L),
    MONTH(R.id.graph_time_month, 365f / 12f, 180L),
    SIX_WEEKS(R.id.graph_time_six_weeks, 42f, 365L),
    TWO_MONTHS(R.id.graph_time_two_months, 365f / 6f, 0),
    FOUR_MONTHS(R.id.graph_time_four_months, 365f / 4f, 0),
    HALF_YEAR(R.id.graph_time_half_year, 365f / 2f, 0),
    YEAR(R.id.graph_time_year, 365f, 0),
    TWO_YEARS(R.id.graph_time_two_years, 730f, 0),
    ALWAYS(R.id.graph_time_always, 0, 0);

    static final double DAY_MILLIS = 86_400_000d;
    final int menuId;
    private final float windowDays;
    private final long minimumHistoryDays;

    GraphPeriod(int menuId, float windowDays, long minimumHistoryDays) {
        this.menuId = menuId;
        this.windowDays = windowDays;
        this.minimumHistoryDays = minimumHistoryDays;
    }

    static GraphPeriod fromMenuId(int menuId) {
        for (GraphPeriod period : values()) if (period.menuId == menuId) return period;
        return MONTH;
    }

    static boolean isPeriodMenuId(int menuId) {
        for (GraphPeriod period : values()) if (period.menuId == menuId) return true;
        return false;
    }

    float windowDays(long oldestMillis, long newestMillis) {
        if (this != ALWAYS) return windowDays;
        return Math.max(1f, (float) ((newestMillis - oldestMillis) / DAY_MILLIS));
    }

    double rollingWindowMillis(long oldestMillis, long newestMillis) {
        return windowDays(oldestMillis, newestMillis) * DAY_MILLIS * 30d / 365d;
    }

    boolean isAvailable(long historyMillis) {
        return historyMillis > minimumHistoryDays * DAY_MILLIS;
    }
}
