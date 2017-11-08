package co.krypt.kryptonite.transport;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.protocol.NetworkMessage;
import co.krypt.kryptonite.silo.Silo;

/**
 * Created by Kevin King on 12/20/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class BluetoothTransport extends BroadcastReceiver {
    private static final UUID KR_BLUETOOTH_CHARACTERISTIC = UUID.fromString("20F53E48-C08D-423A-B2C2-1C797889AF24");
    private static final UUID CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "BluetoothTransport";

    private final BluetoothAdapter adapter;
    private final Set<UUID> allServiceUUIDS = new HashSet<>();
    private final Set<UUID> advertisingServiceUUIDS = new HashSet<>();

    private final Map<UUID, BluetoothGattService> servicesByUUID = new HashMap<>();
    private final Map<UUID, BluetoothGattCharacteristic> characteristicsByUUID = new HashMap<>();

    private final Map<UUID, Set<BluetoothDevice>> subscribedDevicesByUUID = new HashMap<>();
    private boolean readyToUpdateSubscribers = true;
    private final List<Pair<UUID, byte[]>> queuedOutgoingSplitsWithServiceUUID = new LinkedList<>();
    private final AtomicInteger mtu = new AtomicInteger(512);

    private final Map<UUID, NetworkMessage> lastOutgoingMessage = new HashMap<>();

    private final Map<BluetoothGattCharacteristic, Pair<Byte, ByteArrayOutputStream>> incomingMessageBuffersByCharacteristic = new HashMap<>();

    private final AdvertiseCallback advertiseCallback;
    private final BluetoothGattServer gattServer;
    private final Context context;

    private final ScheduledThreadPoolExecutor advertiseLogicPool = new ScheduledThreadPoolExecutor(1);

    private BluetoothTransport(Context context, BluetoothManager manager, BluetoothAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
        final BluetoothTransport self = this;

        advertiseCallback = new AdvertiseCallback() {
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.v(TAG, "advertising: " + settingsInEffect.toString());
            }
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "error starting advertising: " + errorCode);
            }
        };

        final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTED:
                            Log.v(TAG, "device connected");
                        case BluetoothProfile.STATE_DISCONNECTED:
                            Log.v(TAG, "device disconnected");
                    }
                } else {
                    Log.v(TAG, "device connection state change failure -- status: " + status + " state: " + newState);
                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v(TAG, "service added: " + service.getUuid());
                } else {
                    Log.v(TAG, "failed to add service: " + service.getUuid() + " status: " + status);
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.v(TAG, "characteristic read request");
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.v(TAG, "characteristic write request " + value.length + " bytes");
                self.onCharacteristicWrite(characteristic, value);
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
                }
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.v(TAG, "descriptor read request");
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (descriptor.getUuid().equals(CONFIGURATION_DESCRIPTOR_UUID)) {
                    UUID serviceUUID = descriptor.getCharacteristic().getService().getUuid();
                    Set<BluetoothDevice> subscribedDevices = new HashSet<>();
                    if (subscribedDevicesByUUID.containsKey(serviceUUID)) {
                        subscribedDevices = subscribedDevicesByUUID.get(serviceUUID);
                    }
                    subscribedDevices.add(device);
                    subscribedDevicesByUUID.put(serviceUUID, subscribedDevices);
                    if (responseNeeded) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                        Log.v(TAG, "device subscribed: " + device.toString() + ", sent response");
                    } else {
                        Log.v(TAG, "device subscribed: " + device.toString());
                    }
                    NetworkMessage lastOutgoingMessage = self.lastOutgoingMessage.get(serviceUUID);
                    if (lastOutgoingMessage != null) {
                        try {
                            send(serviceUUID, lastOutgoingMessage);
                        } catch (TransportException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                } else {
                    Log.v(TAG, "unhandled descriptor write request");
                }
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.v(TAG, "onExecuteWrite");
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.v(TAG, "onNotificationSent");
                tryWrite();
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                Log.v(TAG, "onMtuChanged: " + mtu);
                self.mtu.set(mtu);
            }
        };

        gattServer = manager.openGattServer(context, gattServerCallback);

        IntentFilter bluetoothStateFilter = new IntentFilter();
        bluetoothStateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothStateFilter.addAction(MainActivity.LOCATION_PERMISSION_GRANTED_ACTION);
        context.registerReceiver(bluetoothStateReceiver, bluetoothStateFilter);
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "bluetooth adapter intent action: " + intent.getAction());
            if (intent.getAction() == null) {
                return;
            }
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (state != previousState) {
                        switch (state) {
                            case BluetoothAdapter.STATE_ON:
                                readyToUpdateSubscribers = true;
                                advertiseLogicPool.execute(advertiseLogic);
                                break;
                            case BluetoothAdapter.STATE_OFF:
                                readyToUpdateSubscribers = false;
                                advertiseLogicPool.execute(advertiseLogic);
                                break;
                        }
                    } else {
                        Log.v(TAG, "ignoring BluetoothAdapter state change; previous == current");
                    }
                    break;
                case MainActivity.LOCATION_PERMISSION_GRANTED_ACTION:
                    advertiseLogicPool.execute(advertiseLogic);
                    break;
            }

        }
    };

    public synchronized void stop() {
        //TODO unimplemented
        context.unregisterReceiver(bluetoothStateReceiver);
    }


    public static synchronized BluetoothTransport init(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }
        final BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            return null;
        }
        return new BluetoothTransport(context, manager, manager.getAdapter());
    }

    public synchronized void add(Pairing pairing) {
        allServiceUUIDS.add(pairing.uuid);
        advertiseLogicPool.execute(advertiseLogic);
    }

    public synchronized void remove(Pairing pairing) {
        allServiceUUIDS.remove(pairing.uuid);
        //TODO unimplemented
        advertiseLogicPool.execute(advertiseLogic);
    }

    private Runnable advertiseLogic = new Runnable() {
        @Override
        public void run() {
            BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
            if (advertiser != null && adapter.isEnabled() && BluetoothAdapter.STATE_ON == adapter.getState()) {
                try {
                    advertisingServiceUUIDS.clear();
                    advertiser.stopAdvertising(advertiseCallback);
                } catch (NullPointerException e) {
                    //  XXX: internal android bluetooth bug caused by
                    //  android.os.Parcel.readException (Parcel.java:1626)
                    //  android.os.Parcel.readException (Parcel.java:1573)
                    //  android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient (IBluetoothGatt.java:1010)
                    //  android.bluetooth.le.BluetoothLeScanner$BleScanCallbackWrapper.stopLeScan (BluetoothLeScanner.java:338)
                    //  android.bluetooth.le.BluetoothLeScanner.stopScan (BluetoothLeScanner.java:193)
                    //  co.krypt.kryptonite.transport.BluetoothTransport.advertiseLogic (BluetoothTransport.java:285)
                    //  co.krypt.kryptonite.transport.BluetoothTransport$4.run (BluetoothTransport.java:351)
                    //  java.lang.Thread.run (Thread.java:818)
                    e.printStackTrace();
                }
                if (allServiceUUIDS.size() > 0) {
                    //TODO: rotate advertisements

                    UUID advertiseUUID = allServiceUUIDS.iterator().next();
                    BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                            KR_BLUETOOTH_CHARACTERISTIC,
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ,
                            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
                    );
                    BluetoothGattDescriptor configurationDescriptor = new BluetoothGattDescriptor(
                            CONFIGURATION_DESCRIPTOR_UUID,
                            BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
                    characteristic.addDescriptor(configurationDescriptor);

                    BluetoothGattService service = new BluetoothGattService(advertiseUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                    service.addCharacteristic(characteristic);

                    characteristicsByUUID.put(service.getUuid(), characteristic);
                    servicesByUUID.put(service.getUuid(), service);

                    gattServer.addService(service);

                    AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
                    settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
                    settingsBuilder.setTimeout(0);
                    settingsBuilder.setConnectable(true);
                    AdvertiseSettings advertiseSettings = settingsBuilder.build();

                    AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
                    ParcelUuid serviceUUID = ParcelUuid.fromString(advertiseUUID.toString());
                    dataBuilder.addServiceUuid(serviceUUID);
                    dataBuilder.setIncludeDeviceName(false);
                    AdvertiseData advertiseData =  dataBuilder.build();

                    advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
                    advertisingServiceUUIDS.add(advertiseUUID);

                    Log.v(TAG, "advertising " + advertisingServiceUUIDS.toString());
                } else {
                    Log.v(TAG, "stopped advertising");
                }
            } else {
                Log.e(TAG, "bluetooth disabled, not advertising");
            }
        }
    };

    private synchronized void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, byte[] value) {
        Log.v(TAG, "onCharacteristicChanged");
        final UUID uuid = characteristic.getService().getUuid();
        if (value == null) {
            return;
        }
        if (value.length == 0) {
            return;
        }

        if (value.length > 1) {
            byte n = value[0];
            Log.v(TAG, "split " + String.valueOf(n) + " from " + characteristic.toString());
            ByteArrayOutputStream messageSplit = new ByteArrayOutputStream();
            messageSplit.write(value, 1, value.length - 1);
            ByteArrayOutputStream newMessageBuffer = new ByteArrayOutputStream();

            Pair<Byte, ByteArrayOutputStream> lastNAndBuffer = incomingMessageBuffersByCharacteristic.get(characteristic);
            try {
                if (lastNAndBuffer != null) {
                    if (lastNAndBuffer.first == n + 1) {
                        newMessageBuffer.write(lastNAndBuffer.second.toByteArray());
                    }
                    newMessageBuffer.write(messageSplit.toByteArray());
                } else {
                    newMessageBuffer = messageSplit;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if (n == 0) {
                Log.v(TAG, "received message of length " + String.valueOf(newMessageBuffer.toByteArray().length));
                incomingMessageBuffersByCharacteristic.remove(characteristic);
                Silo.shared(context).onMessage(uuid, newMessageBuffer.toByteArray(), "bluetooth");
            } else {
                incomingMessageBuffersByCharacteristic.put(characteristic, new Pair<>(n, newMessageBuffer));
            }
        }
    }

    public synchronized void send(Pairing pairing, NetworkMessage message) throws TransportException {
        lastOutgoingMessage.put(pairing.uuid, message);
        send(pairing.uuid, message);
    }

    private synchronized void send(UUID serviceUUID, NetworkMessage message) throws TransportException {
        BluetoothGattCharacteristic characteristic = characteristicsByUUID.get(serviceUUID);
        if (characteristic == null) {
            return;
        }
        for (byte[] split : splitMessage(message.bytes(), this.mtu.get())) {
            queuedOutgoingSplitsWithServiceUUID.add(new Pair<>(serviceUUID, split));
        }

        tryWrite();

    }

    private synchronized void tryWrite() {
        while (readyToUpdateSubscribers) {
            if (queuedOutgoingSplitsWithServiceUUID.size() == 0) {
                Log.v(TAG, "outgoing queue empty");
                break;
            }
            Pair<UUID, byte[]> next = queuedOutgoingSplitsWithServiceUUID.get(0);
            BluetoothGattCharacteristic characteristic = characteristicsByUUID.get(next.first);
            if (characteristic == null) {
                Log.v(TAG, "no characteristic");
                queuedOutgoingSplitsWithServiceUUID.remove(0);
                continue;
            }
            Set<BluetoothDevice> subscribedDevices = subscribedDevicesByUUID.get(next.first);
            if (subscribedDevices == null) {
                Log.v(TAG, "no subscribed devices");
                queuedOutgoingSplitsWithServiceUUID.remove(0);
                continue;
            }
            characteristic.setValue(next.second);
            if (next.second.length > 0) {
                Log.v(TAG, "set value n=" + String.valueOf(next.second[0]) + " length=" + String.valueOf(next.second.length));
            }
            for (BluetoothDevice device : subscribedDevices) {
                readyToUpdateSubscribers = gattServer.notifyCharacteristicChanged(device, characteristic, false);
            }
            Log.v(TAG, "notified");
            if (readyToUpdateSubscribers) {
                queuedOutgoingSplitsWithServiceUUID.remove(0);
            }
        }
        Log.v(TAG, "tryWrite() done");
    }

    private static synchronized List<byte[]> splitMessage(final byte[] message, final int mtu) {
        List<byte[]> splits = new ArrayList<>();
        if (message.length == 0 || message.length > (mtu - 1) * 255) {
            Log.e(TAG, "invalid message length for Bluetooth: " + String.valueOf(message.length));
            return splits;
        }
        if (mtu <= 1) {
            Log.e(TAG, "invalid mtu: " + String.valueOf(mtu));
            return splits;
        }

        int blockSize = mtu - 1;

        int offset = 0;
        for (int n = (message.length - 1) / blockSize; n >= 0; --n) {
            ByteArrayOutputStream split = new ByteArrayOutputStream();
            split.write(n);
            try {
                split.write(Arrays.copyOfRange(message, offset, Math.min(offset + blockSize, message.length)));
            } catch (IOException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
            splits.add(split.toByteArray());
            offset += blockSize;
        }
        Log.v(TAG, "split message into " + String.valueOf(splits.size()));
        return splits;
    }

    public static synchronized void requestUserEnableBluetooth(Activity activity, int requestID) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, requestID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
