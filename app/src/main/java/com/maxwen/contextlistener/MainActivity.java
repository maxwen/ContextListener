package com.maxwen.contextlistener;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.maxwen.contextlistener.db.Database;
import com.maxwen.contextlistener.location.GeofenceService;
import com.maxwen.contextlistener.service.EventService;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final String TAG = "MainActivity";
    private ListView mEventList;
    private EventCursorAdapter mAdapter;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy:MM:dd kk:mm:ss");
    private Database mEventsDB;
    private Handler mHandler = new Handler();
    private EventsObserver mEventsObserver;
    private GeofenceService mGeofenceService;

    public static final String KEY_ID = "_id";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_DATA = "data";
    public static final String KEY_TYPE = "type";

    public static final Uri EVENTS_ALL_URI
            = Uri.parse("content://com.maxwen.contextlistener/events/all");

    final String[] EVENTS_PROJECTION = new String[]{
            KEY_ID,
            KEY_TIMESTAMP,
            KEY_TYPE,
            KEY_DATA
    };

    private class EventsObserver extends ContentObserver {
        EventsObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "onChange " + uri);
            try {
                if (uri.equals(EVENTS_ALL_URI)) {
                    mAdapter.swapCursor(getEvents());
                }
            } catch (Exception e) {

            }
        }
    }

    private class EventCursorAdapter extends CursorAdapter {
        public EventCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.event_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView timeStampText = view.findViewById(R.id.timestamp);
            TextView dataText = view.findViewById(R.id.data);
            long timeStamp = cursor.getLong(cursor.getColumnIndex(Database.KEY_TIMESTAMP));
            String data = cursor.getString(cursor.getColumnIndex(Database.KEY_DATA));
            TIME_FORMAT.setTimeZone(TimeZone.getDefault());
            timeStampText.setText(TIME_FORMAT.format(timeStamp));
            dataText.setText(data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mGeofenceService = new GeofenceService(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_delete);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Database(MainActivity.this).clearDB();
                mGeofenceService.initHomeFence();
                mAdapter.swapCursor(mEventsDB.getEvents());
                EventService.startEventService(MainActivity.this);
            }
        });

        fab = (FloatingActionButton) findViewById(R.id.fab_geofence);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setGeofenceToHere();
            }
        });
        Config.setEnabled(this, true);

        mEventsObserver = new EventsObserver();

        mEventsDB = new Database(this);
        mAdapter = new EventCursorAdapter(this, mEventsDB.getEvents());
        mEventList = (ListView) findViewById(R.id.event_list);
        mEventList.setAdapter(mAdapter);

        if (checkLocationPermissions()) {
            if (!checkLocationEnabled()) {
                Log.d(TAG, "Locations disabled");
                showLocationDisabledDialog();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mEventsObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(EVENTS_ALL_URI,
                true, mEventsObserver);
        mAdapter.swapCursor(getEvents());
    }

    private boolean checkLocationEnabled() {
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, -1) != Settings.Secure.LOCATION_MODE_OFF;
    }

    private void showLocationDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Dialog dialog;

        // Build and show the dialog
        builder.setTitle("Location disabled");
        builder.setMessage("Location access is disabled");
        builder.setCancelable(false);
        builder.setPositiveButton("Enable",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        dialog = builder.create();
        dialog.show();
    }

    private boolean checkLocationPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return false;
        } else {
            EventService.startEventService(this);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    EventService.startEventService(this);
                }
                break;
            }
        }
    }
    private Cursor getEvents() {
        String orderBy = KEY_TIMESTAMP + " DESC";
        return getContentResolver().query(EVENTS_ALL_URI, EVENTS_PROJECTION,
                null, null, orderBy);
    }

    private void setGeofenceToHere() {
        mGeofenceService.setGeofenceToHere();
    }
}
