package co.krypt.krypton.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NoAuthReceiver extends BroadcastReceiver {
    private static final String TAG = "NoAuthReceiver";

    public NoAuthReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String requestID = intent.getStringExtra("requestID");
        String action = intent.getStringExtra("action");
        new Thread(() -> Policy.onAction(context, requestID, action)).start();
    }
}
