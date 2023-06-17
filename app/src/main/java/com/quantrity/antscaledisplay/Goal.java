package com.quantrity.antscaledisplay;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class Goal {
    private final static String TAG = "Goal";

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

    public Goal(String serializedObj) throws JSONException {
        deserialize(serializedObj);
    }

    static String goalsFilePath(Context context) {
        return context.getFilesDir() + "/goals";
    }

    private void deserialize(String serializedObj) throws JSONException {
        JSONObject obj = new JSONObject(serializedObj);
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

    private JSONObject serializeToObj() throws JSONException {
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

    static String loadJSONFromFile(String path) {
        String json = null;
        try {
            File f = new File(path);
            FileInputStream fin = new FileInputStream(f);

            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            json = sb.toString();
        } catch (Exception e) {
            if (Debug.ON) e.printStackTrace();
        }
        return json;
    }

    private static void deserializeArray(String serializedArray, ArrayList<Goal> goalsArray) throws JSONException {
        JSONArray jsonObjs = new JSONArray(serializedArray);
        for (int i = 0; i < jsonObjs.length(); i++) {
            JSONObject goal = jsonObjs.getJSONObject(i);
            goalsArray.add(new Goal(goal));
        }
    }

    static void deserializeGoals(Context context, ArrayList<Goal> goalsArray) {
        String serializedArray = loadJSONFromFile(goalsFilePath(context));
        if (serializedArray == null) return;

        try {
            deserializeArray(serializedArray, goalsArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void serializeGoals(final Context context, final ArrayList<Goal> output) {
        new Thread(() -> {
            if (Debug.ON) Log.v("TAG", "writing " + output.size() + " goals to " + goalsFilePath(context) + " output=" + output);
            Collections.sort(output, new EndDateComparator());
            JSONArray jsonArray = new JSONArray();
            Iterator<Goal> tmp = output.iterator();
            try {
                while (tmp.hasNext()) jsonArray.put(tmp.next().serializeToObj());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            try {
                String finalname = goalsFilePath(context);
                String tmpname = finalname + ".tmp";
                FileWriter file = new FileWriter(tmpname);
                file.write(jsonArray.toString());
                file.flush();
                file.close();

                File filefinal = new File(finalname);
                File filedelete = new File(finalname + ".del");
                if ((filefinal.exists()) && !filefinal.renameTo(filedelete)) {
                    if (Debug.ON) Log.v(TAG, filefinal + " could not be renamed to " + filedelete);
                } else {
                    File filetmp = new File(tmpname);
                    File filefinal2 = new File(finalname);
                    if (!filetmp.renameTo(filefinal2)) {
                        if (Debug.ON) Log.v(TAG, filetmp + " could not be renamed to " + filefinal2);
                    } else {
                        if (!filedelete.delete()) {
                            if (Debug.ON) Log.v(TAG, filedelete + " could not be deleted");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    static class EndDateComparator implements Comparator<Goal> {
        @Override
        public int compare(Goal o1, Goal o2) {
            return Long.compare(o2.end_date, o1.end_date);
        }
    }
}
