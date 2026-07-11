package com.quantrity.antscaledisplay;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.Locale;

public class Weight implements Cloneable {
    long date = -1;
    String uuid = null;
    int age = -1;
    boolean isMale = true;

    double height = -1;
    double weight = -1;
    double trunkPercentFat = -1;
    double trunkMuscleMass = -1;
    double leftArmPercentFat = -1;
    double leftArmMuscleMass = -1;
    double rightArmPercentFat = -1;
    double rightArmMuscleMass = -1;
    double leftLegPercentFat = -1;
    double leftLegMuscleMass = -1;
    double rightLegPercentFat = -1;
    double rightLegMuscleMass = -1;
    double percentFat = -1;
    double percentHydration = -1;
    double boneMass = -1;
    double muscleMass = -1;
    int physiqueRating = -1;
    double visceralFatRating = -1;
    int metabolicAge = -1;
    double activeMet = -1;
    double basalMet = -1;
    int activityLevel = -1;

    public Weight() {}

    public Weight(JSONObject obj) throws JSONException {
        deserializeFromObj(obj);
    }

    private void deserializeFromObj(JSONObject obj) throws JSONException {
        this.date = obj.getLong("date");
        this.uuid = obj.getString("uuid");
        this.age = obj.getInt("age");
        this.isMale = obj.getBoolean("isMale");

        this.height = obj.getInt("height");
        this.weight = obj.getDouble("weight");
        if (obj.has("trunkPercentFat")) this.trunkPercentFat = obj.getDouble("trunkPercentFat");
        if (obj.has("trunkMuscleMass")) this.trunkMuscleMass = obj.getDouble("trunkMuscleMass");
        if (obj.has("leftArmPercentFat")) this.leftArmPercentFat = obj.getDouble("leftArmPercentFat");
        if (obj.has("leftArmMuscleMass")) this.leftArmMuscleMass = obj.getDouble("leftArmMuscleMass");
        if (obj.has("rightArmPercentFat")) this.rightArmPercentFat = obj.getDouble("rightArmPercentFat");
        if (obj.has("rightArmMuscleMass")) this.rightArmMuscleMass = obj.getDouble("rightArmMuscleMass");
        if (obj.has("leftLegPercentFat")) this.leftLegPercentFat = obj.getDouble("leftLegPercentFat");
        if (obj.has("leftLegMuscleMass")) this.leftLegMuscleMass = obj.getDouble("leftLegMuscleMass");
        if (obj.has("rightLegPercentFat")) this.rightLegPercentFat = obj.getDouble("rightLegPercentFat");
        if (obj.has("rightLegMuscleMass")) this.rightLegMuscleMass = obj.getDouble("rightLegMuscleMass");
        if (obj.has("percentFat")) this.percentFat = obj.getDouble("percentFat");
        if (obj.has("percentHydration")) this.percentHydration = obj.getDouble("percentHydration");
        if (obj.has("boneMass")) this.boneMass = obj.getDouble("boneMass");
        if (obj.has("muscleMass")) this.muscleMass = obj.getDouble("muscleMass");
        if (obj.has("physiqueRating")) this.physiqueRating = obj.getInt("physiqueRating");
        if (obj.has("visceralFatRating")) this.visceralFatRating = obj.getDouble("visceralFatRating");
        if (obj.has("metabolicAge")) this.metabolicAge = obj.getInt("metabolicAge");
        if (obj.has("activeMet")) this.activeMet = obj.getDouble("activeMet");
        if (obj.has("basalMet")) this.basalMet = obj.getDouble("basalMet");
        if (obj.has("activityLevel")) this.activityLevel = obj.getInt("activityLevel");
        
        if ((basalMet == -1) && (activeMet != -1)) {
            basalMet = activeMet;
            activeMet = -1;
        }
    }

