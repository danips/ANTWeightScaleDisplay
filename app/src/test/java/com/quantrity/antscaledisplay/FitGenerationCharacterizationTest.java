package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.garmin.fit.Decode;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;

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
        assertEquals("71b74f44bf2e59a96330997cf345ebdfdec3696c6687958b62916a1bf9803fbe", sha256(output));
    }

    private static String sha256(File file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format("%02x", value));
        return hex.toString();
    }
}
