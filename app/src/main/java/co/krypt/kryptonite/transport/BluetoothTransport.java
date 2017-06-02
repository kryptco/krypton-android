package co.krypt.kryptonite.transport;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static final String TAG = "BluetoothTransport";

    public static final byte REFRESH_BYTE = 0;
    public static final byte PING_BYTE = 1;

    private final BluetoothManager manager;
    private final BluetoothAdapter adapter;
    private final Set<UUID> allServiceUUIDS = new HashSet<>();
    private final Set<UUID> scanningServiceUUIDS = new HashSet<>();

    private final Set<BluetoothDevice> connectedDevices = new HashSet<>();
    private final Set<BluetoothGatt> connectedGatts = new HashSet<>();
    private final Set<BluetoothDevice> connectingDevices = new HashSet<>();
    private final Set<BluetoothGatt> refreshedServiceCaches = new HashSet<>();
    private final Map<BluetoothDevice, Set<UUID>> discoveredServiceUUIDSByDevice = new HashMap<>();
    private final Map<UUID, Pair<BluetoothGatt, BluetoothGattCharacteristic>> characteristicsAndDevicesByServiceUUID = new HashMap<>();

    private final Map<UUID, NetworkMessage> pendingMessagesByUUID = new HashMap<>();

    private final Map<BluetoothGattCharacteristic, Pair<Byte, ByteArrayOutputStream>> incomingMessageBuffersByCharacteristic = new HashMap<>();
    private final Map<BluetoothGatt, Integer> mtuByBluetoothGatt = new HashMap<>();
    private final Map<BluetoothGattCharacteristic, List<byte[]>> outgoingMessagesByCharacteristic = new HashMap<>();
    private final Map<BluetoothGattCharacteristic, Boolean> characteristicWritePending = new HashMap<>();

    private final ScanCallback scanCallback;
    private final BluetoothGattCallback gattCallback;
    private final Context context;

    private BluetoothTransport(Context context, BluetoothManager manager, BluetoothAdapter adapter) {
        this.context = context;
        this.manager = manager;
        this.adapter = adapter;
        final BluetoothTransport self = this;

        scanCallback = new ScanCallback() {
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                self.onBatchScanResults(results);
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                self.onScanResult(callbackType, result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "scan failed with error code: " + String.valueOf(errorCode));
            }
        };

        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                self.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                self.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.v(TAG, "characteristic read");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v(TAG, "write completed");
                } else {
                    Log.v(TAG, "write failed");
                }
                self.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                self.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                self.onMtuChanged(gatt, mtu, status);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                Log.v(TAG, "reliable write completed");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                          int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v(TAG, "descriptor write completed");
                } else {
                    Log.v(TAG, "descriptor write failed");
                }
                self.onDescriptorWrite(gatt, descriptor, status);
            }
        };

        IntentFilter bluetoothStateFilter = new IntentFilter();
        bluetoothStateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        bluetoothStateFilter.addAction(MainActivity.LOCATION_PERMISSION_GRANTED_ACTION);
        context.registerReceiver(bluetoothStateReceiver, bluetoothStateFilter);
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "bluetooth adapter intent action: " + intent.getAction());
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (state != previousState) {
                        switch (state) {
                            case BluetoothAdapter.STATE_ON:
                                connectingDevices.clear();
                                scanLogic();
                                break;
                            case BluetoothAdapter.STATE_OFF:
                                connectingDevices.clear();
                                connectedDevices.clear();
                                connectedGatts.clear();
                                scanLogic();
                                break;
                        }
                    } else {
                        Log.v(TAG, "ignoring BluetoothAdapter state change; previous == current");
                    }
                    break;
                case MainActivity.LOCATION_PERMISSION_GRANTED_ACTION:
                    scanLogic();
                    break;
            }

        }
    };

    private synchronized boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
        }
        return false;
    }

    public synchronized void stop() {
        for (Pair<BluetoothGatt, BluetoothGattCharacteristic> pair : characteristicsAndDevicesByServiceUUID.values()) {
            pair.first.close();
            pair.first.disconnect();
        }
        adapter.cancelDiscovery();
        context.unregisterReceiver(bluetoothStateReceiver);
    }


    public static synchronized BluetoothTransport init(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }
        final BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return new BluetoothTransport(context, manager, manager.getAdapter());
    }

    public synchronized void add(Pairing pairing) {
        for (BluetoothGatt connectedGatt: connectedGatts) {
            refreshDeviceCache(connectedGatt);
            connectedGatt.discoverServices();
        }
        allServiceUUIDS.add(pairing.uuid);
        scanLogic();
    }

    public synchronized void remove(Pairing pairing) {
        allServiceUUIDS.remove(pairing.uuid);
        Pair<BluetoothGatt, BluetoothGattCharacteristic> characteristicAndDevice = characteristicsAndDevicesByServiceUUID.get(pairing.uuid);
        if (characteristicAndDevice != null) {
            characteristicAndDevice.first.setCharacteristicNotification(characteristicAndDevice.second, false);
            characteristicAndDevice.first.disconnect();
            mtuByBluetoothGatt.remove(characteristicAndDevice.first);
            outgoingMessagesByCharacteristic.remove(characteristicAndDevice.second);
        }
        scanLogic();
    }

    public synchronized void setPairingUUIDs(List<String> uuidStrings) {
        Set<UUID> uuids = new HashSet<>();
        for (String uuidString: uuidStrings) {
            try {
                uuids.add(UUID.fromString(uuidString));
            } catch(IllegalArgumentException e) {
                Log.e(TAG, "failed to parse uuid string " + uuidString);
            }
        }
        allServiceUUIDS.clear();
        allServiceUUIDS.addAll(uuids);
        Set<UUID> deleteUUIDS = new HashSet<UUID>(characteristicsAndDevicesByServiceUUID.keySet());
        deleteUUIDS.removeAll(allServiceUUIDS);
        for (UUID deleteUUID: uuids) {
            Pair<BluetoothGatt, BluetoothGattCharacteristic> removed = characteristicsAndDevicesByServiceUUID.remove(deleteUUID);
            if (removed != null) {
                removed.first.disconnect();
            }
        }
        scanLogic();
    }

    public synchronized void scanLogic() {
        Set<UUID> serviceUUIDSToScan = new HashSet<>(allServiceUUIDS);
        for (Set<UUID> discoveredServices: discoveredServiceUUIDSByDevice.values()) {
            serviceUUIDSToScan.removeAll(discoveredServices);
        }

        for (BluetoothDevice device: adapter.getBondedDevices()) {
            if (device.getName().equals("krsshagent") && BluetoothDevice.BOND_BONDED == device.getBondState()) {
                Log.v(TAG, "found bonded device: " + device.getName());
                if (!connectingDevices.contains(device) && !connectedDevices.contains(device)) {
                    if (!device.createBond()) {
                        Log.e(TAG, "error creating bond to " + device.getName() + ", not connecting");
                    } else {
                        Log.v(TAG, "connecting bonded device: " + device.getName());
                        connectingDevices.add(device);
                        device.connectGatt(context, true, gattCallback);
                    }
                }
            } else {
                Log.v(TAG, "ignoring bonded device: " + device.getName());
            }
        }

        List<ScanFilter> scanFilters = new ArrayList<>();
        for (UUID serviceUUID : serviceUUIDSToScan) {
            ScanFilter.Builder scanFilter = new ScanFilter.Builder();
            scanFilter.setServiceUuid(new ParcelUuid(serviceUUID));
            scanFilters.add(scanFilter.build());
        }
        ScanSettings scanSettings;
        try {
             scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    .setReportDelay(0)
                    .build();
        } catch (NoSuchMethodError e) {
            //  some phones do not have the ScanSettings.Builder() method
            FirebaseCrash.report(e);
            return;
        }

        scanningServiceUUIDS.clear();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null && adapter.isEnabled() && BluetoothAdapter.STATE_ON == adapter.getState()) {
            try {
                scanner.stopScan(scanCallback);
            } catch (NullPointerException e) {
                //  XXX: internal android bluetooth bug caused by
                //  android.os.Parcel.readException (Parcel.java:1626)
                //  android.os.Parcel.readException (Parcel.java:1573)
                //  android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient (IBluetoothGatt.java:1010)
                //  android.bluetooth.le.BluetoothLeScanner$BleScanCallbackWrapper.stopLeScan (BluetoothLeScanner.java:338)
                //  android.bluetooth.le.BluetoothLeScanner.stopScan (BluetoothLeScanner.java:193)
                //  co.krypt.kryptonite.transport.BluetoothTransport.scanLogic (BluetoothTransport.java:285)
                //  co.krypt.kryptonite.transport.BluetoothTransport$4.run (BluetoothTransport.java:351)
                //  java.lang.Thread.run (Thread.java:818)
                e.printStackTrace();
            }
            if (serviceUUIDSToScan.size() > 0) {
                scanner.startScan(scanFilters, scanSettings, scanCallback);
                scanningServiceUUIDS.addAll(serviceUUIDSToScan);
                Log.v(TAG, "scanning for " + scanningServiceUUIDS.toString());
            } else {
                Log.v(TAG, "stopped scanning");
            }
        } else {
            Log.e(TAG, "bluetooth disabled, not scanning");
        }
    }

    private synchronized void onBatchScanResults(List<ScanResult> results) {
        for (ScanResult result: results) {
            handleScanResult(result);
        }
    }

    private synchronized void onScanResult(int callbackType, ScanResult result) {
        switch (callbackType) {
            case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                handleScanResult(result);
                break;
            case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                handleScanResult(result);
                break;
            case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                Log.d(TAG, "scan result lost: " + result.getDevice().getName());
            default:
                break;
        }
    }

    private synchronized void handleScanResult(ScanResult result) {
        if (connectingDevices.contains(result.getDevice()) || connectedDevices.contains(result.getDevice())) {
            Log.v(TAG, "already connecting to " + result.getDevice().getName());
            return;
        }

        connectingDevices.add(result.getDevice());
        refreshDeviceCache(result.getDevice().connectGatt(context, true, gattCallback));
        Log.v(TAG, "scan result: " + result.getDevice().getName());
    }

    private synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                Log.v(TAG, gatt.getDevice().getName() + " connected");
                gatt.discoverServices();
                connectingDevices.remove(gatt.getDevice());
                connectedDevices.add(gatt.getDevice());
                connectedGatts.add(gatt);
                if (!gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)) {
                    Log.e(TAG, "initial connection priority request failed");
                }
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                Log.v(TAG, gatt.getDevice().getName() + " disconnected");
                connectedDevices.remove(gatt.getDevice());
                connectedGatts.remove(gatt);
                connectingDevices.remove(gatt.getDevice());
                discoveredServiceUUIDSByDevice.remove(gatt.getDevice());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        scanLogic();
                    }
                }).start();
                break;
        }
    }

    private synchronized void onServicesDiscovered(final BluetoothGatt gatt, int status) {
        if (!connectedDevices.contains(gatt.getDevice())) {
            Log.e(TAG, gatt.getDevice().getAddress() + " not connected");
            return;
        }
        Log.v(TAG, gatt.getDevice().getAddress() + " discovered services ");
        HashSet<UUID> serviceUUIDS = new HashSet<>();
        for (BluetoothGattService service : gatt.getServices()) {
            Log.v(TAG, service.getUuid().toString());
            if (allServiceUUIDS.contains(service.getUuid())) {
                Log.v(TAG, gatt.getDevice().getAddress() + " discovered paired service");
                serviceUUIDS.add(service.getUuid());
                final BluetoothGattCharacteristic characteristic = service.getCharacteristic(KR_BLUETOOTH_CHARACTERISTIC);
                if (characteristic != null) {
                    Pair<BluetoothGatt, BluetoothGattCharacteristic> oldCharacteristic = characteristicsAndDevicesByServiceUUID.get(service.getUuid());
                    if (oldCharacteristic != null) {
                        oldCharacteristic.first.setCharacteristicNotification(oldCharacteristic.second, false);
                    }
                    characteristicsAndDevicesByServiceUUID.put(service.getUuid(), new Pair<>(gatt, characteristic));
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    gatt.setCharacteristicNotification(characteristic, true);
                    Log.v(TAG, "subscribing to characteristic");
                    UUID configDescriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(configDescriptorUUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        if (!gatt.writeDescriptor(descriptor)) {
                            Log.e(TAG, "failed to write descriptor");
                        }
                    } else {
                        Log.e(TAG, "config descriptor null");
                    }
                }
            }
        }
        if (serviceUUIDS.size() > 0) {
            refreshedServiceCaches.remove(gatt);
            discoveredServiceUUIDSByDevice.put(gatt.getDevice(), serviceUUIDS);
            scanLogic();
        } else {
            Log.e(TAG, "failed to find paired service");
            if (refreshedServiceCaches.contains(gatt)) {
                return;
            }
            refreshedServiceCaches.add(gatt);
            refreshDeviceCache(gatt);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        gatt.discoverServices();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private synchronized void refreshConnection(UUID uuid, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.v(TAG, "refreshing connection to " + uuid.toString());
        gatt.setCharacteristicNotification(characteristic, false);
        gatt.disconnect();
        connectedDevices.remove(gatt.getDevice());
        connectedGatts.remove(gatt);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    scanLogic();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private synchronized void onCharacteristicChanged(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.v(TAG, "onCharacteristicChanged");
        final UUID uuid = characteristic.getService().getUuid();
        byte[] value = characteristic.getValue();
        if (value == null) {
            return;
        }
        if (value.length == 0) {
            return;
        }

        if (value.length == 1) {
            switch (value[0]) {
                case REFRESH_BYTE:
                    refreshConnection(uuid, gatt, characteristic);
                    break;
            }
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
                incomingMessageBuffersByCharacteristic.remove(uuid);
                Silo.shared(context).onMessage(uuid, newMessageBuffer.toByteArray(), "bluetooth");
            } else {
                incomingMessageBuffersByCharacteristic.put(characteristic, new Pair<>(n, newMessageBuffer));
            }
        }
    }

    public synchronized void send(Pairing pairing, NetworkMessage message) throws TransportException {
        Pair<BluetoothGatt, BluetoothGattCharacteristic> characteristicAndDevice = characteristicsAndDevicesByServiceUUID.get(pairing.uuid);
        if (characteristicAndDevice == null) {
            pendingMessagesByUUID.put(pairing.uuid, message);
            return;
        }
        Integer mtu = mtuByBluetoothGatt.get(characteristicAndDevice.first);
        if (mtu == null) {
            mtu = 20;
        }
        List<byte[]> queue = outgoingMessagesByCharacteristic.get(characteristicAndDevice.second);
        if (queue == null) {
            queue = new ArrayList<>();
        }
        queue.addAll(splitMessage(message.bytes(), mtu));
        outgoingMessagesByCharacteristic.put(characteristicAndDevice.second, queue);

        tryWrite(characteristicAndDevice.first, characteristicAndDevice.second);
    }

    private synchronized void tryWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        if (characteristicWritePending.get(characteristic) != null) {
            return;
        }
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        List<byte[]> queue = outgoingMessagesByCharacteristic.get(characteristic);
        if (queue == null) {
            queue = new ArrayList<>();
        }

        NetworkMessage pendingMessage = pendingMessagesByUUID.remove(characteristic.getService().getUuid());
        if (pendingMessage != null) {
            try {
                queue.addAll(splitMessage(pendingMessage.bytes(), 20));
                Log.v(TAG, "added pending message to queue");
            } catch (TransportException e) {
                e.printStackTrace();
            }
        }

        if (queue.size() > 0) {
            byte[] value = queue.remove(0);
            Log.v(TAG, "set value n=" + String.valueOf(value[0]) + " length=" + String.valueOf(value.length));
            characteristic.setValue(value);
            if (!gatt.writeCharacteristic(characteristic)) {
                Log.e(TAG, "characteristic write failed");
                queue.add(0, value);
            } else {
                characteristicWritePending.put(characteristic, true);
            }
        }
        outgoingMessagesByCharacteristic.put(characteristic, queue);
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

    private synchronized void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.v(TAG, "mtu change success: " + String.valueOf(mtu));
            Integer oldMTU = mtuByBluetoothGatt.get(gatt);
            if (oldMTU != null && oldMTU == mtu) {
                return;
            }
            mtuByBluetoothGatt.put(gatt, mtu);
        } else {
            Log.v(TAG, "mtu change failure: " + String.valueOf(mtu));
        }
    }

    private synchronized void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        characteristicWritePending.remove(characteristic);
        tryWrite(gatt, characteristic);
    }

    private synchronized void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int status) {
        tryWrite(gatt, descriptor.getCharacteristic());
    }

    public static synchronized void requestUserEnableBluetooth(Activity activity, int requestID) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, requestID);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
