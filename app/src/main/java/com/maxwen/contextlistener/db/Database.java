package com.maxwen.contextlistener.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.maxwen.contextlistener.provider.EventProvider;

import org.json.JSONException;
import org.json.JSONObject;

public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "event_log";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_EVENTS = "events";

    private static final String KEY_ID = "_id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_DATA = "data";
    public static final String KEY_TYPE = "type";

    public static final int KEY_TYPE_UNKNOWN = 0;
    public static final int KEY_TYPE_LOCATION = 1;
    public static final int KEY_TYPE_NETWORK = 2;
    public static final int KEY_TYPE_BT = 3;
    public static final int KEY_TYPE_NFC = 4;
    public static final int KEY_TYPE_POWER = 5;
    public static final int KEY_TYPE_GEFOENCE = 6;

    public static final String KEY_LOCATION_LAT = "lat";
    public static final String KEY_LOCATION_LONG = "long";
    public static final String KEY_NETWORK_TYPE = "network";
    public static final String KEY_AP_NAME = "ap";
    public static final String KEY_BT_DEVICE_NAME = "device";
    public static final String KEY_NFC_TAG_ID = "tag";
    public static final String KEY_POWER_CHARGING = "charging";
    public static final String KEY_GEOFENCE_NAME = "name";
    public static final String KEY_GEOFENCE_ACTION_TYPE = "action";
    public static final String KEY_GEOFENCE_ACTION_ENTER = "enter";
    public static final String KEY_GEOFENCE_ACTION_LEAVE = "leave";
    public static final String KEY_GEOFENCE_ACTION_CREATE = "create";

    private Context mContext;

    public Database(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_EVENTS = "CREATE TABLE " + TABLE_EVENTS + "(" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                KEY_TIMESTAMP + " INTEGER DEFAULT 0," +
                KEY_TYPE + " INTEGER DEFAULT 0," +
                KEY_DATA + " TEXT NOT NULL)";

        db.execSQL(CREATE_TABLE_EVENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);

        // Create tables again
        onCreate(db);
    }

    public void addLocationEvent(long timeStamp, Location location) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject locationData = new JSONObject();
            locationData.put(KEY_LOCATION_LAT, location.getLatitude());
            locationData.put(KEY_LOCATION_LONG, location.getLongitude());

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_LOCATION);
            values.put(KEY_DATA, locationData.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }

    public JSONObject getLastLocationEvent() throws JSONException {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = KEY_TIMESTAMP + " DESC";
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{KEY_ID, KEY_TIMESTAMP, KEY_TYPE, KEY_DATA}, KEY_TYPE + " =? ", new String[]{String.valueOf(KEY_TYPE_LOCATION)}, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                String data = cursor.getString(cursor.getColumnIndex(Database.KEY_DATA));
                JSONObject jData = new JSONObject(data);
                return jData;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void addNetworkEvent(long timeStamp, String type, String accessPoint) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject networkData = new JSONObject();
            networkData.put(KEY_NETWORK_TYPE, type);
            if (accessPoint != null) {
                networkData.put(KEY_AP_NAME, accessPoint);
            }

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_NETWORK);
            values.put(KEY_DATA, networkData.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
            updateNetworkProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }

    public JSONObject getLastNetworkEvent() throws JSONException {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = KEY_TIMESTAMP + " DESC";
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{KEY_ID, KEY_TIMESTAMP, KEY_TYPE, KEY_DATA}, KEY_TYPE + " =? ", new String[]{String.valueOf(KEY_TYPE_NETWORK)}, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                String data = cursor.getString(cursor.getColumnIndex(Database.KEY_DATA));
                JSONObject jData = new JSONObject(data);
                return jData;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void addBTEvent(long timeStamp, String btDevice) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject btDeviceData = new JSONObject();
            btDeviceData.put(KEY_BT_DEVICE_NAME, btDevice);

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_BT);
            values.put(KEY_DATA, btDeviceData.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }

    public JSONObject getLastBTEvent() throws JSONException {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = KEY_TIMESTAMP + " DESC";
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{KEY_ID, KEY_TIMESTAMP, KEY_TYPE, KEY_DATA}, KEY_TYPE + " =? ", new String[]{String.valueOf(KEY_TYPE_BT)}, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                String data = cursor.getString(cursor.getColumnIndex(Database.KEY_DATA));
                JSONObject jData = new JSONObject(data);
                return jData;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public void addNfcTagEvent(long timeStamp, String tag) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject nfcData = new JSONObject();
            nfcData.put(KEY_NFC_TAG_ID, tag);

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_NFC);
            values.put(KEY_DATA, nfcData.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }

    public void addPowerEvent(long timeStamp, boolean charging) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject networkData = new JSONObject();
            networkData.put(KEY_POWER_CHARGING, charging);

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_POWER);
            values.put(KEY_DATA, networkData.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }

    public JSONObject getLastPowerEvent() throws JSONException {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = KEY_TIMESTAMP + " DESC";
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{KEY_ID, KEY_TIMESTAMP, KEY_TYPE, KEY_DATA}, KEY_TYPE + " =? ", new String[]{String.valueOf(KEY_TYPE_POWER)}, null, null, orderBy);
        try {
            if (cursor.moveToFirst()) {
                String data = cursor.getString(cursor.getColumnIndex(Database.KEY_DATA));
                JSONObject jData = new JSONObject(data);
                return jData;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public Cursor getEvents() {
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = KEY_TIMESTAMP + " DESC";
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{KEY_ID, KEY_TIMESTAMP, KEY_TYPE, KEY_DATA}, null, null, null, null, orderBy);
        return cursor;
    }

    public Cursor query(String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EVENTS, projection, selection, selectionArgs, null, null, sortOrder);
    }

    public void clearDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_EVENTS);
        db.close();
    }

    private void updateProvider() {
        mContext.getContentResolver().notifyChange(
                Uri.parse("content://" + EventProvider.AUTHORITY + "/events/all"), null);
    }

    private void updateNetworkProvider() {
        mContext.getContentResolver().notifyChange(
                Uri.parse("content://" + EventProvider.AUTHORITY + "/events/network"), null);
    }

    private void updateGeofenceProvider() {
        mContext.getContentResolver().notifyChange(
                Uri.parse("content://" + EventProvider.AUTHORITY + "/events/geofence"), null);
    }

    public void addGeofenceEvent(long timeStamp, String geoFenceId, String action) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            JSONObject fenceDate = new JSONObject();
            fenceDate.put(KEY_GEOFENCE_NAME, geoFenceId);
            fenceDate.put(KEY_GEOFENCE_ACTION_TYPE, action);

            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timeStamp);
            values.put(KEY_TYPE, KEY_TYPE_GEFOENCE);
            values.put(KEY_DATA, fenceDate.toString());
            db.insert(TABLE_EVENTS, null, values);
            updateProvider();
            updateGeofenceProvider();
        } catch (JSONException e) {

        } finally {
            db.close();
        }
    }
}
