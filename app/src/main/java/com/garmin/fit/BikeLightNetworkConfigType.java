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


public enum BikeLightNetworkConfigType {
   AUTO((short)0),
   INDIVIDUAL((short)4),
   HIGH_VISIBILITY((short)5),
   TRAIL((short)6),
    INVALID((short)255);

    protected final short value;

    BikeLightNetworkConfigType(short value) {
        this.value = value;
    }

   public static BikeLightNetworkConfigType getByValue(final Short value) {
      for (final BikeLightNetworkConfigType type : BikeLightNetworkConfigType.values()) {
         if (value == type.value)
            return type;
      }

      return BikeLightNetworkConfigType.INVALID;
   }

    /**
     * Retrieves the String Representation of the Value
     * @return The string representation of the value
     */
   public static String getStringFromValue( BikeLightNetworkConfigType value ) {
       return value.name();
   }

   public short getValue() {
      return value;
   }


}
