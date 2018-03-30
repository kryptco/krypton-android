package co.krypt.krypton.transport;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.amazonaws.util.Base64;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.UUID;

import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.utils.Services;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final Services services = new Services();

    private static final String TAG = "FirebaseMessaging";
    public FirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> notification = remoteMessage.getData();

        if (notification.containsKey("new_team_data")) {
            EventBus.getDefault().post(new TeamService.UpdateTeamHomeDataWithNotification(this));
            return;
        }
        if (!notification.containsKey("message") || !notification.containsKey("queue")) {
            Log.e(TAG, "notification does not contain a message and queue name");
            Log.e(TAG, notification.toString());
            return;
        }

        // https://github.com/wix/react-native-notifications/issues/42
        PowerManager pm = (PowerManager)getBaseContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyCpuLock");
        wl_cpu.acquire(10000);

        String message = notification.get("message");
        String queue = notification.get("queue");
        try {
            UUID uuid = UUID.fromString(queue);
            Log.i(TAG, "received message " + message + " from queue " + queue);
            new Thread(() -> Silo.shared(getApplicationContext()).onMessage(uuid, Base64.decode(message), "remoteNotification")).start();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
