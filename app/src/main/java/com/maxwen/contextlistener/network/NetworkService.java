package com.maxwen.contextlistener.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class NetworkService {
    private static final String TAG = "NetworkService";
    private static final boolean DEBUG = true;
    private static final String SHARED_PREFERENCES_NAME = "network";
    private static final String KEY_FILTERED_APS = "access_points";

    public static void updateNetworkInfo(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            return;
        }
        if (isFilteredNetwork(context, info)) {
            if (!info.isConnected()) {
                handleNetworkDisconnect(context, info);
            }

            if (isNewNetwork(context, getNetworkType(context, info), getWifiName(context))) {
                handleNetworkDisconnect(context, info);
                Database database = new Database(context);
                database.addNetworkEvent(System.currentTimeMillis(), getNetworkType(context, info),
                        isWifiNetwork(context, info) ? getWifiName(context) : null,
                        info.isConnected() ? Database.KEY_NETWORK_ACTION_CONNECT : Database.KEY_NETWORK_ACTION_DISCONNECT);
            }
        }
    }

    public static void handleNetworkDisconnect(Context context,NetworkInfo info) {
        try {
            Database database = new Database(context);
            JSONObject networkData = database.getLastNetworkEvent();
            if (networkData != null) {
                String networkType = networkData.getString(Database.KEY_NETWORK_TYPE);
                String apName = null;
                if (networkType.equals("wifi")) {
                    apName = networkData.getString(Database.KEY_AP_NAME);
                }
                boolean connected = networkData.getString(Database.KEY_NETWORK_ACTION).equals(Database.KEY_NETWORK_ACTION_CONNECT);
                if (connected) {
                    database.addNetworkEvent(System.currentTimeMillis(), networkType, apName, Database.KEY_NETWORK_ACTION_DISCONNECT);
                }
            }
        } catch(JSONException e) {
        }
    }

    private static String getNetworkType(Context context, NetworkInfo info) {
        if (isWifiNetwork(context, info)) {
            return "wifi";
        }
        if (isWMobileNetwork(context, info)) {
            return "mobile";
        }
        if (info != null && info.isConnected()) {
            return "unknown";
        }
        return "offline";
    }

    private static boolean isWifiNetwork(Context context, NetworkInfo info) {
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_WIFI ||
                    info.getType() == ConnectivityManager.TYPE_WIMAX) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWMobileNetwork(Context context, NetworkInfo info) {
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE ||
                    info.getType() == ConnectivityManager.TYPE_MOBILE_DUN) {
                return true;
            }
        }
        return false;
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

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static boolean isFilteredNetwork(Context context, NetworkInfo info) {
        if (isWifiNetwork(context, info)) {
            String apName = getWifiName(context);
            return getPrefs(context).getStringSet(KEY_FILTERED_APS, new HashSet<String>()).contains(apName);
        }
        return true;
    }

    public static Set<String> getFilteredNetworks(Context context) {
        return getPrefs(context).getStringSet(KEY_FILTERED_APS, new HashSet<String>());
    }

    public static void setFilteredNetworks(Context context, Set<String> networks) {
        getPrefs(context).edit().putStringSet(KEY_FILTERED_APS, networks).commit();
    }
}
