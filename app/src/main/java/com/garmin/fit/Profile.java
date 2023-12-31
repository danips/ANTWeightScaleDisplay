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

public class Profile {
   public static class SubFields {
      // file_id message, product field
      public static final int FILE_ID_MESG_PRODUCT_FIELD_GARMIN_PRODUCT = 0;
      public static final int FILE_ID_MESG_PRODUCT_FIELD_SUBFIELDS = 1;
      public static final int FILE_ID_MESG_PRODUCT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int FILE_ID_MESG_PRODUCT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // slave_device message, product field
      public static final int SLAVE_DEVICE_MESG_PRODUCT_FIELD_GARMIN_PRODUCT = 0;
      public static final int SLAVE_DEVICE_MESG_PRODUCT_FIELD_SUBFIELDS = 1;
      public static final int SLAVE_DEVICE_MESG_PRODUCT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SLAVE_DEVICE_MESG_PRODUCT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // mesg_capabilities message, count field
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_NUM_PER_FILE = 0;
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_MAX_PER_FILE = 1;
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_MAX_PER_FILE_TYPE = 2;
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_SUBFIELDS = 3;
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int MESG_CAPABILITIES_MESG_COUNT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // watchface_settings message, layout field
      public static final int WATCHFACE_SETTINGS_MESG_LAYOUT_FIELD_DIGITAL_LAYOUT = 0;
      public static final int WATCHFACE_SETTINGS_MESG_LAYOUT_FIELD_ANALOG_LAYOUT = 1;
      public static final int WATCHFACE_SETTINGS_MESG_LAYOUT_FIELD_SUBFIELDS = 2;
      public static final int WATCHFACE_SETTINGS_MESG_LAYOUT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int WATCHFACE_SETTINGS_MESG_LAYOUT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // session message, total_cycles field
      public static final int SESSION_MESG_TOTAL_CYCLES_FIELD_TOTAL_STRIDES = 0;
      public static final int SESSION_MESG_TOTAL_CYCLES_FIELD_SUBFIELDS = 1;
      public static final int SESSION_MESG_TOTAL_CYCLES_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SESSION_MESG_TOTAL_CYCLES_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // session message, avg_cadence field
      public static final int SESSION_MESG_AVG_CADENCE_FIELD_AVG_RUNNING_CADENCE = 0;
      public static final int SESSION_MESG_AVG_CADENCE_FIELD_SUBFIELDS = 1;
      public static final int SESSION_MESG_AVG_CADENCE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SESSION_MESG_AVG_CADENCE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // session message, max_cadence field
      public static final int SESSION_MESG_MAX_CADENCE_FIELD_MAX_RUNNING_CADENCE = 0;
      public static final int SESSION_MESG_MAX_CADENCE_FIELD_SUBFIELDS = 1;
      public static final int SESSION_MESG_MAX_CADENCE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SESSION_MESG_MAX_CADENCE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // lap message, total_cycles field
      public static final int LAP_MESG_TOTAL_CYCLES_FIELD_TOTAL_STRIDES = 0;
      public static final int LAP_MESG_TOTAL_CYCLES_FIELD_SUBFIELDS = 1;
      public static final int LAP_MESG_TOTAL_CYCLES_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int LAP_MESG_TOTAL_CYCLES_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // lap message, avg_cadence field
      public static final int LAP_MESG_AVG_CADENCE_FIELD_AVG_RUNNING_CADENCE = 0;
      public static final int LAP_MESG_AVG_CADENCE_FIELD_SUBFIELDS = 1;
      public static final int LAP_MESG_AVG_CADENCE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int LAP_MESG_AVG_CADENCE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // lap message, max_cadence field
      public static final int LAP_MESG_MAX_CADENCE_FIELD_MAX_RUNNING_CADENCE = 0;
      public static final int LAP_MESG_MAX_CADENCE_FIELD_SUBFIELDS = 1;
      public static final int LAP_MESG_MAX_CADENCE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int LAP_MESG_MAX_CADENCE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // event message, data field
      public static final int EVENT_MESG_DATA_FIELD_TIMER_TRIGGER = 0;
      public static final int EVENT_MESG_DATA_FIELD_COURSE_POINT_INDEX = 1;
      public static final int EVENT_MESG_DATA_FIELD_BATTERY_LEVEL = 2;
      public static final int EVENT_MESG_DATA_FIELD_VIRTUAL_PARTNER_SPEED = 3;
      public static final int EVENT_MESG_DATA_FIELD_HR_HIGH_ALERT = 4;
      public static final int EVENT_MESG_DATA_FIELD_HR_LOW_ALERT = 5;
      public static final int EVENT_MESG_DATA_FIELD_SPEED_HIGH_ALERT = 6;
      public static final int EVENT_MESG_DATA_FIELD_SPEED_LOW_ALERT = 7;
      public static final int EVENT_MESG_DATA_FIELD_CAD_HIGH_ALERT = 8;
      public static final int EVENT_MESG_DATA_FIELD_CAD_LOW_ALERT = 9;
      public static final int EVENT_MESG_DATA_FIELD_POWER_HIGH_ALERT = 10;
      public static final int EVENT_MESG_DATA_FIELD_POWER_LOW_ALERT = 11;
      public static final int EVENT_MESG_DATA_FIELD_TIME_DURATION_ALERT = 12;
      public static final int EVENT_MESG_DATA_FIELD_DISTANCE_DURATION_ALERT = 13;
      public static final int EVENT_MESG_DATA_FIELD_CALORIE_DURATION_ALERT = 14;
      public static final int EVENT_MESG_DATA_FIELD_FITNESS_EQUIPMENT_STATE = 15;
      public static final int EVENT_MESG_DATA_FIELD_SPORT_POINT = 16;
      public static final int EVENT_MESG_DATA_FIELD_GEAR_CHANGE_DATA = 17;
      public static final int EVENT_MESG_DATA_FIELD_RIDER_POSITION = 18;
      public static final int EVENT_MESG_DATA_FIELD_COMM_TIMEOUT = 19;
      public static final int EVENT_MESG_DATA_FIELD_SUBFIELDS = 20;
      public static final int EVENT_MESG_DATA_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int EVENT_MESG_DATA_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // device_info message, device_type field
      public static final int DEVICE_INFO_MESG_DEVICE_TYPE_FIELD_ANTPLUS_DEVICE_TYPE = 0;
      public static final int DEVICE_INFO_MESG_DEVICE_TYPE_FIELD_ANT_DEVICE_TYPE = 1;
      public static final int DEVICE_INFO_MESG_DEVICE_TYPE_FIELD_SUBFIELDS = 2;
      public static final int DEVICE_INFO_MESG_DEVICE_TYPE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int DEVICE_INFO_MESG_DEVICE_TYPE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // device_info message, product field
      public static final int DEVICE_INFO_MESG_PRODUCT_FIELD_GARMIN_PRODUCT = 0;
      public static final int DEVICE_INFO_MESG_PRODUCT_FIELD_SUBFIELDS = 1;
      public static final int DEVICE_INFO_MESG_PRODUCT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int DEVICE_INFO_MESG_PRODUCT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // training_file message, product field
      public static final int TRAINING_FILE_MESG_PRODUCT_FIELD_GARMIN_PRODUCT = 0;
      public static final int TRAINING_FILE_MESG_PRODUCT_FIELD_SUBFIELDS = 1;
      public static final int TRAINING_FILE_MESG_PRODUCT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int TRAINING_FILE_MESG_PRODUCT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // three_d_sensor_calibration message, calibration_factor field
      public static final int THREE_D_SENSOR_CALIBRATION_MESG_CALIBRATION_FACTOR_FIELD_ACCEL_CAL_FACTOR = 0;
      public static final int THREE_D_SENSOR_CALIBRATION_MESG_CALIBRATION_FACTOR_FIELD_GYRO_CAL_FACTOR = 1;
      public static final int THREE_D_SENSOR_CALIBRATION_MESG_CALIBRATION_FACTOR_FIELD_SUBFIELDS = 2;
      public static final int THREE_D_SENSOR_CALIBRATION_MESG_CALIBRATION_FACTOR_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int THREE_D_SENSOR_CALIBRATION_MESG_CALIBRATION_FACTOR_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // segment_lap message, total_cycles field
      public static final int SEGMENT_LAP_MESG_TOTAL_CYCLES_FIELD_TOTAL_STROKES = 0;
      public static final int SEGMENT_LAP_MESG_TOTAL_CYCLES_FIELD_SUBFIELDS = 1;
      public static final int SEGMENT_LAP_MESG_TOTAL_CYCLES_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SEGMENT_LAP_MESG_TOTAL_CYCLES_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // workout_step message, duration_value field
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_TIME = 0;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_DISTANCE = 1;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_HR = 2;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_CALORIES = 3;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_STEP = 4;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_DURATION_POWER = 5;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_SUBFIELDS = 6;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int WORKOUT_STEP_MESG_DURATION_VALUE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // workout_step message, target_value field
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_TARGET_SPEED_ZONE = 0;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_TARGET_HR_ZONE = 1;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_TARGET_CADENCE_ZONE = 2;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_TARGET_POWER_ZONE = 3;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_STEPS = 4;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_TIME = 5;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_DISTANCE = 6;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_CALORIES = 7;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_HR = 8;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_REPEAT_POWER = 9;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_TARGET_STROKE_TYPE = 10;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_SUBFIELDS = 11;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int WORKOUT_STEP_MESG_TARGET_VALUE_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // workout_step message, custom_target_value_low field
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_CUSTOM_TARGET_SPEED_LOW = 0;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_CUSTOM_TARGET_HEART_RATE_LOW = 1;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_CUSTOM_TARGET_CADENCE_LOW = 2;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_CUSTOM_TARGET_POWER_LOW = 3;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_SUBFIELDS = 4;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_LOW_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // workout_step message, custom_target_value_high field
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_CUSTOM_TARGET_SPEED_HIGH = 0;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_CUSTOM_TARGET_HEART_RATE_HIGH = 1;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_CUSTOM_TARGET_CADENCE_HIGH = 2;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_CUSTOM_TARGET_POWER_HIGH = 3;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_SUBFIELDS = 4;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int WORKOUT_STEP_MESG_CUSTOM_TARGET_VALUE_HIGH_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // schedule message, product field
      public static final int SCHEDULE_MESG_PRODUCT_FIELD_GARMIN_PRODUCT = 0;
      public static final int SCHEDULE_MESG_PRODUCT_FIELD_SUBFIELDS = 1;
      public static final int SCHEDULE_MESG_PRODUCT_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int SCHEDULE_MESG_PRODUCT_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

