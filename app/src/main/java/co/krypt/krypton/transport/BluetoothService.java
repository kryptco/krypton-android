package co.krypt.krypton.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.utils.Services;

public class BluetoothService extends Service {
    private static final Services services = new Services();

    private static final String TAG = "BluetoothService";
    private Silo silo;
    public BluetoothService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");

        silo = Silo.shared(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        silo.stop();
        silo.exit();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
