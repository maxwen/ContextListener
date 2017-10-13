package com.maxwen.contextlistener.power;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONException;
import org.json.JSONObject;

public class PowerService {
    private static final String TAG = "PowerService";
    private static final boolean DEBUG = true;
    public static final String ACTION_BROADCAST = "com.maxwen.contextlistener.power.POWER_UPDATE";

    public static void handlePowerEvent(Context context, boolean charging) {
        Log.d(TAG, "handlePowerEvent " + charging);
        if (isChangedCharging(context, charging)) {
            Database database = new Database(context);
            database.addPowerEvent(System.currentTimeMillis(), charging);

            Intent updateIntent = new Intent(ACTION_BROADCAST);
            context.sendBroadcast(updateIntent);
        }
    }

    private static boolean isChangedCharging(Context context, boolean charging) {
        try {
            JSONObject powerData = new Database(context).getLastPowerEvent();
            if (powerData != null) {
                boolean c = powerData.getBoolean(Database.KEY_POWER_CHARGING);
                return c != charging;
            } else {
                // first one
                return true;
            }
        } catch (JSONException e) {
        }
        Log.d(TAG, "no new charging");
        return false;
    }
}
