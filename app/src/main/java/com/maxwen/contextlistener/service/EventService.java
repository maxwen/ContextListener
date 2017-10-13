package com.maxwen.contextlistener.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.util.Log;

import com.maxwen.contextlistener.Config;
import com.maxwen.contextlistener.bluetooth.BluetoothService;
import com.maxwen.contextlistener.location.CustomLocationListener;
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
                    int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    PowerService.handlePowerEvent(context, chargePlug > 0 ? true : false);
                }
            } finally {
                mWakeLock.release();
            }
        }
    };

    public EventService() {
    }

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
        context.startService(i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, EventService.class);
        context.stopService(i);
    }

    private Notification getBackgroundNotification() {
        Notification.Builder builder = new Notification.Builder(this, EVENT_LISTENER)
                .setContentTitle("Event listener running");
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
            LocationService.updateLocation(this, mLocationListener);

            BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
            boolean charging = bm.isCharging();
            PowerService.handlePowerEvent(this, charging);
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

        this.registerReceiver(mStateListener, filter);

        try {
            if (LocationService.checkPermissions(this)) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                /*if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
                }*/
                lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mLocationListener);
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
}
