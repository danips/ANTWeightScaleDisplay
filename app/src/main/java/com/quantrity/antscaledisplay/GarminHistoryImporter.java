package com.quantrity.antscaledisplay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts Garmin weight-history JSON into the app model and removes existing measurements. */
final class GarminHistoryImporter {
    private static final long DUPLICATE_WINDOW_MILLIS = 2 * 60 * 60 * 1000L;

    interface ProgressCallback {
        void onProgress(int completed, int total);
    }

    static final class Result {
        final ArrayList<Weight> weights;
        final int received;
        final int added;

        Result(ArrayList<Weight> weights, int received, int added) {
            this.weights = weights;
            this.received = received;
            this.added = added;
        }
    }

    Result importHistory(String json, User user, List<Weight> existing,
                         ProgressCallback progress) throws Exception {
        JSONArray summaries = new JSONObject(json).getJSONArray("dailyWeightSummaries");
        ArrayList<Weight> imported = new ArrayList<>();

        for (int index = 0; index < summaries.length(); index++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            progress.onProgress(index + 1, summaries.length());
            JSONObject measurement = summaries.getJSONObject(index).getJSONObject("latestWeight");
            long date;
            if (!measurement.isNull("timestampGMT")) {
                date = measurement.getLong("timestampGMT");
            } else if (!measurement.isNull("date")) {
                date = measurement.getLong("date");
            } else {
                continue;
            }

            Weight weight = decode(measurement);
            if (isDuplicate(weight, date, user.uuid, existing)) continue;
            weight.date = date;
            weight.uuid = user.uuid;
            weight.height = user.height_cm;
            weight.age = user.age;
            weight.isMale = user.isMale;
            imported.add(weight);
        }

        ArrayList<Weight> merged = new ArrayList<>(existing);
        merged.addAll(imported);
        Collections.sort(merged, new Weight.DateComparator());
        return new Result(merged, summaries.length(), imported.size());
    }

    private static Weight decode(JSONObject source) throws Exception {
        Weight weight = new Weight();
        weight.weight = source.getDouble("weight") / 1000.0;
        weight.percentFat = optionalDouble(source, "bodyFat");
        weight.percentHydration = optionalDouble(source, "bodyWater");
        if (!source.isNull("metabolicAge")) {
            double milliseconds = source.getDouble("metabolicAge");
            weight.metabolicAge = (int) Math.round(milliseconds / 365250 / 86400);
        }
        weight.visceralFatRating = source.isNull("visceralFat")
                ? -1 : source.getInt("visceralFat");
        weight.physiqueRating = source.isNull("physiqueRating")
                ? -1 : source.getInt("physiqueRating");
        weight.muscleMass = source.isNull("muscleMass")
                ? -1 : source.getInt("muscleMass") / 1000.0;
        weight.boneMass = source.isNull("boneMass")
                ? -1 : source.getInt("boneMass") / 1000.0;
        return weight;
    }

    private static double optionalDouble(JSONObject source, String key) throws Exception {
        return source.isNull(key) ? -1 : source.getDouble(key);
    }

    /** Preserves the matching tolerances used by the pre-coordinator history workflow. */
    private static boolean isDuplicate(Weight candidate, long date, String userUuid,
                                       List<Weight> existing) {
        for (Weight weight : existing) {
            long signedDelta = date - weight.date;
            if (signedDelta > DUPLICATE_WINDOW_MILLIS) break;
            if (!userUuid.equals(weight.uuid)
                    || Math.abs(signedDelta) > DUPLICATE_WINDOW_MILLIS) continue;

            boolean repeated = Math.abs(candidate.weight - weight.weight) < 0.05;
            if (repeated && candidate.percentFat != -1) {
                repeated = Math.abs(candidate.percentFat - weight.percentFat) < 0.01;
            }
            if (repeated && candidate.percentHydration != -1) {
                repeated = Math.abs(candidate.percentHydration - weight.percentHydration) < 0.01;
            }
            if (repeated && candidate.boneMass != -1) {
                repeated = Math.abs(candidate.boneMass - weight.boneMass) < 0.01;
            }
            if (repeated && candidate.muscleMass != -1) {
                repeated = Math.abs(candidate.muscleMass - weight.muscleMass) < 0.01;
            }
            if (repeated && candidate.physiqueRating != -1) {
                repeated = candidate.physiqueRating == weight.physiqueRating;
            }
            if (repeated && candidate.percentFat != -1) {
                repeated = Math.round(candidate.visceralFatRating)
                        == Math.round(weight.visceralFatRating);
            }
            if (repeated && candidate.metabolicAge != -1) {
                repeated = candidate.metabolicAge - weight.metabolicAge <= 1;
            }
            if (repeated) return true;
        }
        return false;
    }
}
