package com.maxwen.contextlistener;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.maxwen.contextlistener.bluetooth.BluetoothService;
import com.maxwen.contextlistener.db.Database;
import com.maxwen.contextlistener.location.LocationService;
import com.maxwen.contextlistener.network.NetworkService;
import com.maxwen.contextlistener.nfc.NfcService;
import com.maxwen.contextlistener.power.PowerService;
import com.maxwen.contextlistener.service.EventService;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private static final String TAG = "MainActivity";
    private UpdateReceiver mReceiver;
    private ListView mEventList;
    private EventCursorAdapter mAdapter;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy:MM:dd kk:mm:ss");
    private Database mEventsDB;

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LocationService.ACTION_BROADCAST) ||
                    action.equals(NetworkService.ACTION_BROADCAST) ||
                    action.equals(BluetoothService.ACTION_BROADCAST) ||
                    action.equals(NfcService.ACTION_BROADCAST) ||
                    action.equals(PowerService.ACTION_BROADCAST)) {
                mAdapter.swapCursor(mEventsDB.getEvents());
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Database(MainActivity.this).clearDB();
                mAdapter.swapCursor(mEventsDB.getEvents());
            }
        });

        Config.setEnabled(this, true);

        mReceiver = new UpdateReceiver();

        mEventsDB = new Database(this);
        mAdapter = new EventCursorAdapter(this, mEventsDB.getEvents());
        mEventList = (ListView) findViewById(R.id.event_list);
        mEventList.setAdapter(mAdapter);

        checkLocationPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationService.ACTION_BROADCAST);
        filter.addAction(NetworkService.ACTION_BROADCAST);
        filter.addAction(BluetoothService.ACTION_BROADCAST);
        filter.addAction(NfcService.ACTION_BROADCAST);
        filter.addAction(PowerService.ACTION_BROADCAST);
        registerReceiver(mReceiver, filter);
        mAdapter.swapCursor(mEventsDB.getEvents());
    }

    private void checkLocationPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            EventService.startEventService(this);
        }
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
}
