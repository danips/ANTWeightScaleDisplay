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


public enum SubSport {
   GENERIC((short)0),
   TREADMILL((short)1),
   STREET((short)2),
   TRAIL((short)3),
   TRACK((short)4),
   SPIN((short)5),
   INDOOR_CYCLING((short)6),
   ROAD((short)7),
   MOUNTAIN((short)8),
   DOWNHILL((short)9),
   RECUMBENT((short)10),
   CYCLOCROSS((short)11),
   HAND_CYCLING((short)12),
   TRACK_CYCLING((short)13),
   INDOOR_ROWING((short)14),
   ELLIPTICAL((short)15),
   STAIR_CLIMBING((short)16),
   LAP_SWIMMING((short)17),
   OPEN_WATER((short)18),
   FLEXIBILITY_TRAINING((short)19),
   STRENGTH_TRAINING((short)20),
   WARM_UP((short)21),
   MATCH((short)22),
   EXERCISE((short)23),
   CHALLENGE((short)24),
   INDOOR_SKIING((short)25),
   CARDIO_TRAINING((short)26),
   INDOOR_WALKING((short)27),
   E_BIKE_FITNESS((short)28),
   BMX((short)29),
   CASUAL_WALKING((short)30),
   SPEED_WALKING((short)31),
   BIKE_TO_RUN_TRANSITION((short)32),
   RUN_TO_BIKE_TRANSITION((short)33),
   SWIM_TO_BIKE_TRANSITION((short)34),
   ATV((short)35),
   MOTOCROSS((short)36),
   BACKCOUNTRY((short)37),
   RESORT((short)38),
   RC_DRONE((short)39),
   WINGSUIT((short)40),
   WHITEWATER((short)41),
   SKATE_SKIING((short)42),
   YOGA((short)43),
   PILATES((short)44),
   INDOOR_RUNNING((short)45),
   GRAVEL_CYCLING((short)46),
   E_BIKE_MOUNTAIN((short)47),
   COMMUTING((short)48),
   MIXED_SURFACE((short)49),
   NAVIGATE((short)50),
   TRACK_ME((short)51),
   MAP((short)52),
   ALL((short)254),
    INVALID((short)255);

    protected final short value;

    SubSport(short value) {
        this.value = value;
    }

   public static SubSport getByValue(final Short value) {
      for (final SubSport type : SubSport.values()) {
         if (value == type.value)
            return type;
      }

      return SubSport.INVALID;
   }

    /**
     * Retrieves the String Representation of the Value
     * @return The string representation of the value
     */
   public static String getStringFromValue( SubSport value ) {
       return value.name();
   }

   public short getValue() {
      return value;
   }


}
