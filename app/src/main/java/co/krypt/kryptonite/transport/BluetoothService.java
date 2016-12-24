package co.krypt.kryptonite.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import co.krypt.kryptonite.silo.Silo;

public class BluetoothService extends Service {
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
