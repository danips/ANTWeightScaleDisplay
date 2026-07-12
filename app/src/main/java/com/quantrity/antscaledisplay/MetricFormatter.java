package com.quantrity.antscaledisplay;

import java.text.DecimalFormat;

/** Formats canonical metric values for CSV export. */
final class MetricFormatter {
    private MetricFormatter() {}

    static String csv(DecimalFormat format, User user, Weight weight, Metric metric) {
        double value = metric.goalValue(weight, user.show_fat_mass);
        return value == -1 ? "" : format.format(value);
    }
}
