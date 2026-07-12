package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgNum;
import com.garmin.fit.WeightScaleMesg;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;

public class FitGenerationCharacterizationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedWeightFileIsValidFit() throws Exception {
        File output = temporaryFolder.newFile("weight.fit");

        Weight weight = new Weight();
        weight.date = 1_783_728_000_000L;
        weight.height = Math.sqrt(64.2 / 22.75) * 100;
        weight.weight = 64.2;
        weight.percentFat = 26.3;
        weight.percentHydration = 52.4;
        weight.boneMass = 2.4;
        weight.muscleMass = 45.1;

        new FitFileFactory().create(output, weight);

        assertTrue(output.length() > 14);
        try (FileInputStream input = new FileInputStream(output)) {
            assertTrue(new Decode().checkFileIntegrity(input));
        }
        ArrayList<WeightScaleMesg> messages = new ArrayList<>();
        try (FileInputStream input = new FileInputStream(output)) {
            assertTrue(new Decode().read(input, message -> {
                if (message.getNum() == MesgNum.WEIGHT_SCALE) {
                    messages.add(new WeightScaleMesg(message));
                }
            }));
        }
        assertEquals(1, messages.size());
        WeightScaleMesg decoded = messages.get(0);
        assertEquals(64.2, decoded.getWeight(), 0.01);
        assertEquals(26.3, decoded.getPercentFat(), 0.01);
        assertEquals(52.4, decoded.getPercentHydration(), 0.01);
        assertEquals(2.4, decoded.getBoneMass(), 0.01);
        assertEquals(45.1, decoded.getMuscleMass(), 0.01);
        assertEquals(22.8, decoded.getBmi(), 0.01);
        assertEquals("41755a93625f966cf905e3d3758ac6adebbfa2de0cec41f1e9a5aec9b6dfdcfb", sha256(output));
    }

    private static String sha256(File file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format("%02x", value));
        return hex.toString();
    }
}
