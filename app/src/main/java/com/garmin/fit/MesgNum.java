////////////////////////////////////////////////////////////////////////////////
// The following FIT Protocol software provided may be used with FIT protocol
// devices only and remains the copyrighted property of Dynastream Innovations Inc.
// The software is being provided on an "as-is" basis and as an accommodation,
// and therefore all warranties, representations, or guarantees of any kind
// (whether express, implied or statutory) including, without limitation,
// warranties of merchantability, non-infringement, or fitness for a particular
// purpose, are specifically disclaimed.
//
// Copyright 2017 Dynastream Innovations Inc.
////////////////////////////////////////////////////////////////////////////////
// ****WARNING****  This file is auto-generated!  Do NOT edit this file.
// Profile Version = 20.38Release
// Tag = production/akw/20.38.00-0-geccbce3
////////////////////////////////////////////////////////////////////////////////


package com.garmin.fit;

import java.util.HashMap;
import java.util.Map;

public class MesgNum {
    public static final int FILE_ID = 0;
    public static final int CAPABILITIES = 1;
    public static final int DEVICE_SETTINGS = 2;
    public static final int USER_PROFILE = 3;
    public static final int HRM_PROFILE = 4;
    public static final int SDM_PROFILE = 5;
    public static final int BIKE_PROFILE = 6;
    public static final int ZONES_TARGET = 7;
    public static final int HR_ZONE = 8;
    public static final int POWER_ZONE = 9;
    public static final int MET_ZONE = 10;
    public static final int SPORT = 12;
    public static final int GOAL = 15;
    public static final int SESSION = 18;
    public static final int LAP = 19;
    public static final int RECORD = 20;
    public static final int EVENT = 21;
    public static final int DEVICE_INFO = 23;
    public static final int WORKOUT = 26;
    public static final int WORKOUT_STEP = 27;
    public static final int SCHEDULE = 28;
    public static final int WEIGHT_SCALE = 30;
    public static final int COURSE = 31;
    public static final int COURSE_POINT = 32;
    public static final int TOTALS = 33;
    public static final int ACTIVITY = 34;
    public static final int SOFTWARE = 35;
    public static final int FILE_CAPABILITIES = 37;
    public static final int MESG_CAPABILITIES = 38;
    public static final int FIELD_CAPABILITIES = 39;
    public static final int FILE_CREATOR = 49;
    public static final int BLOOD_PRESSURE = 51;
    public static final int SPEED_ZONE = 53;
    public static final int MONITORING = 55;
    public static final int TRAINING_FILE = 72;
    public static final int HRV = 78;
    public static final int ANT_RX = 80;
    public static final int ANT_TX = 81;
    public static final int ANT_CHANNEL_ID = 82;
    public static final int LENGTH = 101;
    public static final int MONITORING_INFO = 103;
    public static final int PAD = 105;
    public static final int SLAVE_DEVICE = 106;
    public static final int CONNECTIVITY = 127;
    public static final int WEATHER_CONDITIONS = 128;
    public static final int WEATHER_ALERT = 129;
    public static final int CADENCE_ZONE = 131;
    public static final int HR = 132;
    public static final int SEGMENT_LAP = 142;
    public static final int MEMO_GLOB = 145;
    public static final int SEGMENT_ID = 148;
    public static final int SEGMENT_LEADERBOARD_ENTRY = 149;
    public static final int SEGMENT_POINT = 150;
    public static final int SEGMENT_FILE = 151;
    public static final int WORKOUT_SESSION = 158;
    public static final int WATCHFACE_SETTINGS = 159;
    public static final int GPS_METADATA = 160;
    public static final int CAMERA_EVENT = 161;
    public static final int TIMESTAMP_CORRELATION = 162;
    public static final int GYROSCOPE_DATA = 164;
    public static final int ACCELEROMETER_DATA = 165;
    public static final int THREE_D_SENSOR_CALIBRATION = 167;
    public static final int VIDEO_FRAME = 169;
    public static final int OBDII_DATA = 174;
    public static final int NMEA_SENTENCE = 177;
    public static final int AVIATION_ATTITUDE = 178;
    public static final int VIDEO = 184;
    public static final int VIDEO_TITLE = 185;
    public static final int VIDEO_DESCRIPTION = 186;
    public static final int VIDEO_CLIP = 187;
    public static final int OHR_SETTINGS = 188;
    public static final int EXD_SCREEN_CONFIGURATION = 200;
    public static final int EXD_DATA_FIELD_CONFIGURATION = 201;
    public static final int EXD_DATA_CONCEPT_CONFIGURATION = 202;
    public static final int FIELD_DESCRIPTION = 206;
    public static final int DEVELOPER_DATA_ID = 207;
    public static final int MAGNETOMETER_DATA = 208;
    public static final int MFG_RANGE_MIN = 0xFF00; // 0xFF00 - 0xFFFE reserved for manufacturer specific messages
    public static final int MFG_RANGE_MAX = 0xFFFE; // 0xFF00 - 0xFFFE reserved for manufacturer specific messages
    public static final int INVALID = Fit.UINT16_INVALID;