    JSONObject serializeToObj() throws JSONException {
        JSONObject serializedObj = new JSONObject();

        serializedObj.put("date", this.date);
        serializedObj.put("uuid", this.uuid);
        serializedObj.put("age", this.age);
        serializedObj.put("isMale", this.isMale);

        serializedObj.put("height", this.height);
        serializedObj.put("weight", this.weight);
        if (this.trunkPercentFat != -1) serializedObj.put("trunkPercentFat", this.trunkPercentFat);
        if (this.trunkMuscleMass != -1) serializedObj.put("trunkMuscleMass", this.trunkMuscleMass);
        if (this.leftArmPercentFat != -1) serializedObj.put("leftArmPercentFat", this.leftArmPercentFat);
        if (this.leftArmMuscleMass != -1) serializedObj.put("leftArmMuscleMass", this.leftArmMuscleMass);
        if (this.rightArmPercentFat != -1) serializedObj.put("rightArmPercentFat", this.rightArmPercentFat);
        if (this.rightArmMuscleMass != -1) serializedObj.put("rightArmMuscleMass", this.rightArmMuscleMass);
        if (this.leftLegPercentFat != -1) serializedObj.put("leftLegPercentFat", this.leftLegPercentFat);
        if (this.leftLegMuscleMass != -1) serializedObj.put("leftLegMuscleMass", this.leftLegMuscleMass);
        if (this.rightLegPercentFat != -1) serializedObj.put("rightLegPercentFat", this.rightLegPercentFat);
        if (this.rightLegMuscleMass != -1) serializedObj.put("rightLegMuscleMass", this.rightLegMuscleMass);
        if (this.percentFat != -1) serializedObj.put("percentFat", this.percentFat);
        if (this.percentHydration != -1) serializedObj.put("percentHydration", this.percentHydration);
        if (this.boneMass != -1) serializedObj.put("boneMass", this.boneMass);
        if (this.muscleMass != -1) serializedObj.put("muscleMass", this.muscleMass);
        if (this.physiqueRating != -1) serializedObj.put("physiqueRating", this.physiqueRating);
        if (this.visceralFatRating != -1) serializedObj.put("visceralFatRating", this.visceralFatRating);
        if (this.metabolicAge != -1) serializedObj.put("metabolicAge", this.metabolicAge);
        if (this.activeMet != -1) serializedObj.put("activeMet", String.format(Locale.getDefault(),"%.0f", this.activeMet));
        if (this.basalMet != -1) serializedObj.put("basalMet", String.format(Locale.getDefault(),"%.0f", this.basalMet));
        if (this.activityLevel != -1) serializedObj.put("activityLevel", this.activityLevel);

        return serializedObj;
    }

    static class DateComparator implements Comparator<Weight> {
        @Override
        public int compare(Weight o1, Weight o2) {
            return Long.compare(o2.date, o1.date);
        }
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Weight other) {
        return (other != null) && (this.date == other.date)
                && (this.uuid.equals(other.uuid))
                && (this.age == other.age)
                && (this.isMale == other.isMale)
                && (this.height == other.height)
                && (this.weight == other.weight)
                && (this.trunkPercentFat == other.trunkPercentFat)
                && (this.trunkMuscleMass == other.trunkMuscleMass)
                && (this.leftArmPercentFat == other.leftArmPercentFat)
                && (this.leftArmMuscleMass == other.leftArmMuscleMass)
                && (this.rightArmPercentFat == other.rightArmPercentFat)
                && (this.rightArmMuscleMass == other.rightArmMuscleMass)
                && (this.leftLegPercentFat == other.leftLegPercentFat)
                && (this.leftLegMuscleMass == other.leftLegMuscleMass)
                && (this.rightLegPercentFat == other.rightLegPercentFat)
                && (this.rightLegMuscleMass == other.rightLegMuscleMass)
                && (this.percentFat == other.percentFat)
                && (this.percentHydration == other.percentHydration)
                && (this.boneMass == other.boneMass)
                && (this.muscleMass == other.muscleMass)
                && (this.physiqueRating == other.physiqueRating)
                && (this.visceralFatRating == other.visceralFatRating)
                && (this.metabolicAge == other.metabolicAge)
                && (this.activeMet == other.activeMet)
                && (this.basalMet == other.basalMet)
                && (this.activityLevel == other.activityLevel);
    }

}
