package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class GarminHistoryImporterTest {
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
    public void sameMeasurementForAnotherUserIsNotADuplicate() throws Exception {
        User user = user("user-one");
        Weight otherUser = weight("user-two", 1_000_000L, 70);

        GarminHistoryImporter.Result result = new GarminHistoryImporter().importHistory(
                "{\"dailyWeightSummaries\":[{\"latestWeight\":{"
                        + "\"timestampGMT\":1000000,\"weight\":70000}}]}",
                user, Arrays.asList(otherUser), (completed, total) -> {});

        assertEquals(1, result.added);
        assertEquals(2, result.weights.size());
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

    private static String historyJson() {
        long metabolicAge = 30L * 365250 * 86400;
        return "{\"dailyWeightSummaries\":["
                + "{\"latestWeight\":{\"timestampGMT\":1000000,\"weight\":70000,"
                + "\"bodyFat\":20,\"bodyWater\":55,\"visceralFat\":8,"
                + "\"physiqueRating\":5,\"muscleMass\":50000,\"boneMass\":3000}},"
                + "{\"latestWeight\":{\"date\":20000000,\"weight\":71250,"
                + "\"metabolicAge\":" + metabolicAge + ",\"muscleMass\":50500,"
                + "\"boneMass\":3100}},"
                + "{\"latestWeight\":{\"weight\":80000}}]}";
    }
}
