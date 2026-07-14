package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.garmin.fit.Decode;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.MesgNum;
import com.garmin.fit.WeightScaleMesg;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;

public class FitGenerationCharacterizationTest {
    private static final long FIT_EPOCH_MILLIS = 631_065_600_000L;
    private static final long MAX_FIT_DATE_MILLIS =
            FIT_EPOCH_MILLIS + 0xFFFF_FFFEL * 1000;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedWeightFileIsValidFitAndContainsEverySupportedValue() throws Exception {
        Weight weight = basicWeight();
        weight.height = Math.sqrt(64.2 / 22.75) * 100;
        weight.percentFat = 26.3;
        weight.percentHydration = 52.4;
        weight.boneMass = 2.4;
        weight.muscleMass = 45.1;
        weight.physiqueRating = 7;
        weight.visceralFatRating = 12.6;
        weight.metabolicAge = 35;
        weight.basalMet = 1420;
        weight.activeMet = 1800;

        DecodedFit decoded = createAndDecode(weight);

        assertEquals(com.garmin.fit.File.WEIGHT, decoded.fileId.getType());
        assertEquals(Manufacturer.TANITA, decoded.fileId.getManufacturer().intValue());
        assertEquals(Integer.valueOf(1), decoded.fileId.getProduct());
        assertEquals(Long.valueOf(1), decoded.fileId.getSerialNumber());
        assertEquals(weight.date, decoded.weight.getTimestamp().getDate().getTime());
        assertEquals(64.2, decoded.weight.getWeight(), 0.01);
        assertEquals(26.3, decoded.weight.getPercentFat(), 0.01);
        assertEquals(52.4, decoded.weight.getPercentHydration(), 0.01);
        assertEquals(2.4, decoded.weight.getBoneMass(), 0.01);
        assertEquals(45.1, decoded.weight.getMuscleMass(), 0.01);
        assertEquals(7, decoded.weight.getPhysiqueRating().intValue());
        assertEquals(13, decoded.weight.getVisceralFatRating().intValue());
        assertEquals(35, decoded.weight.getMetabolicAge().intValue());
        assertEquals(1420, decoded.weight.getActiveMet(), 0.01);
        assertNull(decoded.weight.getBasalMet());
        assertEquals(22.8, decoded.weight.getBmi(), 0.01);
    }

    @Test
    public void definitionsAndValuesUseLittleEndianFit20Header() throws Exception {
        File output = create(basicWeight());
        byte[] bytes = Files.readAllBytes(output.toPath());

        assertEquals(14, bytes[0] & 0xFF);
        assertEquals(0x20, bytes[1] & 0xFF);
        assertEquals(21_205, unsignedLittleEndian(bytes, 2, 2));
        assertEquals(bytes.length - 16, unsignedLittleEndian(bytes, 4, 4));
        assertEquals('.', bytes[8]);
        assertEquals('F', bytes[9]);
        assertEquals('I', bytes[10]);
        assertEquals('T', bytes[11]);
        assertEquals(0, bytes[16] & 0xFF); // file_id definition architecture
        assertEquals(0, bytes[44] & 0xFF); // weight_scale definition architecture
    }

    @Test
    public void missingOptionalValuesAreNotEncoded() throws Exception {
        WeightScaleMesg decoded = createAndDecode(basicWeight()).weight;

        assertNull(decoded.getPercentFat());
        assertNull(decoded.getPercentHydration());
        assertNull(decoded.getBoneMass());
        assertNull(decoded.getMuscleMass());
        assertNull(decoded.getPhysiqueRating());
        assertNull(decoded.getVisceralFatRating());
        assertNull(decoded.getMetabolicAge());
        assertNull(decoded.getActiveMet());
        assertNull(decoded.getBmi());
    }

    @Test
    public void maximumEncodableValuesRemainValid() throws Exception {
        Weight weight = basicWeight();
        weight.date = MAX_FIT_DATE_MILLIS;
        weight.weight = 655.34;
        weight.percentFat = 655.34;
        weight.percentHydration = 655.34;
        weight.boneMass = 655.34;
        weight.muscleMass = 655.34;
        weight.physiqueRating = 254;
        weight.visceralFatRating = 254;
        weight.metabolicAge = 254;
        weight.activeMet = 16_383.5;
        weight.height = Math.sqrt(weight.weight / 6553.4) * 100;

        WeightScaleMesg decoded = createAndDecode(weight).weight;

        assertEquals(MAX_FIT_DATE_MILLIS, decoded.getTimestamp().getDate().getTime());
        assertEquals(655.34, decoded.getWeight(), 0.01);
        assertEquals(655.34, decoded.getPercentFat(), 0.01);
        assertEquals(655.34, decoded.getPercentHydration(), 0.01);
        assertEquals(655.34, decoded.getBoneMass(), 0.01);
        assertEquals(655.34, decoded.getMuscleMass(), 0.01);
        assertEquals(254, decoded.getPhysiqueRating().intValue());
        assertEquals(254, decoded.getVisceralFatRating().intValue());
        assertEquals(254, decoded.getMetabolicAge().intValue());
        assertEquals(16_383.5, decoded.getActiveMet(), 0.01);
        assertEquals(6553.4, decoded.getBmi(), 0.01);
    }

