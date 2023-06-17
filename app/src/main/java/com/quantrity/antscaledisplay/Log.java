package com.quantrity.antscaledisplay;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    public static void v(String tag, String msg) {
        android.util.Log.v(tag, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + msg);
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(tag, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + msg);
    }

    public static void i(String tag, String msg) {
        android.util.Log.i(tag, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + msg);
    }

    public static void d(String tag, String msg) {
        android.util.Log.d(tag, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + msg);
    }

    public static void w(String tag, String msg) {
        android.util.Log.w(tag, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + " " + msg);
    }
}
