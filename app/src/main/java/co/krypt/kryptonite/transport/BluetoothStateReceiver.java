package co.krypt.kryptonite.transport;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothStateReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothStateReceiver";
    public BluetoothStateReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
            case BluetoothAdapter.STATE_ON:
                Log.v(TAG, "ON");
                break;
            case BluetoothAdapter.STATE_OFF:
                Log.v(TAG, "OFF");
                break;
        }
    }
}
