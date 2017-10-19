package com.maxwen.contextlistener.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.maxwen.contextlistener.db.Database;

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
    private static final UriMatcher sUriMatcher;

    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE +
                    "/vnd.de.openminds.lentitems_items";

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "events/all", URI_TYPE_EVENTS_ALL);
        sUriMatcher.addURI(AUTHORITY, "events/geofence", URI_TYPE_EVENTS_GEOFENCE);
        sUriMatcher.addURI(AUTHORITY, "events/network", URI_TYPE_EVENTS_NETWORK);
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
