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


public enum WktStepTarget {
   SPEED((short)0),
   HEART_RATE((short)1),
   OPEN((short)2),
   CADENCE((short)3),
   POWER((short)4),
   GRADE((short)5),
   RESISTANCE((short)6),
   POWER_3S((short)7),
   POWER_10S((short)8),
   POWER_30S((short)9),
   POWER_LAP((short)10),
   SWIM_STROKE((short)11),
   SPEED_LAP((short)12),
   HEART_RATE_LAP((short)13),
    INVALID((short)255);

    protected final short value;

    WktStepTarget(short value) {
        this.value = value;
    }

   public static WktStepTarget getByValue(final Short value) {
      for (final WktStepTarget type : WktStepTarget.values()) {
         if (value == type.value)
            return type;
      }

      return WktStepTarget.INVALID;
   }

    /**
     * Retrieves the String Representation of the Value
     * @return The string representation of the value
     */
   public static String getStringFromValue( WktStepTarget value ) {
       return value.name();
   }

   public short getValue() {
      return value;
   }


}