package com.quantrity.antscaledisplay;

import android.content.Context;

import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/** Android-resource-aware CSV encoding with explicit success and failure results. */
final class CsvExporter {
    interface LabelProvider {
        String get(int resourceId);
    }

    private CsvExporter() {}

    static RepositoryResult<Integer> write(Writer output, Context context, User user,
                                           List<Weight> weights) {
        return writeWithLabels(output, context::getString, user, weights);
    }

    static RepositoryResult<Integer> writeWithLabels(Writer output, LabelProvider labels,
                                                     User user, List<Weight> weights) {
        if (output == null) {
            return RepositoryResult.failure("Could not open CSV destination",
                    new IllegalArgumentException("CSV output is null"));
        }
        try {
            output.append(labels.get(R.string.edit_user_fragment_user)).append(',');
            output.append(labels.get(R.string.history_fragment_date));
            for (Metric metric : Metric.exportMetrics()) {
                output.append(',').append(labels.get(metric.getLabelRes()));
            }
            output.append('\n');

            DecimalFormat number = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
            number.applyPattern("#.##");
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
            for (Weight weight : weights) {
                output.append(user.name).append(',');
                output.append(weight.date == -1 ? "" : date.format(weight.date));
                for (Metric metric : Metric.exportMetrics()) {
                    output.append(',').append(MetricFormatter.csv(number, user, weight, metric));
                }
                output.append('\n');
            }
            output.flush();
            return RepositoryResult.success(weights.size());
        } catch (Exception exception) {
            return RepositoryResult.failure("Could not export CSV history", exception);
        }
    }
}
