package com.maxwen.contextlistener.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class BluetoothService {
    private static final String TAG = "PowerService";
    private static final boolean DEBUG = true;
    private static final String SHARED_PREFERENCES_NAME = "bluetooth";
    private static final String KEY_FILTERED_DEVICES = "devices";

    public static void handleBTConnect(Context context, Intent intent, boolean connected) {
        String deviceName = getBTDeviceName(context, intent);
        String action = connected ? Database.KEY_BT_DEVICE_ACTION_CONNECT : Database.KEY_BT_DEVICE_ACTION_DISCONNECT;
        if (isFilteredBTDevice(context, deviceName) && isNewBTDevice(context, deviceName, action)) {
            Database database = new Database(context);
            database.addBTEvent(System.currentTimeMillis(), deviceName, action);
        }
    }

    public static void handleBTDisable(Context context, Intent intent) {
        try {
            Database database = new Database(context);
            JSONObject btDeviceData = database.getLastBTEvent();
            if (btDeviceData != null) {
                String device = btDeviceData.getString(Database.KEY_BT_DEVICE_NAME);
                boolean connected = btDeviceData.getString(Database.KEY_BT_DEVICE_ACTION).equals(Database.KEY_BT_DEVICE_ACTION_CONNECT);
                if (connected) {
                    database.addBTEvent(System.currentTimeMillis(), device, Database.KEY_BT_DEVICE_ACTION_DISCONNECT);
                }
            }
        } catch(JSONException e) {
        }
    }

    private static String getBTDeviceName(Context context, Intent intent) {
        BluetoothDevice device = intent
                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device != null) {
            return device.getName();
        }
        return "unknown";
    }

    private static boolean isNewBTDevice(Context context, String deviceName, String newAction) {
        try {
            Log.d(TAG, "isNewBTDevice " + deviceName);

            JSONObject btDeviceData = new Database(context).getLastBTEvent();
            if (btDeviceData != null) {
                String device = btDeviceData.getString(Database.KEY_BT_DEVICE_NAME);
                String action = btDeviceData.getString(Database.KEY_BT_DEVICE_ACTION);
                if (!deviceName.equals(device) || !newAction.equals(action)) {
                    return true;
                }
            } else {
                // first one
                return true;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "no new device");
        return false;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static boolean isFilteredBTDevice(Context context, String deviceName) {
        return getPrefs(context).getStringSet(KEY_FILTERED_DEVICES, new HashSet<String>()).contains(deviceName);
    }

    public static Set<String> getFilteredBTDevices(Context context) {
        return getPrefs(context).getStringSet(KEY_FILTERED_DEVICES, new HashSet<String>());
    }

    public static void setFilteredBTDevices(Context context, Set<String> devices) {
        getPrefs(context).edit().putStringSet(KEY_FILTERED_DEVICES, devices).commit();
    }
}
