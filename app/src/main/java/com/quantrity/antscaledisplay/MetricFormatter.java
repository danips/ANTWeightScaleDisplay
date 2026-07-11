package com.quantrity.antscaledisplay;

import android.content.Context;

import java.text.DecimalFormat;
import java.util.Locale;

/** Android string formatting kept separate from the pure metric definitions. */
final class MetricFormatter {
    private MetricFormatter() {}

    static String display(Context context, User user, Weight weight, Metric metric,
                          boolean honorFatMass) {
        double raw = metric.value(weight);
        if (raw == -1) return "";
        Metric.Unit unit = metric.displayedUnit(honorFatMass && user.show_fat_mass);
        if (unit == Metric.Unit.MASS) {
            double kilograms = metric.percentageMayBeMass() ? raw * weight.weight / 100 : raw;
            return user.printMass(context, kilograms);
        }
        if (unit == Metric.Unit.PERCENT) {
            return String.format(context.getString(R.string.weight_fragment_percent_tag), raw);
        }
        if (unit == Metric.Unit.YEARS) {
            return String.format(context.getString(R.string.weight_fragment_years_tag), (int) raw);
        }
        if (unit == Metric.Unit.ENERGY) {
            return String.format(context.getString(R.string.weight_fragment_kcal_tag), raw);
        }
        if (metric == Metric.PHYSIQUERATING) return Integer.toString((int) raw);
        return String.format(Locale.getDefault(), "%.2f", raw);
    }

    static String csv(DecimalFormat format, User user, Weight weight, Metric metric) {
        double value = metric.goalValue(weight, user.show_fat_mass);
        return value == -1 ? "" : format.format(value);
    }
}
