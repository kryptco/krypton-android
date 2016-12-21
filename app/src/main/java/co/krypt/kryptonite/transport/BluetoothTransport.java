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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.silo.Silo;

/**
 * Created by Kevin King on 12/20/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class BluetoothTransport {
    private static final UUID KR_BLUETOOTH_CHARACTERISTIC = UUID.fromString("20F53E48-C08D-423A-B2C2-1C797889AF24");
    private static final String TAG = "BluetoothTransport";

    private final BluetoothManager manager;
    private final BluetoothAdapter adapter;
    private final Set<UUID> allServiceUUIDS = new HashSet<>();
    private final Set<UUID> scanningServiceUUIDS = new HashSet<>();

    private final Set<BluetoothDevice> connectedDevices = new HashSet<>();
    private final Set<BluetoothDevice> connectingDevices = new HashSet<>();
    private final Map<BluetoothDevice, Set<UUID>> discoveredServiceUUIDSByDevice = new HashMap<>();
    private final Map<UUID, BluetoothGattCharacteristic> characteristicsByServiceUUID = new HashMap<>();

    private final Map<UUID, Pair<Byte, ByteArrayOutputStream>> incomingMessageBuffersByServiceUUID = new HashMap<>();

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
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                self.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            }
        };

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
        allServiceUUIDS.add(pairing.uuid);
        scanLogic();
    }

    public synchronized void scanLogic() {
        Set<UUID> serviceUUIDSToScan = new HashSet<>(allServiceUUIDS);
        for (Set<UUID> discoveredServices: discoveredServiceUUIDSByDevice.values()) {
            serviceUUIDSToScan.removeAll(discoveredServices);
        }

        List<ScanFilter> scanFilters = new ArrayList<>();
        for (UUID serviceUUID : serviceUUIDSToScan) {
            ScanFilter.Builder scanFilter = new ScanFilter.Builder();
            scanFilter.setServiceUuid(new ParcelUuid(serviceUUID));
            scanFilters.add(scanFilter.build());
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0)
                .build();

        scanningServiceUUIDS.clear();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(scanCallback);
            if (serviceUUIDSToScan.size() > 0) {
                scanner.startScan(scanFilters, scanSettings, scanCallback);
                scanningServiceUUIDS.addAll(serviceUUIDSToScan);
                Log.v(TAG, "scanning for " + scanningServiceUUIDS.toString());
            } else {
                Log.v(TAG, "stopped scanning");
            }
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
            default:
                break;
        }
    }

    private synchronized void handleScanResult(ScanResult result) {
        if (connectingDevices.contains(result.getDevice()) || connectedDevices.contains(result.getDevice())) {
            Log.v(TAG, "already connecting to " + result.getDevice().getAddress());
            return;
        }
        connectingDevices.add(result.getDevice());
        result.getDevice().connectGatt(context, true, gattCallback);
        Log.v(TAG, "scan result: " + result.getDevice().getName());
    }

    private synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED:
                Log.v(TAG, gatt.getDevice().getAddress() + " connected");
                gatt.discoverServices();
                connectingDevices.remove(gatt.getDevice());
                connectedDevices.add(gatt.getDevice());
                scanLogic();
                break;
            case BluetoothGatt.STATE_DISCONNECTED:
                Log.v(TAG, gatt.getDevice().getAddress() + " disconnected");
                connectedDevices.remove(gatt.getDevice());
                discoveredServiceUUIDSByDevice.remove(gatt.getDevice());
                scanLogic();
                break;
        }
    }

    private synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (!connectedDevices.contains(gatt.getDevice())) {
            Log.e(TAG, gatt.getDevice().getAddress() + " not connected");
            return;
        }
        Log.v(TAG, gatt.getDevice().getAddress() + " discovered services");
        HashSet<UUID> serviceUUIDS = new HashSet<>();
        for (BluetoothGattService service : gatt.getServices()) {
            if (allServiceUUIDS.contains(service.getUuid())) {
                serviceUUIDS.add(service.getUuid());
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(KR_BLUETOOTH_CHARACTERISTIC);
                if (characteristic != null) {
                    boolean success = gatt.setCharacteristicNotification(characteristic, true);
                    if (!success) {
                        Log.e(TAG, "failed to enable characteristic notification");
                    }
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    characteristicsByServiceUUID.put(service.getUuid(), characteristic);
                }
            }
        }
        discoveredServiceUUIDSByDevice.put(gatt.getDevice(), serviceUUIDS);

        scanLogic();
    }

    private synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getService().getUuid();
        byte[] value = characteristic.getValue();
        if (value == null) {
            return;
        }
        if (value.length == 0) {
            return;
        }

        if (value.length == 1) {
            switch (value[0]) {

            }
        }


        if (value.length > 1) {
            byte n = value[0];
            ByteArrayOutputStream messageSplit = new ByteArrayOutputStream();
            messageSplit.write(value, 1, value.length - 1);
            ByteArrayOutputStream newMessageBuffer = new ByteArrayOutputStream();

            Pair<Byte, ByteArrayOutputStream> lastNAndBuffer = incomingMessageBuffersByServiceUUID.get(uuid);
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
                incomingMessageBuffersByServiceUUID.remove(uuid);
                Silo.shared(context).onMessage(uuid, newMessageBuffer.toByteArray());
            } else {
                incomingMessageBuffersByServiceUUID.put(uuid, new Pair<>(n, newMessageBuffer));
            }
        }
    }

    public static synchronized void requestUserEnableBluetooth(Activity activity, int requestID) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, requestID);
    }
}
