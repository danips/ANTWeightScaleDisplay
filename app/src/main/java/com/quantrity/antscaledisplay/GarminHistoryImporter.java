package com.quantrity.antscaledisplay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final int comparisons;

        Result(ArrayList<Weight> weights, int received, int added, int comparisons) {
            this.weights = weights;
            this.received = received;
            this.added = added;
            this.comparisons = comparisons;
        }
    }

    Result importHistory(String json, User user, List<Weight> existing,
                         ProgressCallback progress) throws Exception {
        JSONArray summaries = new JSONObject(json).getJSONArray("dailyWeightSummaries");
        ArrayList<Weight> imported = new ArrayList<>();
        DuplicateIndex duplicates = new DuplicateIndex(user.uuid, existing);

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
            if (duplicates.contains(weight, date)) continue;
            weight.date = date;
            weight.uuid = user.uuid;
            weight.height = user.height_cm;
            weight.age = user.age;
            weight.isMale = user.isMale;
            imported.add(weight);
            duplicates.add(weight);
        }

        ArrayList<Weight> merged = new ArrayList<>(existing);
        merged.addAll(imported);
        Collections.sort(merged, new Weight.DateComparator());
        return new Result(merged, summaries.length(), imported.size(), duplicates.comparisons);
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

    /** Preserves established optional-field tolerances while fixing asymmetric comparisons. */
    private static boolean matches(Weight candidate, Weight existing) {
        boolean repeated = Math.abs(candidate.weight - existing.weight) < 0.05;
        if (repeated && candidate.percentFat != -1) {
            repeated = Math.abs(candidate.percentFat - existing.percentFat) < 0.01;
        }
        if (repeated && candidate.percentHydration != -1) {
            repeated = Math.abs(candidate.percentHydration - existing.percentHydration) < 0.01;
        }
        if (repeated && candidate.boneMass != -1) {
            repeated = Math.abs(candidate.boneMass - existing.boneMass) < 0.01;
        }
        if (repeated && candidate.muscleMass != -1) {
            repeated = Math.abs(candidate.muscleMass - existing.muscleMass) < 0.01;
        }
        if (repeated && candidate.physiqueRating != -1) {
            repeated = candidate.physiqueRating == existing.physiqueRating;
        }
        if (repeated && candidate.visceralFatRating != -1) {
            repeated = Math.round(candidate.visceralFatRating)
                    == Math.round(existing.visceralFatRating);
        }
        if (repeated && candidate.metabolicAge != -1) {
            repeated = Math.abs((long) candidate.metabolicAge - existing.metabolicAge) <= 1;
        }
        return repeated;
    }

    private static final class DuplicateIndex {
        private final Map<Long, ArrayList<Weight>> buckets = new HashMap<>();
        int comparisons;

        DuplicateIndex(String userUuid, List<Weight> existing) {
            for (Weight weight : existing) {
                if (userUuid.equals(weight.uuid)) add(weight);
            }
        }

        void add(Weight weight) {
            long bucket = Math.floorDiv(weight.date, DUPLICATE_WINDOW_MILLIS);
            ArrayList<Weight> values = buckets.get(bucket);
            if (values == null) {
                values = new ArrayList<>();
                buckets.put(bucket, values);
            }
            values.add(weight);
        }

        boolean contains(Weight candidate, long date) {
            long candidateBucket = Math.floorDiv(date, DUPLICATE_WINDOW_MILLIS);
            for (long bucket = candidateBucket - 1; bucket <= candidateBucket + 1; bucket++) {
                List<Weight> possible = buckets.get(bucket);
                if (possible == null) continue;
                for (Weight existing : possible) {
                    comparisons++;
                    long delta = date - existing.date;
                    if (delta < -DUPLICATE_WINDOW_MILLIS
                            || delta > DUPLICATE_WINDOW_MILLIS) continue;
                    if (matches(candidate, existing)) return true;
                }
            }
            return false;
        }
    }
}