    @Test
    public void scaledValuesRoundToNearestRepresentableValue() throws Exception {
        Weight weight = basicWeight();
        weight.weight = 64.205;
        weight.percentFat = 26.345;
        weight.activeMet = 100.125;
        weight.visceralFatRating = 12.5;

        WeightScaleMesg decoded = createAndDecode(weight).weight;

        assertEquals(64.21, decoded.getWeight(), 0.001);
        assertEquals(26.35, decoded.getPercentFat(), 0.001);
        assertEquals(100.25, decoded.getActiveMet(), 0.001);
        assertEquals(13, decoded.getVisceralFatRating().intValue());
    }

    @Test
    public void fitEpochTimestampIsSupported() throws Exception {
        Weight weight = basicWeight();
        weight.date = FIT_EPOCH_MILLIS;

        assertEquals(FIT_EPOCH_MILLIS,
                createAndDecode(weight).weight.getTimestamp().getDate().getTime());
    }

    @Test
    public void invalidRequiredAndOverflowValuesAreRejected() {
        Weight missingWeight = basicWeight();
        missingWeight.weight = -1;
        assertThrows(IllegalArgumentException.class, () -> create(missingWeight));

        Weight oldDate = basicWeight();
        oldDate.date = FIT_EPOCH_MILLIS - 1;
        assertThrows(IllegalArgumentException.class, () -> create(oldDate));

        Weight overflow = basicWeight();
        overflow.percentFat = 655.35;
        assertThrows(IllegalArgumentException.class, () -> create(overflow));

        Weight nonFinite = basicWeight();
        nonFinite.visceralFatRating = Double.NaN;
        assertThrows(IllegalArgumentException.class, () -> create(nonFinite));
    }

    @Test
    public void headerAndFileCrcDetectCorruption() throws Exception {
        File valid = create(basicWeight());
        byte[] bytes = Files.readAllBytes(valid.toPath());

        File damagedHeader = temporaryFolder.newFile("damaged-header.fit");
        byte[] headerBytes = bytes.clone();
        headerBytes[2] ^= 1;
        Files.write(damagedHeader.toPath(), headerBytes);
        assertFalse(hasValidIntegrity(damagedHeader));

        File damagedData = temporaryFolder.newFile("damaged-data.fit");
        byte[] dataBytes = bytes.clone();
        dataBytes[32] ^= 1;
        Files.write(damagedData.toPath(), dataBytes);
        assertFalse(hasValidIntegrity(damagedData));
    }

    private DecodedFit createAndDecode(Weight weight) throws Exception {
        File output = create(weight);
        assertTrue(output.length() > 16);
        assertTrue(hasValidIntegrity(output));

        ArrayList<FileIdMesg> fileIds = new ArrayList<>();
        ArrayList<WeightScaleMesg> weights = new ArrayList<>();
        try (FileInputStream input = new FileInputStream(output)) {
            assertTrue(new Decode().read(input, message -> {
                if (message.getNum() == MesgNum.FILE_ID) {
                    fileIds.add(new FileIdMesg(message));
                } else if (message.getNum() == MesgNum.WEIGHT_SCALE) {
                    weights.add(new WeightScaleMesg(message));
                }
            }));
        }
        assertEquals(1, fileIds.size());
        assertEquals(1, weights.size());
        return new DecodedFit(fileIds.get(0), weights.get(0));
    }

    private File create(Weight weight) throws Exception {
        return new FitFileFactory().create(temporaryFolder.newFile(), weight);
    }

    private static boolean hasValidIntegrity(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            return new Decode().checkFileIntegrity(input);
        }
    }

    private static long unsignedLittleEndian(byte[] bytes, int offset, int size) {
        long value = 0;
        for (int index = 0; index < size; index++) {
            value |= (long) (bytes[offset + index] & 0xFF) << (index * 8);
        }
        return value;
    }

    private static Weight basicWeight() {
        Weight weight = new Weight();
        weight.date = 1_783_728_000_000L;
        weight.weight = 64.2;
        return weight;
    }

    private static final class DecodedFit {
        final FileIdMesg fileId;
        final WeightScaleMesg weight;

        DecodedFit(FileIdMesg fileId, WeightScaleMesg weight) {
            this.fileId = fileId;
            this.weight = weight;
        }
    }
}
