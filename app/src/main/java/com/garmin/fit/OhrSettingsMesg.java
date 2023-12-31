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


public class OhrSettingsMesg extends Mesg {

   
   public static final int EnabledFieldNum = 0;
   

   protected static final  Mesg ohrSettingsMesg;
   static {
      // ohr_settings
      ohrSettingsMesg = new Mesg("ohr_settings", MesgNum.OHR_SETTINGS);
      ohrSettingsMesg.addField(new Field("enabled", EnabledFieldNum, 0, 1, 0, "", false, Profile.Type.SWITCH));
      
   }

   public OhrSettingsMesg() {
      super(Factory.createMesg(MesgNum.OHR_SETTINGS));
   }

   public OhrSettingsMesg(final Mesg mesg) {
      super(mesg);
   }


   /**
    * Get enabled field
    *
    * @return enabled
    */
   public Switch getEnabled() {
      Short value = getFieldShortValue(0, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
      if (value == null)
         return null;
      return Switch.getByValue(value);
   }

   /**
    * Set enabled field
    *
    * @param enabled
    */
   public void setEnabled(Switch enabled) {
      setFieldValue(0, 0, enabled.value, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

}
