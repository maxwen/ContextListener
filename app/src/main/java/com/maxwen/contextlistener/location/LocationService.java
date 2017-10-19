package com.maxwen.contextlistener.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

import org.json.JSONObject;

public class LocationService  {
    private static final String TAG = "LocationService";
    private static final boolean DEBUG = true;
    static final String ACTION_CANCEL_LOCATION_UPDATE =
            "com.maxwen.contextlistener.location.CANCEL_LOCATION_UPDATE";

    private static final float LOCATION_ACCURACY_THRESHOLD_METERS = 50000;
    private static final long OUTDATED_LOCATION_THRESHOLD_MILLIS = 10L * 60L * 1000L; // 10 minutes

    public static final float LOCATION_EQUALS_THRESHOLD_METERS = 100;
    public static final long LOCATION_REQUEST_MIN_TIME = 0;

    private static final Criteria sLocationCriteria;

    static {
        sLocationCriteria = new Criteria();
        sLocationCriteria.setPowerRequirement(Criteria.POWER_LOW);
        sLocationCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        sLocationCriteria.setCostAllowed(false);
    }

    public LocationService() {
    }

    private static Location getCurrentLocation(Context context, CustomLocationListener locationListener) throws SecurityException {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (DEBUG) Log.d(TAG, "Current location is " + location);

        if (location != null && location.getAccuracy() > LOCATION_ACCURACY_THRESHOLD_METERS) {
            Log.w(TAG, "Ignoring inaccurate location");
            location = null;
        }

        // If lastKnownLocation is not present (because none of the apps in the
        // device has requested the current location to the system yet) or outdated,
        // then try to get the current location use the provider that best matches the criteria.
        boolean needsUpdate = location == null;
        if (location != null) {
            long delta = System.currentTimeMillis() - location.getTime();
            needsUpdate = delta > OUTDATED_LOCATION_THRESHOLD_MILLIS;
        }
        if (needsUpdate) {
            if (DEBUG) Log.d(TAG, "Getting best location provider");
            String locationProvider = lm.getBestProvider(sLocationCriteria, true);
            if (TextUtils.isEmpty(locationProvider)) {
                Log.e(TAG, "No available location providers matching criteria.");
            } else {
                Log.d(TAG, "Request single location update");
                lm.requestSingleUpdate(locationProvider, locationListener, null);
            }
        } else {
            Log.d(TAG, "Reuse location " + location);
        }
        return location;
    }

    public static void locationChanged(Context context, Location location) {
        if (checkPermissions(context)) {
            if (location != null && isNewLocation(context, location)) {
                Database database = new Database(context);
                database.addLocationEvent(System.currentTimeMillis(), location);
            }
        } else {
            Log.w(TAG, "no location permissions");
            // we are outa here
        }
    }

    public static void updateLocation(Context context, CustomLocationListener locationListener) {
        if (checkPermissions(context)) {
            Location location = getCurrentLocation(context, locationListener);
            if (location != null && isNewLocation(context, location)) {
                Database database = new Database(context);
                database.addLocationEvent(System.currentTimeMillis(), location);
            }
        } else {
            Log.w(TAG, "no location permissions");
            // we are outa here
        }
    }

    public static boolean checkPermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isNewLocation(Context context, Location location) {
        try {
            Log.d(TAG, "isNewLocation");

            JSONObject locationData = new Database(context).getLastLocationEvent();
            if (locationData != null) {
                Double lat = Double.valueOf(locationData.getString(Database.KEY_LOCATION_LAT));
                Double longitude = Double.valueOf(locationData.getString(Database.KEY_LOCATION_LONG));
                float[] distance = new float[1];
                Location.distanceBetween(lat, longitude, location.getLatitude(), location.getLongitude(), distance);
                if (distance[0] > LOCATION_EQUALS_THRESHOLD_METERS) {
                    Log.d(TAG, "distance = " + distance[0]);
                    return true;
                }
            } else {
                // first one
                return true;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "no new location");
        return false;
    }
}
