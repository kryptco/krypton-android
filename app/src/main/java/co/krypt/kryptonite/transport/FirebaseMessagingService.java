package co.krypt.kryptonite.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.amazonaws.util.Base64;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import co.krypt.kryptonite.silo.Silo;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMessaging";
    public FirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> notification = remoteMessage.getData();
        if (!notification.containsKey("message") || !notification.containsKey("queue")) {
            Log.e(TAG, "notification does not contain a message and queue name");
            Log.e(TAG, notification.toString());
            return;
        }
        String message = notification.get("message");
        String queue = notification.get("queue");
        Log.i(TAG, "received message " + message + " from queue " + queue);
        Silo.shared(getApplicationContext()).onMessage(queue, Base64.decode(message));
    }
}
