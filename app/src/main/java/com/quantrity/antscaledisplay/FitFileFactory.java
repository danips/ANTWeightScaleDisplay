package com.quantrity.antscaledisplay;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Creates the small FIT weight payload required by Garmin Connect. */
final class FitFileFactory {
    private static final int HEADER_SIZE = 14;
    private static final int PROTOCOL_VERSION_2_0 = 0x20;
    private static final int PROFILE_VERSION_21_205 = 21_205;
    private static final long FIT_EPOCH_MILLIS = 631_065_600_000L;

    private static final int DEFINITION_HEADER = 0x40;
    private static final int LOCAL_MESSAGE = 0;
    private static final int FILE_ID_MESSAGE = 0;
    private static final int WEIGHT_SCALE_MESSAGE = 30;

    private static final int BASE_TYPE_ENUM = 0x00;
    private static final int BASE_TYPE_UINT8 = 0x02;
    private static final int BASE_TYPE_UINT16 = 0x84;
    private static final int BASE_TYPE_UINT32 = 0x86;
    private static final int BASE_TYPE_UINT32Z = 0x8C;

    private static final int FILE_TYPE_WEIGHT = 9;
    private static final int MANUFACTURER_TANITA = 11;
    private static final long MAX_UINT8 = 0xFEL;
    private static final long MAX_UINT16 = 0xFFFEL;
    private static final long MAX_UINT32 = 0xFFFF_FFFEL;

    File create(File output, Weight weight) {
        if (output == null || weight == null) {
            throw new IllegalArgumentException("FIT output and weight are required");
        }

        List<Field> weightFields = new ArrayList<>();
        weightFields.add(new Field(253, 4, BASE_TYPE_UINT32,
                fitTimestamp(weight.date)));
        weightFields.add(scaledUint16(0, weight.weight, 100, "weight"));
        addOptionalScaled(weightFields, 1, weight.percentFat, 100, "percent fat");
        addOptionalScaled(weightFields, 2, weight.percentHydration, 100,
                "percent hydration");
        addOptionalScaled(weightFields, 4, weight.boneMass, 100, "bone mass");
        addOptionalScaled(weightFields, 5, weight.muscleMass, 100, "muscle mass");
        addOptionalUint8(weightFields, 8, weight.physiqueRating, "physique rating");
        addOptionalRoundedUint8(weightFields, 11, weight.visceralFatRating,
                "visceral fat rating");
        addOptionalUint8(weightFields, 10, weight.metabolicAge, "metabolic age");

        double activeMet = weight.basalMet != -1 ? weight.basalMet : weight.activeMet;
        addOptionalScaled(weightFields, 9, activeMet, 4, "active metabolism");
        if (weight.height > 0) {
            double heightMetres = weight.height / 100;
            double bmi = weight.weight / (heightMetres * heightMetres);
            weightFields.add(scaledUint16(13, bmi, 10, "BMI"));
        }

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        List<Field> fileFields = new ArrayList<>();
        fileFields.add(new Field(0, 1, BASE_TYPE_ENUM, FILE_TYPE_WEIGHT));
        fileFields.add(new Field(1, 2, BASE_TYPE_UINT16, MANUFACTURER_TANITA));
        fileFields.add(new Field(2, 2, BASE_TYPE_UINT16, 1));
        fileFields.add(new Field(3, 4, BASE_TYPE_UINT32Z, 1));
        writeMessage(data, FILE_ID_MESSAGE, fileFields);
        writeMessage(data, WEIGHT_SCALE_MESSAGE, weightFields);

        byte[] dataBytes = data.toByteArray();
        byte[] header = createHeader(dataBytes.length);
        ByteArrayOutputStream file = new ByteArrayOutputStream(
                header.length + dataBytes.length + 2);
        file.write(header, 0, header.length);
        file.write(dataBytes, 0, dataBytes.length);
        writeLittleEndian(file, crc16(file.toByteArray()), 2);

        try (FileOutputStream stream = new FileOutputStream(output, false)) {
            file.writeTo(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create FIT file", exception);
        }
        return output;
    }

    private static byte[] createHeader(int dataSize) {
        ByteArrayOutputStream header = new ByteArrayOutputStream(HEADER_SIZE);
        header.write(HEADER_SIZE);
        header.write(PROTOCOL_VERSION_2_0);
        writeLittleEndian(header, PROFILE_VERSION_21_205, 2);
        writeLittleEndian(header, dataSize, 4);
        header.write('.');
        header.write('F');
        header.write('I');
        header.write('T');
        writeLittleEndian(header, crc16(header.toByteArray()), 2);
        return header.toByteArray();
    }

    private static void writeMessage(ByteArrayOutputStream output, int globalMessage,
                                     List<Field> fields) {
        output.write(DEFINITION_HEADER | LOCAL_MESSAGE);
        output.write(0); // Reserved.
        output.write(0); // Little-endian architecture.
        writeLittleEndian(output, globalMessage, 2);
        output.write(fields.size());
        for (Field field : fields) {
            output.write(field.number);
            output.write(field.size);
            output.write(field.baseType);
        }

        output.write(LOCAL_MESSAGE);
        for (Field field : fields) writeLittleEndian(output, field.value, field.size);
    }

    private static Field scaledUint16(int number, double value, int scale, String name) {
        return new Field(number, 2, BASE_TYPE_UINT16,
                encodeScaled(value, scale, MAX_UINT16, name));
    }

    private static void addOptionalScaled(List<Field> fields, int number, double value,
                                          int scale, String name) {
        if (value != -1) fields.add(scaledUint16(number, value, scale, name));
    }

    private static void addOptionalUint8(List<Field> fields, int number, long value, String name) {
        if (value == -1) return;
        if (value < 0 || value > MAX_UINT8) throw outOfRange(name);
        fields.add(new Field(number, 1, BASE_TYPE_UINT8, value));
    }

    private static void addOptionalRoundedUint8(List<Field> fields, int number, double value,
                                                String name) {
        if (value == -1) return;
        if (Double.isNaN(value) || Double.isInfinite(value)) throw outOfRange(name);
        addOptionalUint8(fields, number, Math.round(value), name);
    }

    private static long encodeScaled(double value, int scale, long maximum, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw outOfRange(name);
        }
        double scaled = (float) value * scale;
        if (scaled > maximum + 0.5) throw outOfRange(name);
        long encoded = Math.round(scaled);
        if (encoded > maximum) throw outOfRange(name);
        return encoded;
    }

    private static long fitTimestamp(long unixMillis) {
        if (unixMillis < FIT_EPOCH_MILLIS) throw outOfRange("timestamp");
        long timestamp = (unixMillis - FIT_EPOCH_MILLIS) / 1000;
        if (timestamp > MAX_UINT32) throw outOfRange("timestamp");
        return timestamp;
    }

    private static IllegalArgumentException outOfRange(String name) {
        return new IllegalArgumentException("FIT " + name + " is out of range");
    }

    private static void writeLittleEndian(ByteArrayOutputStream output, long value, int size) {
        for (int index = 0; index < size; index++) {
            output.write((int) (value >>> (index * 8)) & 0xFF);
        }
    }

    private static int crc16(byte[] bytes) {
        int crc = 0;
        for (byte value : bytes) {
            crc ^= value & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0xA001 : crc >>> 1;
            }
        }
        return crc & 0xFFFF;
    }

    private static final class Field {
        final int number;
        final int size;
        final int baseType;
        final long value;

        Field(int number, int size, int baseType, long value) {
            this.number = number;
            this.size = size;
            this.baseType = baseType;
            this.value = value;
        }
    }
}
