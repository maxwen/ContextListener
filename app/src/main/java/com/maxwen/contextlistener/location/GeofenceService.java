package com.maxwen.contextlistener.location;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.maxwen.contextlistener.db.Database;
import com.maxwen.contextlistener.service.GeofenceTransistionService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by maxl on 10/15/17.
 */

public class GeofenceService {
    private static final String TAG = "GeofenceService";
    private GeofencingClient mGeofencingClient;
    private List<Geofence> mGeofenceList;
    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final int GEOFENCE_DEFAULT_RADIUS = 200;

    public static final String GEOFENCE_HOME = "{\"name\":\"home\",\"lat\":47.8201718,\"long\":13.016651}";
    private static final String SHARED_PREFERENCES_NAME = "geofences.xml";
    private static final String KEY_GEOFENCE_LIST = "fences";
    private static final String KEY_GEOFENCE_FILTER_LIST = "filtered_fences";

    /**
     * For this sample, geofences expire after twelve hours.
     */
    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    private Context mContext;

    public GeofenceService(Context context) {
        mContext = context;
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        mGeofenceList = new ArrayList<>();

        Set<String> fences = getPrefs(mContext).getStringSet(KEY_GEOFENCE_LIST, null);
        if (fences == null) {
            fences = new HashSet<>();
            fences.add(GEOFENCE_HOME);
            getPrefs(mContext).edit().putStringSet(KEY_GEOFENCE_LIST, fences).commit();
        }
        createFences(fences);
    }

    public void initHomeFence() {
        Set<String> fences = new HashSet<>();
        fences.add(GEOFENCE_HOME);
        getPrefs(mContext).edit().putStringSet(KEY_GEOFENCE_LIST, fences).commit();
        createFences(fences);
    }

    public void addGeofence(String name, double lat, double longitude) throws SecurityException {
        Set<String> fences = new HashSet<>();
        fences.add(GEOFENCE_HOME);
        try {
            JSONObject fence = new JSONObject();
            fence.put(Database.KEY_GEOFENCE_NAME, name);
            fence.put(Database.KEY_LOCATION_LAT, lat);
            fence.put(Database.KEY_LOCATION_LONG, longitude);
            fences.add(fence.toString());
        } catch (JSONException e) {
        }
        getPrefs(mContext).edit().putStringSet(KEY_GEOFENCE_LIST, fences).commit();
        createFences(fences);
    }

    public void createFences(Set<String> fences) {
        mGeofenceList.clear();
        for (String fence : fences) {
            try {
                JSONObject jsonFence = new JSONObject(fence);
                Log.d(TAG, "create fence" + fence);
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(jsonFence.getString(Database.KEY_GEOFENCE_NAME))
                        .setCircularRegion(jsonFence.getDouble(Database.KEY_LOCATION_LAT), jsonFence.getDouble(Database.KEY_LOCATION_LONG), GEOFENCE_DEFAULT_RADIUS)
                        .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
                new Database(mContext).addGeofenceEvent(System.currentTimeMillis(), jsonFence.getString(Database.KEY_GEOFENCE_NAME), Database.KEY_GEOFENCE_ACTION_CREATE);
            } catch (Exception e) {
            }
        }
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent());
        } catch (SecurityException e){
        }
    }


    public void removeGeofence() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(mContext, GeofenceTransistionService.class);
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    public void setGeofenceToHere() {
        try {
            removeGeofence();
            JSONObject locationData = new Database(mContext).getLastLocationEvent();
            if (locationData != null) {
                Double lat = Double.valueOf(locationData.getString(Database.KEY_LOCATION_LAT));
                Double longitude = Double.valueOf(locationData.getString(Database.KEY_LOCATION_LONG));
                Log.d(TAG, "addGeofence " + lat + ":" + longitude);
                addGeofence("here", lat, longitude);
            }
        } catch (JSONException e) {
        }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isFilteredFence(Context context, String fence) {
        return getPrefs(context).getStringSet(KEY_GEOFENCE_FILTER_LIST, new HashSet<String>()).contains(fence);
    }

    public static Set<String> getFilteredFences(Context context) {
        return getPrefs(context).getStringSet(KEY_GEOFENCE_FILTER_LIST, new HashSet<String>());
    }

    public static void setFilteredFences(Context context, Set<String> fences) {
        getPrefs(context).edit().putStringSet(KEY_GEOFENCE_FILTER_LIST, fences).commit();
    }

    public static Set<String> getFenceList(Context context) {
        return getPrefs(context).getStringSet(KEY_GEOFENCE_LIST, new HashSet<String>());
    }
}
