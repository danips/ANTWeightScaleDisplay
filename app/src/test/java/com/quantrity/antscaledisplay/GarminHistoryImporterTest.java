package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GarminHistoryImporterTest {
    private static final long DUPLICATE_WINDOW_MILLIS = 2 * 60 * 60 * 1000L;

    @Test
    public void importsNewMeasurementsAndKeepsExistingOnes() throws Exception {
        User user = user("user-one");
        Weight duplicate = weight("user-one", 1_000_000L, 70);
        duplicate.percentFat = 20;
        duplicate.percentHydration = 55;
        duplicate.visceralFatRating = 8;
        duplicate.physiqueRating = 5;
        duplicate.muscleMass = 50;
        duplicate.boneMass = 3;
        ArrayList<String> progress = new ArrayList<>();

        GarminHistoryImporter.Result result = new GarminHistoryImporter().importHistory(
                historyJson(), user, Arrays.asList(duplicate),
                (completed, total) -> progress.add(completed + "/" + total));

        assertEquals(3, result.received);
        assertEquals(1, result.added);
        assertEquals(2, result.weights.size());
        assertEquals(1, result.comparisons);
        Weight imported = result.weights.get(0);
        assertEquals(20_000_000L, imported.date);
        assertEquals("user-one", imported.uuid);
        assertEquals(71.25, imported.weight, 0.001);
        assertEquals(30, imported.metabolicAge);
        assertEquals(50.5, imported.muscleMass, 0.001);
        assertEquals(3.1, imported.boneMass, 0.001);
        assertEquals(180, imported.height, 0.001);
        assertEquals(42, imported.age);
        assertEquals(Arrays.asList("1/3", "2/3", "3/3"), progress);
    }

    @Test
    public void comparesVisceralFatWhenBodyFatIsMissing() throws Exception {
        Weight existing = weight("user-one", 1_000_000L, 70);
        existing.visceralFatRating = 7;

        GarminHistoryImporter.Result result = importOne(
                measurement(1_000_000L, 70, ",\"visceralFat\":8"),
                Collections.singletonList(existing));

        assertEquals(1, result.added);
    }

    @Test
    public void keepsOptionalVisceralFatToleranceForMissingCandidateValue() throws Exception {
        Weight existing = weight("user-one", 1_000_000L, 70);
        existing.visceralFatRating = 8;

        GarminHistoryImporter.Result missingCandidate = importOne(
                measurement(1_000_000L, 70, ""), Collections.singletonList(existing));
        GarminHistoryImporter.Result missingExisting = importOne(
                measurement(1_000_000L, 70, ",\"visceralFat\":8"),
                Collections.singletonList(weight("user-one", 1_000_000L, 70)));

        assertEquals(0, missingCandidate.added);
        assertEquals(1, missingExisting.added);
    }

    @Test
    public void metabolicAgeToleranceIsSymmetric() throws Exception {
        Weight youngerExisting = weight("user-one", 1_000_000L, 70);
        youngerExisting.metabolicAge = 28;
        Weight olderExisting = weight("user-one", 1_000_000L, 70);
        olderExisting.metabolicAge = 30;

        GarminHistoryImporter.Result olderCandidate = importOne(
                measurement(1_000_000L, 70, metabolicAgeField(30)),
                Collections.singletonList(youngerExisting));
        GarminHistoryImporter.Result youngerCandidate = importOne(
                measurement(1_000_000L, 70, metabolicAgeField(28)),
                Collections.singletonList(olderExisting));
        GarminHistoryImporter.Result withinTolerance = importOne(
                measurement(1_000_000L, 70, metabolicAgeField(29)),
                Collections.singletonList(olderExisting));

        assertEquals(1, olderCandidate.added);
        assertEquals(1, youngerCandidate.added);
        assertEquals(0, withinTolerance.added);
    }

    @Test
    public void duplicateWindowIncludesBothExactBoundaries() throws Exception {
        long existingDate = 20_000_000L;
        Weight existing = weight("user-one", existingDate, 70);

        GarminHistoryImporter.Result earlierBoundary = importOne(
                measurement(existingDate - DUPLICATE_WINDOW_MILLIS, 70, ""),
                Collections.singletonList(existing));
        GarminHistoryImporter.Result laterBoundary = importOne(
                measurement(existingDate + DUPLICATE_WINDOW_MILLIS, 70, ""),
                Collections.singletonList(existing));
        GarminHistoryImporter.Result outsideBoundary = importOne(
                measurement(existingDate + DUPLICATE_WINDOW_MILLIS + 1, 70, ""),
                Collections.singletonList(existing));

        assertEquals(0, earlierBoundary.added);
        assertEquals(0, laterBoundary.added);
        assertEquals(1, outsideBoundary.added);
    }

    @Test
    public void duplicateDetectionDoesNotDependOnExistingSortOrder() throws Exception {
        Weight oldest = weight("user-one", 1_000_000L, 60);
        Weight matching = weight("user-one", 20_000_000L, 70);
        Weight newest = weight("user-one", 40_000_000L, 80);

        GarminHistoryImporter.Result result = importOne(
                measurement(20_000_000L, 70, ""),
                Arrays.asList(oldest, newest, matching));

        assertEquals(0, result.added);
        assertEquals(40_000_000L, result.weights.get(0).date);
        assertEquals(1_000_000L, result.weights.get(2).date);
    }

    @Test
    public void indexesOnlyTheSelectedUsersMeasurements() throws Exception {
        ArrayList<Weight> existing = new ArrayList<>();
        for (int index = 0; index < 1_000; index++) {
            existing.add(weight("other-user", 1_000_000L + index, 70));
        }
        existing.add(weight("user-one", 1_000_000L, 70));

        GarminHistoryImporter.Result result = importOne(
                measurement(1_000_000L, 70, ""), existing);

        assertEquals(0, result.added);
        assertEquals(1, result.comparisons);
    }

    @Test
    public void repeatedImportsAndRepeatedSummariesDoNotAddDuplicates() throws Exception {
        String repeated = history(
                measurement(1_000_000L, 70, ""),
                measurement(1_000_000L, 70, ""));
        GarminHistoryImporter importer = new GarminHistoryImporter();

        GarminHistoryImporter.Result first = importer.importHistory(
                repeated, user("user-one"), Collections.emptyList(), (completed, total) -> {});
        GarminHistoryImporter.Result second = importer.importHistory(
                repeated, user("user-one"), first.weights, (completed, total) -> {});

        assertEquals(1, first.added);
        assertEquals(0, second.added);
        assertEquals(1, second.weights.size());
    }

    @Test
    public void dateBucketsBoundComparisonsForLargeHistories() throws Exception {
        ArrayList<Weight> existing = new ArrayList<>();
        long day = 24 * 60 * 60 * 1000L;
        for (int index = 0; index < 10_000; index++) {
            existing.add(weight("user-one", index * day, 70));
        }

        GarminHistoryImporter.Result result = importOne(
                measurement(5_000 * day, 70, ""), existing);

        assertEquals(0, result.added);
        assertTrue("date index should inspect only nearby candidates", result.comparisons <= 3);
    }

    private static GarminHistoryImporter.Result importOne(
            String measurement, List<Weight> existing) throws Exception {
        return new GarminHistoryImporter().importHistory(
                history(measurement), user("user-one"), existing, (completed, total) -> {});
    }

    private static User user(String uuid) {
        User user = new User();
        user.uuid = uuid;
        user.height_cm = 180;
        user.age = 42;
        user.isMale = true;
        return user;
    }

    private static Weight weight(String uuid, long date, double kilograms) {
        Weight weight = new Weight();
        weight.uuid = uuid;
        weight.date = date;
        weight.weight = kilograms;
        return weight;
    }

    private static String history(String... measurements) {
        StringBuilder json = new StringBuilder("{\"dailyWeightSummaries\":[");
        for (int index = 0; index < measurements.length; index++) {
            if (index > 0) json.append(',');
            json.append("{\"latestWeight\":").append(measurements[index]).append('}');
        }
        return json.append("]}").toString();
    }

    private static String measurement(long date, double kilograms, String extraFields) {
        return "{\"timestampGMT\":" + date + ",\"weight\":"
                + Math.round(kilograms * 1000) + extraFields + "}";
    }

    private static String metabolicAgeField(int years) {
        return ",\"metabolicAge\":" + years * 365250L * 86400L;
    }

    private static String historyJson() {
        return "{\"dailyWeightSummaries\":["
                + "{\"latestWeight\":{\"timestampGMT\":1000000,\"weight\":70000,"
                + "\"bodyFat\":20,\"bodyWater\":55,\"visceralFat\":8,"
                + "\"physiqueRating\":5,\"muscleMass\":50000,\"boneMass\":3000}},"
                + "{\"latestWeight\":{\"date\":20000000,\"weight\":71250,"
                + "\"metabolicAge\":" + 30L * 365250 * 86400 + ",\"muscleMass\":50500,"
                + "\"boneMass\":3100}},"
                + "{\"latestWeight\":{\"weight\":80000}}]}";
    }
}
