package com.maxwen.contextlistener;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {
    public static final String PREF_KEY_ENABLE = "enable";
    public static final String PREF_KEY_UPDATE_INTERVAL = "update_interval";

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.getBoolean(PREF_KEY_ENABLE, false);
    }

    public static boolean setEnabled(Context context, boolean value) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        return prefs.edit().putBoolean(PREF_KEY_ENABLE, value).commit();
    }

    public static int getUpdateInterval(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        String valueString = prefs.getString(PREF_KEY_UPDATE_INTERVAL, "1");
        return Integer.valueOf(valueString);
    }
}
