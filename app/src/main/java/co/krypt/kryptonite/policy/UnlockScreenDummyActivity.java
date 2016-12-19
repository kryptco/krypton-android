package co.krypt.kryptonite.policy;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Kevin King on 12/19/16.
 * Copyright 2016. KryptCo, Inc.
 *
 * Enforces unlocking of phone for notification actions that require user authentication.
 */
public class UnlockScreenDummyActivity extends Activity {
    private static final String TAG = "UnlockScreenActivity";

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "screen unlocked");
        handleIntent(getIntent());
        finish();
    }

    private void handleIntent(Intent intent) {
        intent.setClass(getApplicationContext(), NoAuthReceiver.class);
        PendingIntent forwardAction = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
        try {
            forwardAction.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent");
        super.onNewIntent(intent);

        handleIntent(intent);
    }
}
