package co.krypt.kryptonite.silo;

import android.content.Context;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.amazonaws.util.Base64;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.ProtocolException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.me.MeStorage;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pairing.Pairings;
import co.krypt.kryptonite.policy.Policy;
import co.krypt.kryptonite.protocol.AckResponse;
import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.MeResponse;
import co.krypt.kryptonite.protocol.NetworkMessage;
import co.krypt.kryptonite.protocol.PairingQR;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.Response;
import co.krypt.kryptonite.protocol.SignResponse;
import co.krypt.kryptonite.protocol.UnpairResponse;
import co.krypt.kryptonite.transport.BluetoothTransport;
import co.krypt.kryptonite.transport.SNSTransport;
import co.krypt.kryptonite.transport.SQSPoller;
import co.krypt.kryptonite.transport.SQSTransport;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Silo {
    private static final String TAG = "Silo";

    private static Silo singleton;

    private final Pairings pairingStorage;
    private final MeStorage meStorage;
    private HashMap<UUID, Pairing> activePairingsByUUID;
    private HashMap<Pairing, SQSPoller> pollers;
    private final BluetoothTransport bluetoothTransport;
    private final Context context;
    private final HashMap<Pairing, Long> lastRequestTimeSeconds = new HashMap<>();
    private final LruCache<String, Response> responseMemCacheByRequestID = new LruCache<>(8192);
    private DiskLruCache responseDiskCacheByRequestID;

    private Silo(Context context) {
        this.context = context;
        pairingStorage = new Pairings(context);
        meStorage = new MeStorage(context);
        Set<Pairing> pairings = pairingStorage.loadAll();
        activePairingsByUUID = new HashMap<>();
        bluetoothTransport = BluetoothTransport.init(context);
        for (Pairing p : pairings) {
            activePairingsByUUID.put(p.uuid, p);
            if (bluetoothTransport != null) {
                bluetoothTransport.add(p);
            }
        }
        pollers = new HashMap<>();

        try {
             responseDiskCacheByRequestID = DiskLruCache.open(context.getCacheDir(), 0, 1, 2 << 19);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Silo shared(Context context) {
        if (singleton == null) {
            singleton = new Silo(context);
        }
        return singleton;
    }

    public synchronized boolean hasActivity(Pairing pairing) {
        return lastRequestTimeSeconds.get(pairing) != null;
    }

    public Pairings pairings() {
        return pairingStorage;
    }

    public MeStorage meStorage() {
        return meStorage;
    }

    public synchronized void start() {
        for (Pairing pairing : activePairingsByUUID.values()) {
            Log.i(TAG, "starting "+ Base64.encodeAsString(pairing.workstationPublicKey));
            pollers.put(pairing, new SQSPoller(context, pairing));
        }
    }

    public synchronized void stop() {
        Log.i(TAG, "stopping");
        for (SQSPoller poller: pollers.values()) {
            poller.stop();
        }
        pollers.clear();
    }

    public synchronized void exit() {
        bluetoothTransport.stop();
        if (responseDiskCacheByRequestID != null) {
            try {
                responseDiskCacheByRequestID.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized Pairing pair(PairingQR pairingQR) throws CryptoException, TransportException {
        Pairing pairing = Pairing.generate(pairingQR);
        if (activePairingsByUUID.containsValue(pairing)) {
            Log.w(TAG, "already paired with " + pairing.workstationName);
            return activePairingsByUUID.get(pairing.uuid);
        }
        byte[] wrappedKey = pairing.wrapKey();
        NetworkMessage wrappedKeyMessage = new NetworkMessage(NetworkMessage.Header.WRAPPED_KEY, wrappedKey);
        send(pairing, wrappedKeyMessage);

        pairingStorage.pair(pairing);
        activePairingsByUUID.put(pairing.uuid, pairing);
        pollers.put(pairing, new SQSPoller(context, pairing));
        if (bluetoothTransport != null) {
            bluetoothTransport.add(pairing);
            bluetoothTransport.send(pairing, wrappedKeyMessage);
        }
        return pairing;
    }

    public synchronized void unpair(Pairing pairing, boolean sendResponse) {
        if (sendResponse) {
            Response unpairResponse = new Response();
            unpairResponse.requestID = "";
            unpairResponse.unpairResponse = new UnpairResponse();
            try {
                send(pairing, unpairResponse);
            } catch (CryptoException | TransportException e) {
                e.printStackTrace();
            }
        }
        pairingStorage.unpair(pairing);
        activePairingsByUUID.remove(pairing.uuid);
        SQSPoller poller = pollers.remove(pairing);
        if (poller != null) {
            poller.stop();
        }
        bluetoothTransport.remove(pairing);
    }

    public synchronized void unpairAll() {
        List<Pairing> toDelete = new ArrayList<>(activePairingsByUUID.values());
        for (Pairing pairing: toDelete) {
            unpair(pairing, true);
        }
    }

    public synchronized void onMessage(UUID pairingUUID, byte[] incoming) {
        try {
            NetworkMessage message = NetworkMessage.parse(incoming);
            Pairing pairing = activePairingsByUUID.get(pairingUUID);
            if (pairing == null) {
                Log.e(TAG, "not valid pairing: " + pairingUUID);
                return;
            }
            switch (message.header) {
                case CIPHERTEXT:
                    byte[] json = pairing.unseal(message.message);
                    Request request = JSON.fromJson(json, Request.class);
                    handle(pairing, request);
                    break;
                case WRAPPED_KEY:
                    break;
            }
        } catch (TransportException | IOException | InvalidKeyException | ProtocolException | CryptoException e) {
            e.printStackTrace();
        }
    }

    public void send(Pairing pairing, Response response) throws CryptoException, TransportException {
        byte[] responseJson = JSON.toJson(response).getBytes();
        byte[] sealed = pairing.seal(responseJson);
        send(pairing, new NetworkMessage(NetworkMessage.Header.CIPHERTEXT, sealed));
    }

    public void send(final Pairing pairing, final NetworkMessage message) throws TransportException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothTransport.send(pairing, message);
                } catch (TransportException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SQSTransport.sendMessage(pairing, message);
                } catch (TransportException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private synchronized boolean sendCachedResponseIfPresent(Pairing pairing, Request request) throws CryptoException, TransportException, IOException {
        if (responseDiskCacheByRequestID != null) {
            DiskLruCache.Snapshot cacheEntry = responseDiskCacheByRequestID.get(request.requestIDCacheKey());
            if (cacheEntry != null) {
                String cachedJSON = cacheEntry.getString(0);
                if (cachedJSON != null) {
                    send(pairing, JSON.fromJson(cachedJSON, Response.class));
                    Log.i(TAG, "sent cached response to " + request.requestID);
                    return true;
                } else {
                    Log.v(TAG, "no cache JSON");
                }
            } else {
                Log.v(TAG, "no cache entry");
            }
        }
        Response cachedResponse = responseMemCacheByRequestID.get(request.requestID);
        if (cachedResponse != null) {
            send(pairing, cachedResponse);
            Log.i(TAG, "sent memory cached response to " + request.requestID);
            return true;
        }
        return false;
    }


    public synchronized void handle(Pairing pairing, Request request) throws CryptoException, TransportException, IOException, InvalidKeyException, ProtocolException {
        if (Math.abs(request.unixSeconds - (System.currentTimeMillis() / 1000)) > 120) {
            throw new ProtocolException("invalid request time");
        }

        if (request.unpairRequest != null) {
            unpair(pairing, false);
        }

        if (sendCachedResponseIfPresent(pairing, request)) {
            return;
        }

        lastRequestTimeSeconds.put(pairing, System.currentTimeMillis() / 1000);

        if (request.signRequest != null && !pairings().isApprovedNow(pairing)) {
            Policy.requestApproval(context, pairing, request);
            if (request.sendACK == true) {
                Response ackResponse = Response.with(request);
                ackResponse.ackResponse = new AckResponse();
                send(pairing, ackResponse);
            }
        } else {
            respondToRequest(pairing, request, true);
        }
    }

    public synchronized void respondToRequest(Pairing pairing, Request request, boolean signatureAllowed) throws CryptoException, InvalidKeyException, IOException, TransportException {
        if (sendCachedResponseIfPresent(pairing, request)) {
            return;
        }

        Response response = Response.with(request);
        if (request.meRequest != null) {
            response.meResponse = new MeResponse(meStorage.load());
        }

        if (request.signRequest != null) {
            response.signResponse = new SignResponse();
            if (signatureAllowed) {
                try {
                    SSHKeyPair key = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
                    if (MessageDigest.isEqual(request.signRequest.publicKeyFingerprint, key.publicKeyFingerprint())) {
                        response.signResponse.signature = key.signDigest(request.signRequest.digest);
                        pairings().appendToLog(pairing, new SignatureLog(request.signRequest.digest, request.signRequest.command, System.currentTimeMillis() / 1000));
                        Notifications.notifySuccess(context, pairing, request);
                    } else {
                        Log.e(TAG, Base64.encodeAsString(request.signRequest.publicKeyFingerprint) + " != " + Base64.encodeAsString(key.publicKeyFingerprint()));
                        response.signResponse.error = "unknown key fingerprint";
                    }
                } catch (NoSuchAlgorithmException | SignatureException e) {
                    response.signResponse.error = "unknown error";
                    e.printStackTrace();
                }
            } else {
                response.signResponse.error = "rejected";
            }
        }

        response.snsEndpointARN = SNSTransport.getInstance(context).getEndpointARN();

        if (responseDiskCacheByRequestID != null) {
            DiskLruCache.Editor cacheEditor = responseDiskCacheByRequestID.edit(request.requestIDCacheKey());
            cacheEditor.set(0, JSON.toJson(response));
            cacheEditor.commit();
            responseDiskCacheByRequestID.flush();
        }
        responseMemCacheByRequestID.put(request.requestID, response);
        send(pairing, response);
    }

}
