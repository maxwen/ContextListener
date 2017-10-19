package com.maxwen.contextlistener.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.maxwen.contextlistener.Config;
import com.maxwen.contextlistener.MainActivity;
import com.maxwen.contextlistener.bluetooth.BluetoothService;
import com.maxwen.contextlistener.location.CustomLocationListener;
import com.maxwen.contextlistener.location.GeofenceService;
import com.maxwen.contextlistener.location.LocationService;
import com.maxwen.contextlistener.network.NetworkService;
import com.maxwen.contextlistener.power.PowerService;

import java.util.ArrayList;
import java.util.List;

public class EventService extends Service {
    private static final String TAG = "EventService";
    private static final boolean DEBUG = true;
    private static final String ACTION_UPDATE = "com.maxwen.contextlistener.service.ACTION_UPDATE";
    private static final String ACTION_ALARM = "com.maxwen.contextlistener.service.ACTION_ALARM";

    private static final String EVENT_LISTENER = "EVENT_LISTENER";
    private HandlerThread mHandlerThread;
    private PowerManager.WakeLock mWakeLock;
    private CustomLocationListener mLocationListener;

    private BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Config.isEnabled(context)) {
                return;
            }
            String action = intent.getAction();
            mWakeLock.acquire();
            try {
                if (DEBUG) Log.d(TAG, "onReceive " + action);
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    startEventService(context);
                }
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    NetworkService.updateNetworkInfo(context);
                }

                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    BluetoothService.handleBTConnect(context, intent, true);
                }

                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothService.handleBTConnect(context, intent, false);
                }

                if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                    PowerService.handlePowerEvent(context, status == BatteryManager.BATTERY_STATUS_CHARGING);
                }

                if (LocationManager.MODE_CHANGED_ACTION.equals(action)) {
                    if (checkLocationEnabled()) {
                        LocationService.updateLocation(context, mLocationListener);
                    }
                }
            } finally {
                mWakeLock.release();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
        mHandlerThread = new HandlerThread("EventService Thread");
        mHandlerThread.start();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(true);
        mLocationListener = new CustomLocationListener(this);
        // init stored geofences
        new GeofenceService(this);

        NotificationChannel channel = new NotificationChannel(
                EVENT_LISTENER,
                "Event listener",
                NotificationManager.IMPORTANCE_LOW);

        List<NotificationChannel> channelList = new ArrayList<>();
        channelList.add(channel);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannels(channelList);

        registerListener();
    }

    public static void startEventService(Context context) {
        start(context, ACTION_UPDATE);
    }

    private static void start(Context context, String action) {
        Intent i = new Intent(context, EventService.class);
        i.setAction(action);
        context.startForegroundService(i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, EventService.class);
        context.stopService(i);
    }

    private Notification getBackgroundNotification() {
        Notification.Builder builder = new Notification.Builder(this, EVENT_LISTENER)
                .setContentTitle("Event listener running");
        builder.setSmallIcon(android.R.drawable.ic_dialog_alert);
        Intent activityIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            if (DEBUG) Log.d(TAG, "onStartCommand - no intent");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (DEBUG) Log.d(TAG, "onStartCommand - start");
        mWakeLock.acquire();
        startForeground(1, getBackgroundNotification());
        try {
            if (!Config.isEnabled(this)) {
                Log.w(TAG, "Service started, but not enabled ... stopping");
                stopSelf();
                return START_NOT_STICKY;
            }

            NetworkService.updateNetworkInfo(this);
            if (checkLocationEnabled()) {
                LocationService.updateLocation(this, mLocationListener);
            }
        } finally {
            mWakeLock.release();
        }
        if (DEBUG) Log.d(TAG, "onStartCommand - end");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "onDestroy");
        unregisterListener();
    }

    private void registerListener() {
        if (DEBUG) Log.d(TAG, "registerListener");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);

        this.registerReceiver(mStateListener, filter);

        try {
            if (LocationService.checkPermissions(this)) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LocationService.LOCATION_REQUEST_MIN_TIME,
                        LocationService.LOCATION_EQUALS_THRESHOLD_METERS, mLocationListener);
            }
        } catch (SecurityException e) {
        }
    }

    private void unregisterListener() {
        if (DEBUG) Log.d(TAG, "unregisterListener");
        try {
            this.unregisterReceiver(mStateListener);
        } catch (Exception e) {
        }

        try {
            if (LocationService.checkPermissions(this)) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                lm.removeUpdates(mLocationListener);
            }
        } catch (SecurityException e) {
        }
    }

    private boolean checkLocationEnabled() {
        return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, -1) != Settings.Secure.LOCATION_MODE_OFF;
    }
}
