package com.maxwen.contextlistener.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.maxwen.contextlistener.bluetooth.BluetoothService;
import com.maxwen.contextlistener.db.Database;
import com.maxwen.contextlistener.location.GeofenceService;
import com.maxwen.contextlistener.network.NetworkService;

import java.util.Set;

/**
 * Created by maxl on 10/14/17.
 */

public class EventProvider extends ContentProvider {
    private static final String TAG = "EventProvider";
    private static final boolean DEBUG = false;
    public static final String AUTHORITY = "com.maxwen.contextlistener";
    private static final int URI_TYPE_EVENTS = 1;
    private static final int URI_TYPE_EVENTS_ALL = 2;
    private static final int URI_TYPE_EVENTS_GEOFENCE = 3;
    private static final int URI_TYPE_EVENTS_NETWORK = 4;
    private static final int URI_TYPE_EVENTS_BLUETOOTH = 5;
    private static final int URI_TYPE_FILTER_BLUETOOTH = 6;
    private static final int URI_TYPE_FILTER_NETWORK = 7;
    private static final int URI_TYPE_FILTER_GEOFENCE = 8;

    private static final UriMatcher sUriMatcher;

    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE +
                    "/vnd.de.openminds.lentitems_items";

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "events/all", URI_TYPE_EVENTS_ALL);
        sUriMatcher.addURI(AUTHORITY, "events/geofence", URI_TYPE_EVENTS_GEOFENCE);
        sUriMatcher.addURI(AUTHORITY, "events/network", URI_TYPE_EVENTS_NETWORK);
        sUriMatcher.addURI(AUTHORITY, "events/bluetooth", URI_TYPE_EVENTS_BLUETOOTH);
        sUriMatcher.addURI(AUTHORITY, "filter/network", URI_TYPE_FILTER_NETWORK);
        sUriMatcher.addURI(AUTHORITY, "filter/bluetooth", URI_TYPE_FILTER_BLUETOOTH);
        sUriMatcher.addURI(AUTHORITY, "filter/geofence", URI_TYPE_FILTER_GEOFENCE);
    }


    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        final int projectionType = sUriMatcher.match(uri);
        Log.d(TAG, "query " + uri + " " + projectionType);

        if (projectionType == URI_TYPE_EVENTS_ALL) {
            return new Database(getContext()).query(projection, selection, selectionArgs, sortOrder);
        } else if (projectionType == URI_TYPE_EVENTS_GEOFENCE) {
            return new Database(getContext()).query(projection, Database.KEY_TYPE + " =? ", new String[]{String.valueOf(Database.KEY_TYPE_GEFOENCE)}, sortOrder);
        } else if (projectionType == URI_TYPE_EVENTS_NETWORK) {
            return new Database(getContext()).query(projection, Database.KEY_TYPE + " =? ", new String[]{String.valueOf(Database.KEY_TYPE_NETWORK)}, sortOrder);
        } else if (projectionType == URI_TYPE_EVENTS_BLUETOOTH) {
            return new Database(getContext()).query(projection, Database.KEY_TYPE + " =? ", new String[]{String.valueOf(Database.KEY_TYPE_BT)}, sortOrder);
        } else if (projectionType == URI_TYPE_FILTER_NETWORK) {
            final Set<String> filteredNetworks = NetworkService.getFilteredNetworks(getContext());
            MatrixCursor cursor = new MatrixCursor(new String[] { "network"});
            for (String device : filteredNetworks) {
                MatrixCursor.RowBuilder builder = cursor.newRow();
                builder.add("network", device);
            }
            cursor.setNotificationUri(getContext().getContentResolver(),uri);
            return cursor;
        } else if (projectionType == URI_TYPE_FILTER_BLUETOOTH) {
            final Set<String> filteredDevices = BluetoothService.getFilteredBTDevices(getContext());
            MatrixCursor cursor = new MatrixCursor(new String[] { "device"});
            for (String device : filteredDevices) {
                MatrixCursor.RowBuilder builder = cursor.newRow();
                builder.add("device", device);
            }
            cursor.setNotificationUri(getContext().getContentResolver(),uri);
            return cursor;
        } else if (projectionType == URI_TYPE_FILTER_GEOFENCE) {
            final Set<String> filteredFences = GeofenceService.getFilteredFences(getContext());
            MatrixCursor cursor = new MatrixCursor(new String[] { "fence"});
            for (String fence : filteredFences) {
                MatrixCursor.RowBuilder builder = cursor.newRow();
                builder.add("fence", fence);
            }
            cursor.setNotificationUri(getContext().getContentResolver(),uri);
            return cursor;
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return CONTENT_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
