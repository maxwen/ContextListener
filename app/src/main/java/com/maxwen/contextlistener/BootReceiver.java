package com.maxwen.contextlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.maxwen.contextlistener.service.EventService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            Log.d(TAG, "start EventService");
            EventService.startEventService(context);
        } catch (Exception e) {
            Log.e(TAG, "Can't start service", e);
        }
    }
}
