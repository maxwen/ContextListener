package com.maxwen.contextlistener.network;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkService {
    private static final String TAG = "NetworkService";
    private static final boolean DEBUG = true;
    public static final String ACTION_BROADCAST = "com.maxwen.contextlistener.network.NETWORK_UPDATE";

    public static void updateNetworkInfo(Context context) {
        if (isNetworkAvailable(context) && isNewNetwork(context, getNetworkType(context), getWifiName(context))) {
            if (DEBUG) Log.d(TAG, "Update network");

            Database database = new Database(context);
            database.addNetworkEvent(System.currentTimeMillis(), getNetworkType(context), getWifiName(context));

            Intent updateIntent = new Intent(ACTION_BROADCAST);
            context.sendBroadcast(updateIntent);
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        }
        return false;
    }

    private static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                return "wifi";
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "mobile";
            } else {
                return "unknown";
            }
        }
        return "offline";
    }

    private static String getWifiName(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }

    private static boolean isNewNetwork(Context context, String networkType, String apName) {
        try {
            JSONObject networkData = new Database(context).getLastNetworkEvent();
            if (networkData != null) {
                String type = networkData.getString(Database.KEY_NETWORK_TYPE);
                if (!networkType.equals(type)) {
                    return true;
                }
                if (apName != null) {
                    String ap = networkData.getString(Database.KEY_AP_NAME);
                    if (!apName.equals(ap)) {
                        return true;
                    }
                }
            } else {
                // first one
                return true;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "no new network");
        return false;
    }
}
