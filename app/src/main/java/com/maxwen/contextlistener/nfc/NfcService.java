package com.maxwen.contextlistener.nfc;

import android.content.Context;
import android.content.Intent;

import com.maxwen.contextlistener.db.Database;

public class NfcService {
    private static final String TAG = "NfcService";
    private static final boolean DEBUG = true;
    public static final String ACTION_BROADCAST = "com.maxwen.contextlistener.nfc.NFC_UPDATE";


    public static void handleNfcTagScanned(Context context, String tag) {
        Database database = new Database(context);
        database.addNfcTagEvent(System.currentTimeMillis(), tag);

        Intent updateIntent = new Intent(ACTION_BROADCAST);
        context.sendBroadcast(updateIntent);
    }
}
