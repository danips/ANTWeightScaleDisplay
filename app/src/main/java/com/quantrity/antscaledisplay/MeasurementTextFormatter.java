package com.quantrity.antscaledisplay;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Produces shareable measurement text without depending on an Activity. */
final class MeasurementTextFormatter {
    interface Strings {
        String get(int resourceId);
        String format(int resourceId, Object... arguments);
    }

    static final class AndroidStrings implements Strings {
        private final Context context;

        AndroidStrings(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override public String get(int resourceId) { return context.getString(resourceId); }
        @Override public String format(int resourceId, Object... arguments) {
            return context.getString(resourceId, arguments);
        }
    }

    static final class EmailMessage {
        final String recipient;
        final String subject;
        final String body;

        EmailMessage(String recipient, String subject, String body) {
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
        }
    }

    EmailMessage email(Strings strings, User user, Weight weight) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
                .format(new Date(weight.date));
        String subject = user.name + " " + strings.get(R.string.lateral_menu_option_weight)
                + " " + timestamp;
        StringBuilder body = new StringBuilder();
        body.append(strings.get(R.string.edit_user_fragment_user)).append(": ")
                .append(user.name).append('\n');
        body.append(strings.get(R.string.edit_user_fragment_height)).append(": ");
        if (user.usesCm) {
            body.append(user.height_cm).append(' ')
                    .append(strings.get(R.string.edit_user_fragment_units_tag_cm));
        } else {
            body.append(user.height_ft).append(' ')
                    .append(strings.get(R.string.edit_user_fragment_units_tag_ft))
                    .append(user.height_in).append(' ')
                    .append(strings.get(R.string.edit_user_fragment_units_tag_in));
        }
        body.append('\n');

        for (Metric metric : Metric.emailMetrics()) {
            String value = metricValue(strings, user, weight, metric,
                    metric == Metric.PERCENTFAT);
            if (!value.isEmpty()) {
                body.append(strings.get(metric.getLabelRes())).append(": ")
                        .append(value).append('\n');
            }
        }
        return new EmailMessage(user.email_to, subject, body.toString());
    }

    private String metricValue(Strings strings, User user, Weight weight, Metric metric,
                               boolean honorFatMass) {
        double raw = metric.value(weight);
        if (raw == -1) return "";
        Metric.Unit unit = metric.displayedUnit(honorFatMass && user.show_fat_mass);
        if (unit == Metric.Unit.MASS) {
            double kilograms = metric.percentageMayBeMass() ? raw * weight.weight / 100 : raw;
            return mass(strings, user.mass_unit, kilograms);
        }
        if (unit == Metric.Unit.PERCENT) {
            return strings.format(R.string.weight_fragment_percent_tag, raw);
        }
        if (unit == Metric.Unit.YEARS) {
            return strings.format(R.string.weight_fragment_years_tag, (int) raw);
        }
        if (unit == Metric.Unit.ENERGY) {
            return strings.format(R.string.weight_fragment_kcal_tag, raw);
        }
        if (metric == Metric.PHYSIQUERATING) return Integer.toString((int) raw);
        return String.format(Locale.getDefault(), "%.2f", raw);
    }

    private String mass(Strings strings, User.MassUnit unit, double kilograms) {
        if (unit == User.MassUnit.LB) {
            return strings.format(R.string.edit_user_fragment_units_tag_lb,
                    MassConverter.kilogramsToPounds(kilograms));
        }
        if (unit == User.MassUnit.ST) {
            MassConverter.StonePounds value = MassConverter.toStonePounds(kilograms);
            if (value.stones == 0) {
                return strings.format(R.string.edit_user_fragment_units_tag_lb,
                        MassConverter.kilogramsToPounds(kilograms));
            }
            return strings.format(R.string.edit_user_fragment_units_tag_st,
                    value.stones, value.pounds);
        }
        return strings.format(R.string.edit_user_fragment_units_tag_kg, kilograms);
    }
}
