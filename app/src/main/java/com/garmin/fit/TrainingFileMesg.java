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


public class TrainingFileMesg extends Mesg {

   
   public static final int TimestampFieldNum = 253;
   
   public static final int TypeFieldNum = 0;
   
   public static final int ManufacturerFieldNum = 1;
   
   public static final int ProductFieldNum = 2;
   
   public static final int SerialNumberFieldNum = 3;
   
   public static final int TimeCreatedFieldNum = 4;
   

   protected static final  Mesg trainingFileMesg;
   static {
      int field_index = 0;
      int subfield_index = 0;
      // training_file
      trainingFileMesg = new Mesg("training_file", MesgNum.TRAINING_FILE);
      trainingFileMesg.addField(new Field("timestamp", TimestampFieldNum, 134, 1, 0, "", false, Profile.Type.DATE_TIME));
      field_index++;
      trainingFileMesg.addField(new Field("type", TypeFieldNum, 0, 1, 0, "", false, Profile.Type.FILE));
      field_index++;
      trainingFileMesg.addField(new Field("manufacturer", ManufacturerFieldNum, 132, 1, 0, "", false, Profile.Type.MANUFACTURER));
      field_index++;
      trainingFileMesg.addField(new Field("product", ProductFieldNum, 132, 1, 0, "", false, Profile.Type.UINT16));
      subfield_index = 0;
      trainingFileMesg.fields.get(field_index).subFields.add(new SubField("garmin_product", 132, 1, 0, ""));
      trainingFileMesg.fields.get(field_index).subFields.get(subfield_index).addMap(1, 1);
      trainingFileMesg.fields.get(field_index).subFields.get(subfield_index).addMap(1, 15);
      trainingFileMesg.fields.get(field_index).subFields.get(subfield_index).addMap(1, 13);
      subfield_index++;
      field_index++;
      trainingFileMesg.addField(new Field("serial_number", SerialNumberFieldNum, 140, 1, 0, "", false, Profile.Type.UINT32Z));
      field_index++;
      trainingFileMesg.addField(new Field("time_created", TimeCreatedFieldNum, 134, 1, 0, "", false, Profile.Type.DATE_TIME));
      field_index++;
   }

   public TrainingFileMesg() {
      super(Factory.createMesg(MesgNum.TRAINING_FILE));
   }

   public TrainingFileMesg(final Mesg mesg) {
      super(mesg);
   }


   /**
    * Get timestamp field
    *
    * @return timestamp
    */
   public DateTime getTimestamp() {
      return timestampToDateTime(getFieldLongValue(253, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD));
   }

   /**
    * Set timestamp field
    *
    * @param timestamp
    */
   public void setTimestamp(DateTime timestamp) {
      setFieldValue(253, 0, timestamp.getTimestamp(), Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get type field
    *
    * @return type
    */
   public File getType() {
      Short value = getFieldShortValue(0, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
      if (value == null)
         return null;
      return File.getByValue(value);
   }

   /**
    * Set type field
    *
    * @param type
    */
   public void setType(File type) {
      setFieldValue(0, 0, type.value, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get manufacturer field
    *
    * @return manufacturer
    */
   public Integer getManufacturer() {
      return getFieldIntegerValue(1, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Set manufacturer field
    *
    * @param manufacturer
    */
   public void setManufacturer(Integer manufacturer) {
      setFieldValue(1, 0, manufacturer, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get product field
    *
    * @return product
    */
   public Integer getProduct() {
      return getFieldIntegerValue(2, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Set product field
    *
    * @param product
    */
   public void setProduct(Integer product) {
      setFieldValue(2, 0, product, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get garmin_product field
    *
    * @return garmin_product
    */
   public Integer getGarminProduct() {
      return getFieldIntegerValue(2, 0, Profile.SubFields.TRAINING_FILE_MESG_PRODUCT_FIELD_GARMIN_PRODUCT);
   }

   /**
    * Set garmin_product field
    *
    * @param garminProduct
    */
   public void setGarminProduct(Integer garminProduct) {
      setFieldValue(2, 0, garminProduct, Profile.SubFields.TRAINING_FILE_MESG_PRODUCT_FIELD_GARMIN_PRODUCT);
   }

   /**
    * Get serial_number field
    *
    * @return serial_number
    */
   public Long getSerialNumber() {
      return getFieldLongValue(3, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Set serial_number field
    *
    * @param serialNumber
    */
   public void setSerialNumber(Long serialNumber) {
      setFieldValue(3, 0, serialNumber, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get time_created field
    *
    * @return time_created
    */
   public DateTime getTimeCreated() {
      return timestampToDateTime(getFieldLongValue(4, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD));
   }

   /**
    * Set time_created field
    *
    * @param timeCreated
    */
   public void setTimeCreated(DateTime timeCreated) {
      setFieldValue(4, 0, timeCreated.getTimestamp(), Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

}