    private static final Map<Integer, String> stringMap;

    static {
        stringMap = new HashMap<>();
        stringMap.put(FILE_ID, "FILE_ID");
        stringMap.put(CAPABILITIES, "CAPABILITIES");
        stringMap.put(DEVICE_SETTINGS, "DEVICE_SETTINGS");
        stringMap.put(USER_PROFILE, "USER_PROFILE");
        stringMap.put(HRM_PROFILE, "HRM_PROFILE");
        stringMap.put(SDM_PROFILE, "SDM_PROFILE");
        stringMap.put(BIKE_PROFILE, "BIKE_PROFILE");
        stringMap.put(ZONES_TARGET, "ZONES_TARGET");
        stringMap.put(HR_ZONE, "HR_ZONE");
        stringMap.put(POWER_ZONE, "POWER_ZONE");
        stringMap.put(MET_ZONE, "MET_ZONE");
        stringMap.put(SPORT, "SPORT");
        stringMap.put(GOAL, "GOAL");
        stringMap.put(SESSION, "SESSION");
        stringMap.put(LAP, "LAP");
        stringMap.put(RECORD, "RECORD");
        stringMap.put(EVENT, "EVENT");
        stringMap.put(DEVICE_INFO, "DEVICE_INFO");
        stringMap.put(WORKOUT, "WORKOUT");
        stringMap.put(WORKOUT_STEP, "WORKOUT_STEP");
        stringMap.put(SCHEDULE, "SCHEDULE");
        stringMap.put(WEIGHT_SCALE, "WEIGHT_SCALE");
        stringMap.put(COURSE, "COURSE");
        stringMap.put(COURSE_POINT, "COURSE_POINT");
        stringMap.put(TOTALS, "TOTALS");
        stringMap.put(ACTIVITY, "ACTIVITY");
        stringMap.put(SOFTWARE, "SOFTWARE");
        stringMap.put(FILE_CAPABILITIES, "FILE_CAPABILITIES");
        stringMap.put(MESG_CAPABILITIES, "MESG_CAPABILITIES");
        stringMap.put(FIELD_CAPABILITIES, "FIELD_CAPABILITIES");
        stringMap.put(FILE_CREATOR, "FILE_CREATOR");
        stringMap.put(BLOOD_PRESSURE, "BLOOD_PRESSURE");
        stringMap.put(SPEED_ZONE, "SPEED_ZONE");
        stringMap.put(MONITORING, "MONITORING");
        stringMap.put(TRAINING_FILE, "TRAINING_FILE");
        stringMap.put(HRV, "HRV");
        stringMap.put(ANT_RX, "ANT_RX");
        stringMap.put(ANT_TX, "ANT_TX");
        stringMap.put(ANT_CHANNEL_ID, "ANT_CHANNEL_ID");
        stringMap.put(LENGTH, "LENGTH");
        stringMap.put(MONITORING_INFO, "MONITORING_INFO");
        stringMap.put(PAD, "PAD");
        stringMap.put(SLAVE_DEVICE, "SLAVE_DEVICE");
        stringMap.put(CONNECTIVITY, "CONNECTIVITY");
        stringMap.put(WEATHER_CONDITIONS, "WEATHER_CONDITIONS");
        stringMap.put(WEATHER_ALERT, "WEATHER_ALERT");
        stringMap.put(CADENCE_ZONE, "CADENCE_ZONE");
        stringMap.put(HR, "HR");
        stringMap.put(SEGMENT_LAP, "SEGMENT_LAP");
        stringMap.put(MEMO_GLOB, "MEMO_GLOB");
        stringMap.put(SEGMENT_ID, "SEGMENT_ID");
        stringMap.put(SEGMENT_LEADERBOARD_ENTRY, "SEGMENT_LEADERBOARD_ENTRY");
        stringMap.put(SEGMENT_POINT, "SEGMENT_POINT");
        stringMap.put(SEGMENT_FILE, "SEGMENT_FILE");
        stringMap.put(WORKOUT_SESSION, "WORKOUT_SESSION");
        stringMap.put(WATCHFACE_SETTINGS, "WATCHFACE_SETTINGS");
        stringMap.put(GPS_METADATA, "GPS_METADATA");
        stringMap.put(CAMERA_EVENT, "CAMERA_EVENT");
        stringMap.put(TIMESTAMP_CORRELATION, "TIMESTAMP_CORRELATION");
        stringMap.put(GYROSCOPE_DATA, "GYROSCOPE_DATA");
        stringMap.put(ACCELEROMETER_DATA, "ACCELEROMETER_DATA");
        stringMap.put(THREE_D_SENSOR_CALIBRATION, "THREE_D_SENSOR_CALIBRATION");
        stringMap.put(VIDEO_FRAME, "VIDEO_FRAME");
        stringMap.put(OBDII_DATA, "OBDII_DATA");
        stringMap.put(NMEA_SENTENCE, "NMEA_SENTENCE");
        stringMap.put(AVIATION_ATTITUDE, "AVIATION_ATTITUDE");
        stringMap.put(VIDEO, "VIDEO");
        stringMap.put(VIDEO_TITLE, "VIDEO_TITLE");
        stringMap.put(VIDEO_DESCRIPTION, "VIDEO_DESCRIPTION");
        stringMap.put(VIDEO_CLIP, "VIDEO_CLIP");
        stringMap.put(OHR_SETTINGS, "OHR_SETTINGS");
        stringMap.put(EXD_SCREEN_CONFIGURATION, "EXD_SCREEN_CONFIGURATION");
        stringMap.put(EXD_DATA_FIELD_CONFIGURATION, "EXD_DATA_FIELD_CONFIGURATION");
        stringMap.put(EXD_DATA_CONCEPT_CONFIGURATION, "EXD_DATA_CONCEPT_CONFIGURATION");
        stringMap.put(FIELD_DESCRIPTION, "FIELD_DESCRIPTION");
        stringMap.put(DEVELOPER_DATA_ID, "DEVELOPER_DATA_ID");
        stringMap.put(MAGNETOMETER_DATA, "MAGNETOMETER_DATA");
        stringMap.put(MFG_RANGE_MIN, "MFG_RANGE_MIN");
        stringMap.put(MFG_RANGE_MAX, "MFG_RANGE_MAX");
    }


    /**
     * Retrieves the String Representation of the Value
     * @return The string representation of the value, or empty if unknown
     */
    public static String getStringFromValue( Integer value ) {
        if( stringMap.containsKey( value ) ) {
            return stringMap.get( value );
        }

        return "";
    }

    /**
     * Retrieves a value given a string representation
     * @return The value or INVALID if unkwown
     */
    public static Integer getValueFromString( String value ) {
        for( Map.Entry<Integer, String> entry : stringMap.entrySet() ) {
            if( entry.getValue().equals( value ) ) {
                return entry.getKey();
            }
        }

        return INVALID;
    }

}