      // monitoring message, cycles field
      public static final int MONITORING_MESG_CYCLES_FIELD_STEPS = 0;
      public static final int MONITORING_MESG_CYCLES_FIELD_STROKES = 1;
      public static final int MONITORING_MESG_CYCLES_FIELD_SUBFIELDS = 2;
      public static final int MONITORING_MESG_CYCLES_FIELD_ACTIVE_SUBFIELD = Fit.SUBFIELD_INDEX_ACTIVE_SUBFIELD;
      public static final int MONITORING_MESG_CYCLES_FIELD_MAIN_FIELD = Fit.SUBFIELD_INDEX_MAIN_FIELD;

   }

    public enum Type {
        ENUM,
        SINT8,
        UINT8,
        SINT16,
        UINT16,
        SINT32,
        UINT32,
        STRING,
        FLOAT32,
        FLOAT64,
        UINT8Z,
        UINT16Z,
        UINT32Z,
        BYTE,
        SINT64,
        UINT64,
        UINT64Z,
        BOOL,
        FILE,
        MESG_NUM,
        CHECKSUM,
        FILE_FLAGS,
        MESG_COUNT,
        DATE_TIME,
        LOCAL_DATE_TIME,
        MESSAGE_INDEX,
        DEVICE_INDEX,
        GENDER,
        LANGUAGE,
        LANGUAGE_BITS_0,
        LANGUAGE_BITS_1,
        LANGUAGE_BITS_2,
        LANGUAGE_BITS_3,
        LANGUAGE_BITS_4,
        TIME_ZONE,
        DISPLAY_MEASURE,
        DISPLAY_HEART,
        DISPLAY_POWER,
        DISPLAY_POSITION,
        SWITCH,
        SPORT,
        SPORT_BITS_0,
        SPORT_BITS_1,
        SPORT_BITS_2,
        SPORT_BITS_3,
        SPORT_BITS_4,
        SPORT_BITS_5,
        SPORT_BITS_6,
        SUB_SPORT,
        SPORT_EVENT,
        ACTIVITY,
        INTENSITY,
        SESSION_TRIGGER,
        AUTOLAP_TRIGGER,
        LAP_TRIGGER,
        TIME_MODE,
        BACKLIGHT_MODE,
        DATE_MODE,
        EVENT,
        EVENT_TYPE,
        TIMER_TRIGGER,
        FITNESS_EQUIPMENT_STATE,
        AUTOSCROLL,
        ACTIVITY_CLASS,
        HR_ZONE_CALC,
        PWR_ZONE_CALC,
        WKT_STEP_DURATION,
        WKT_STEP_TARGET,
        GOAL,
        GOAL_RECURRENCE,
        GOAL_SOURCE,
        SCHEDULE,
        COURSE_POINT,
        MANUFACTURER,
        GARMIN_PRODUCT,
        ANTPLUS_DEVICE_TYPE,
        ANT_NETWORK,
        WORKOUT_CAPABILITIES,
        BATTERY_STATUS,
        HR_TYPE,
        COURSE_CAPABILITIES,
        WEIGHT,
        WORKOUT_HR,
        WORKOUT_POWER,
        BP_STATUS,
        USER_LOCAL_ID,
        SWIM_STROKE,
        ACTIVITY_TYPE,
        ACTIVITY_SUBTYPE,
        ACTIVITY_LEVEL,
        SIDE,
        LEFT_RIGHT_BALANCE,
        LEFT_RIGHT_BALANCE_100,
        LENGTH_TYPE,
        DAY_OF_WEEK,
        CONNECTIVITY_CAPABILITIES,
        WEATHER_REPORT,
        WEATHER_STATUS,
        WEATHER_SEVERITY,
        WEATHER_SEVERE_TYPE,
        TIME_INTO_DAY,
        LOCALTIME_INTO_DAY,
        STROKE_TYPE,
        BODY_LOCATION,
        SEGMENT_LAP_STATUS,
        SEGMENT_LEADERBOARD_TYPE,
        SEGMENT_DELETE_STATUS,
        SEGMENT_SELECTION_TYPE,
        SOURCE_TYPE,
        DISPLAY_ORIENTATION,
        WORKOUT_EQUIPMENT,
        WATCHFACE_MODE,
        DIGITAL_WATCHFACE_LAYOUT,
        ANALOG_WATCHFACE_LAYOUT,
        RIDER_POSITION_TYPE,
        POWER_PHASE_TYPE,
        CAMERA_EVENT_TYPE,
        SENSOR_TYPE,
        BIKE_LIGHT_NETWORK_CONFIG_TYPE,
        COMM_TIMEOUT_TYPE,
        CAMERA_ORIENTATION_TYPE,
        ATTITUDE_STAGE,
        ATTITUDE_VALIDITY,
        AUTO_SYNC_FREQUENCY,
        EXD_LAYOUT,
        EXD_DISPLAY_TYPE,
        EXD_DATA_UNITS,
        EXD_QUALIFIERS,
        EXD_DESCRIPTORS,
        AUTO_ACTIVITY_DETECT,
        SUPPORTED_EXD_SCREEN_LAYOUTS,
        FIT_BASE_TYPE,
        TURN_TYPE,
        BIKE_LIGHT_BEAM_ANGLE_MODE,
        FIT_BASE_UNIT,
        
