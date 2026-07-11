package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.WeightScaleMesg;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Date;

public class FitGenerationCharacterizationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedWeightFileIsValidFit() throws Exception {
        File output = temporaryFolder.newFile("weight.fit");

        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.WEIGHT);
        fileId.setManufacturer(Manufacturer.TANITA);
        fileId.setProduct(1);
        fileId.setSerialNumber(1L);

        WeightScaleMesg weight = new WeightScaleMesg();
        weight.setTimestamp(new DateTime(new Date(1_783_728_000_000L)));
        weight.setWeight(64.2f);
        weight.setPercentFat(26.3f);
        weight.setPercentHydration(52.4f);
        weight.setBoneMass(2.4f);
        weight.setMuscleMass(45.1f);
        weight.setBmi(22.75f);

        FileEncoder encoder = new FileEncoder(output, Fit.ProtocolVersion.V2_0);
        encoder.write(fileId);
        encoder.write(weight);
        encoder.close();

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
