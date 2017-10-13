package com.maxwen.contextlistener.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class CustomLocationListener implements LocationListener {
    private static final String TAG = "CustomLocationListener";
    private static final boolean DEBUG = false;
    private Context mContext;

    public CustomLocationListener(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "The location has changed, schedule an update ");
        LocationService.locationChanged(mContext, location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
