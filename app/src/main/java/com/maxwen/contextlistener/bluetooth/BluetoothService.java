package com.maxwen.contextlistener.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONObject;

public class BluetoothService {
    private static final String TAG = "PowerService";
    private static final boolean DEBUG = true;

    public static void handleBTConnect(Context context, Intent intent, boolean connected) {
        String deviceName = getBTDeviceName(context, intent);
        if (isNewBTDevice(context, deviceName, connected)) {
            Database database = new Database(context);
            database.addBTEvent(System.currentTimeMillis(), connected ? deviceName : "none");
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

    private static boolean isNewBTDevice(Context context, String deviceName, boolean connected) {
        try {
            Log.d(TAG, "isNewBTDevice " + deviceName);

            JSONObject btDeviceData = new Database(context).getLastBTEvent();
            if (btDeviceData != null) {
                String device = btDeviceData.getString(Database.KEY_BT_DEVICE_NAME);
                String newDeviceName = connected ? deviceName : "none";
                if (!newDeviceName.equals(device)) {
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
}
