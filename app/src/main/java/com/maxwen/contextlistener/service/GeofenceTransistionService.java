package com.maxwen.contextlistener.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.maxwen.contextlistener.db.Database;
import com.maxwen.contextlistener.location.GeofenceService;

import java.util.List;

/**
 * Created by maxl on 10/15/17.
 */

public class GeofenceTransistionService extends IntentService {

    private static final String TAG = "GeofenceTransitionService";

    public GeofenceTransistionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence fence : triggeringGeofences) {
                if (GeofenceService.isFilteredFence(this, fence.getRequestId())) {
                    new Database(this).addGeofenceEvent(System.currentTimeMillis(), fence.getRequestId(),
                            geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ?
                                    Database.KEY_GEOFENCE_ACTION_ENTER : Database.KEY_GEOFENCE_ACTION_LEAVE);
                }
            }
        }
    }
}
