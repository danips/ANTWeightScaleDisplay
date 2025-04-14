package com.quantrity.antscaledisplay;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.Collator;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class User {
    private final static String TAG = "User";

    public enum MassUnit implements Serializable {
        KG, LB, ST;

        public String getStatus() {
            return this.name();
        }
    }

    String uuid;
    String name;
    boolean isMale;
    long birthdate;
    int age;
    int height_cm;
    int height_ft;
    int height_in;
    int activity_level;
    boolean isLifetimeAthlete;
    boolean usesCm;
    MassUnit mass_unit;
    boolean autoupload;
    boolean show_fat_mass;

    String gc_user;
    String gc_pass;
    String garminOauth1Token;
    String garminOauth1TokenSecret;
    String garminOauth1MfaToken;
    long garminOauth1MfaExpirationTimestamp;
    String garminOauth2Token;
    String garminOauth2RefreshToken;
    long garminOauth2ExpiryTimestamp;
    long garminOauth2RefreshExpiryTimestamp;
    String email_to;

    public User() {}

    public User(JSONObject obj) throws JSONException {
        deserializeFromObj(obj);
    }

    public User(String serializedObj) throws JSONException {
        deserialize(serializedObj);
    }

    static String usersFilePath(Context context) {
        return context.getFilesDir() + "/users";
    }

    static int calcAgeNow(long time_in_millis) {
        return calcAge(time_in_millis, Calendar.getInstance().getTimeInMillis());
    }

    static int calcAge(long birth, long date) {
        Calendar dob = Calendar.getInstance();
        dob.setTimeInMillis(birth);
        Calendar when = Calendar.getInstance();
        when.setTimeInMillis(date);
        int age = when.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (when.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
        return age;
    }

    private void deserialize(String serializedObj) throws JSONException {
        JSONObject obj = new JSONObject(serializedObj);
        deserializeFromObj(obj);
    }

    private void deserializeFromObj(JSONObject obj) throws JSONException {
        this.uuid = obj.getString("uuid");
        this.name = obj.getString("name");
        this.isMale = obj.getBoolean("isMale");
        if (obj.has("birthdate")) this.birthdate = obj.getLong("birthdate");
        else {
            Calendar birthdate = Calendar.getInstance();
            int year  = birthdate.get(Calendar.YEAR) - obj.getInt("age");
            birthdate.set(year, Calendar.JANUARY, 1, 0, 0, 0);
            birthdate.set(Calendar.MILLISECOND, 0);
            this.birthdate = birthdate.getTimeInMillis();
        }
        this.age = calcAgeNow(this.birthdate);

        this.usesCm = obj.getBoolean("usesCm");
        try {
            this.mass_unit = MassUnit.valueOf(obj.getString("mass_unit"));
        } catch (JSONException e)
        {
            this.mass_unit = obj.getBoolean("usesKg") ? MassUnit.KG : MassUnit.LB;
        }
        if (this.usesCm) {
            this.height_cm = obj.getInt("height_cm");
        } else {
            this.height_ft = obj.getInt("height_ft");
            this.height_in = obj.getInt("height_in");
            this.height_cm = (int)(this.height_ft * 30.48 + this.height_in * 2.54);
        }
        this.activity_level = obj.getInt("activity_level");
        this.isLifetimeAthlete = obj.getBoolean("isLifetimeAthlete");
        if (obj.has("gc_user")) this.gc_user = obj.getString("gc_user");
        if (obj.has("gc_pass")) this.gc_pass = obj.getString("gc_pass");
        if (obj.has("garminOauth1Token")) this.garminOauth1Token = obj.getString("garminOauth1Token");
        if (obj.has("garminOauth1TokenSecret")) this.garminOauth1TokenSecret = obj.getString("garminOauth1TokenSecret");
        if (obj.has("garminOauth1MfaToken")) this.garminOauth1MfaToken = obj.getString("garminOauth1MfaToken");
        if (obj.has("garminOauth1MfaExpirationTimestamp")) this.garminOauth1MfaExpirationTimestamp = obj.getLong("garminOauth1MfaExpirationTimestamp");
        if (obj.has("garminOauth2Token")) this.garminOauth2Token = obj.getString("garminOauth2Token");
        if (obj.has("garminOauth2RefreshToken")) this.garminOauth2RefreshToken = obj.getString("garminOauth2RefreshToken");
        if (obj.has("garminOauth2ExpiryTimestamp")) this.garminOauth2ExpiryTimestamp = obj.getLong("garminOauth2ExpiryTimestamp");
        if (obj.has("garminOauth2RefreshExpiryTimestamp")) this.garminOauth2RefreshExpiryTimestamp = obj.getLong("garminOauth2RefreshExpiryTimestamp");
        if (obj.has("email_to")) this.email_to = obj.getString("email_to");

        this.autoupload = !obj.has("autoupload") || obj.getBoolean("autoupload");
        this.show_fat_mass = (obj.has("show_fat_mass")) && obj.getBoolean("show_fat_mass");
    }

    private JSONObject serializeToObj() throws JSONException {
        JSONObject serializedObj = new JSONObject();

        serializedObj.put("uuid", this.uuid);
        serializedObj.put("name", this.name);
        serializedObj.put("isMale", this.isMale);
        serializedObj.put("birthdate", this.birthdate);
        if (this.usesCm) {
            serializedObj.put("height_cm", this.height_cm);
        } else {
            serializedObj.put("height_ft", this.height_ft);
            serializedObj.put("height_in", this.height_in);
        }
        serializedObj.put("activity_level", this.activity_level);
        serializedObj.put("isLifetimeAthlete", this.isLifetimeAthlete);
        serializedObj.put("usesCm", this.usesCm);
        serializedObj.put("mass_unit", this.mass_unit.getStatus());
        serializedObj.put("gc_user", this.gc_user);
        serializedObj.put("garminOauth1Token",this.garminOauth1Token);
        serializedObj.put("garminOauth1TokenSecret",this.garminOauth1TokenSecret);
        serializedObj.put("garminOauth1MfaToken",this.garminOauth1MfaToken);
        serializedObj.put("garminOauth1MfaExpirationTimestamp",this.garminOauth1MfaExpirationTimestamp);
        serializedObj.put("garminOauth2Token",this.garminOauth2Token);
        serializedObj.put("garminOauth2RefreshToken",this.garminOauth2RefreshToken);
        serializedObj.put("garminOauth2ExpiryTimestamp",this.garminOauth2ExpiryTimestamp);
        serializedObj.put("garminOauth2RefreshExpiryTimestamp",this.garminOauth2RefreshExpiryTimestamp);
        serializedObj.put("gc_pass", this.gc_pass);
        serializedObj.put("email_to", this.email_to);

        serializedObj.put("autoupload", this.autoupload);
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

    private static void deserializeArray(String serializedArray, ArrayList<User> usersArray) throws JSONException {
        JSONArray jsonObjs = new JSONArray(serializedArray);
        for (int i = 0; i < jsonObjs.length(); i++) {
            JSONObject user = jsonObjs.getJSONObject(i);
            usersArray.add(new User(user));
        }
    }

    static void deserializeUsers(Context context, ArrayList<User> usersArray) {
        String serializedArray = loadJSONFromFile(usersFilePath(context));
        if (serializedArray == null) return;

        try {
            deserializeArray(serializedArray, usersArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void serializeUsers(final Context context, final ArrayList<User> output) {
        new Thread(() -> {
            if (Debug.ON) Log.v("TAG", "writing " + output.size() + " users to " + usersFilePath(context) + " output=" + output);
            final Collator collator = Collator.getInstance();
            Collections.sort(output, (o1, o2) -> collator.compare(o1.name, o2.name));
            JSONArray jsonArray = new JSONArray();
            Iterator<User> tmp = output.iterator();
            try {
                while (tmp.hasNext()) jsonArray.put(tmp.next().serializeToObj());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            try {
                String finalname = usersFilePath(context);
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

    @NonNull
    public String toString() {
        return name;
    }

    public static String[] toString(List<User> l) {
        String[] users = new String[l.size()];
        Iterator<User> it = l.iterator();
        int i = 0;
        while (it.hasNext()) users[i++] = it.next().name;
        return users;
    }

    String printMass(Context c, double mass) {
        return printMass(c, mass, true, false);
    }

    String printMass(Context c, double mass, boolean show_units, boolean ignore_stones) {
        int format = R.string.edit_user_fragment_units_tag_kg;
        double value = mass;
        if ((this.mass_unit == User.MassUnit.LB) ||
                (ignore_stones && this.mass_unit == User.MassUnit.ST)) {
            format = R.string.edit_user_fragment_units_tag_lb;
            value = mass * 2.20462262;
        } else if (this.mass_unit == User.MassUnit.ST) {
            double lbs = mass * 2.20462262;
            if (!show_units)
            {
                return String.format(Locale.getDefault(),"%1$.2f", Math.floor(lbs/14));
            }

            double divisor = (float)Math.floor(lbs / 14);
            double remainder = lbs % 14;
            if (divisor == 0)
            {
                format = R.string.edit_user_fragment_units_tag_lb;
                value = mass * 2.20462262;
            }
            else
            {
                return String.format(c.getString(R.string.edit_user_fragment_units_tag_st), divisor, remainder);
            }
        }
        if (show_units)
        {
            return String.format(c.getString(format), value);
        }
        else
        {
            return String.format(Locale.getDefault(),"%1$.2f", value);
        }
    }

    double calc_mass2(double d, double weight, boolean percent)
    {
        if (percent)
        {
            if (this.show_fat_mass)
            {
                if (this.mass_unit == User.MassUnit.KG) {
                    return (d * weight / 100);
                }
                else
                {
                    return (d * (2.20462262 * weight) / 100);
                }
            }
            else {
                return d;
            }
        }
        else {
            if (this.mass_unit == User.MassUnit.KG) {
                return d;
            } else {
                return d / 2.20462262;
            }
        }
    }

    double calc_mass(double d, double weight, boolean percent)
    {
        if (percent)
        {
            if (this.show_fat_mass)
            {
                if (this.mass_unit == User.MassUnit.KG) {
                    return (d * 100 /  weight);
                }
                else
                {
                    return (d * 100 / (2.20462262 * weight));
                }
            }
            else {
                return d;
            }
        }
        else {
            if (this.mass_unit == User.MassUnit.KG) {
                return d;
            } else {
                return d / 2.20462262;
            }
        }
    }

    double calc_mass(EditText et, double weight, boolean percent)
    {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        Number number;
        try {
            number = format.parse(et.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
        return calc_mass(number.doubleValue(), weight, percent);
    }
}