        NUM_TYPES;

        public static Type fromBaseType(final int baseType) {

            switch(baseType) {
                case Fit.BASE_TYPE_SINT8:
                    return Type.SINT8;
                case Fit.BASE_TYPE_UINT8:
                    return Type.UINT8;
                case Fit.BASE_TYPE_SINT16:
                    return Type.SINT16;
                case Fit.BASE_TYPE_UINT16:
                    return Type.UINT16;
                case Fit.BASE_TYPE_SINT32:
                    return Type.SINT32;
                case Fit.BASE_TYPE_UINT32:
                    return Type.UINT32;

                case Fit.BASE_TYPE_STRING:
                    return Type.STRING;

                case Fit.BASE_TYPE_FLOAT32:
                    return Type.FLOAT32;
                case Fit.BASE_TYPE_FLOAT64:
                    return Type.FLOAT64;

                case Fit.BASE_TYPE_UINT8Z:
                    return Type.UINT8Z;
                case Fit.BASE_TYPE_UINT16Z:
                    return Type.UINT16Z;
                case Fit.BASE_TYPE_UINT32Z:
                    return Type.UINT32Z;

                case Fit.BASE_TYPE_UINT64:
                    return Type.UINT64;
                case Fit.BASE_TYPE_SINT64:
                    return Type.SINT64;
                case Fit.BASE_TYPE_UINT64Z:
                    return Type.UINT64Z;

                case Fit.BASE_TYPE_ENUM:
                default:
                    return Type.ENUM;
            }
        }
    }
}
