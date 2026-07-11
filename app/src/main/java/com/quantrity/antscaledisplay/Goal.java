package com.quantrity.antscaledisplay;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

public class Goal {
    String uuid;
    long start_date = -1;
    long end_date = -1;
    double start_value = -1;
    double end_value = -1;
    Metric type = Metric.UNDEFINED;
    int color = 0;
    boolean show_fat_mass = false;

    public Goal() {}

    public Goal(JSONObject obj) throws JSONException {
        deserializeFromObj(obj);
    }

    private void deserializeFromObj(JSONObject obj) throws JSONException {
        this.uuid = obj.getString("uuid");
        this.start_date = obj.getLong("start_date");
        this.end_date = obj.getLong("end_date");
        this.start_value = obj.getDouble("start_value");
        this.end_value = obj.getDouble("end_value");
        String type = obj.getString("type");
        this.type = Metric.valueOf(type);
        this.color = obj.getInt("color");
        this.show_fat_mass = obj.getBoolean("show_fat_mass");
    }

    JSONObject serializeToObj() throws JSONException {
        JSONObject serializedObj = new JSONObject();

        serializedObj.put("uuid", this.uuid);
        serializedObj.put("start_date", this.start_date);
        serializedObj.put("end_date", this.end_date);
        serializedObj.put("start_value", this.start_value);
        serializedObj.put("end_value", this.end_value);
        serializedObj.put("type", this.type.toString());
        serializedObj.put("color", this.color);
        serializedObj.put("show_fat_mass", this.show_fat_mass);

        return serializedObj;
    }

    static class EndDateComparator implements Comparator<Goal> {
        @Override
        public int compare(Goal o1, Goal o2) {
            return Long.compare(o2.end_date, o1.end_date);
        }
    }
}
