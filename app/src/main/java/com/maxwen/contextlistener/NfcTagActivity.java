package com.maxwen.contextlistener;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.maxwen.contextlistener.nfc.NdefReaderTask;

public class NfcTagActivity extends AppCompatActivity {

    private static final String TAG = "NfcTagActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            handleNfcIntent(getIntent());
        }
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d(TAG, "handleNfcIntent " + action);
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            new NdefReaderTask(this).execute(tag);
            finish();
        }
    }
}
