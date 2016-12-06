package co.krypt.kryptonite.silo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.amazonaws.util.Base64;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.krypt.kryptonite.JSON;
import co.krypt.kryptonite.KeyManager;
import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.NetworkMessage;
import co.krypt.kryptonite.Pairing;
import co.krypt.kryptonite.Pairings;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.protocol.MeResponse;
import co.krypt.kryptonite.protocol.PairingQR;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.Response;
import co.krypt.kryptonite.protocol.SignResponse;
import co.krypt.kryptonite.transport.SNSTransport;
import co.krypt.kryptonite.transport.SQSPoller;
import co.krypt.kryptonite.transport.SQSTransport;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Silo {
    private static final String TAG = "Silo";

    private static Silo singleton;

    private Pairings pairingStorage;
    private HashMap<String, Pairing> activePairingsByUUID;
    private HashMap<Pairing, SQSPoller> pollers;
    private final Context context;

    private Silo(Context context) {
        this.context = context;
        pairingStorage = new Pairings(context);
        Set<Pairing> pairings = pairingStorage.loadAll();
        activePairingsByUUID = new HashMap<>();
        for (Pairing p : pairings) {
            activePairingsByUUID.put(p.getUUIDString(), p);
        }
        pollers = new HashMap<>();
    }

    public static synchronized Silo shared(Context context) {
        if (singleton == null) {
            singleton = new Silo(context);
        }
        return singleton;
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

    public synchronized void pair(PairingQR pairingQR) throws CryptoException, TransportException {
        Pairing pairing = Pairing.generate(pairingQR);
        if (activePairingsByUUID.containsValue(pairing)) {
            Log.w(TAG, "already paired with " + pairing.workstationName);
            return;
        }
        byte[] wrappedKey = pairing.wrapKey();
        NetworkMessage wrappedKeyMessage = new NetworkMessage(NetworkMessage.Header.WRAPPED_KEY, wrappedKey);
        send(pairing, wrappedKeyMessage);

        pairingStorage.pair(pairing);
        activePairingsByUUID.put(pairing.getUUIDString(), pairing);
        pollers.put(pairing, new SQSPoller(context, pairing));
    }

    public synchronized void unpair(Pairing pairing) {
        //  TODO: send unpair response
        pairingStorage.unpair(pairing);
        activePairingsByUUID.remove(pairing.getUUIDString());
        SQSPoller poller = pollers.remove(pairing);
        poller.stop();
    }

    public synchronized void unpairAll() {
        List<Pairing> toDelete = new ArrayList<>(activePairingsByUUID.values());
        for (Pairing pairing: toDelete) {
            unpair(pairing);
        }
    }

    public synchronized void onMessage(String pairingUUID, byte[] incoming) {
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
                    Log.i(TAG, "got JSON " + new String(json, "UTF-8"));
                    Request request = JSON.fromJson(json, Request.class);
                    handle(pairing, request);
                    break;
                case WRAPPED_KEY:
                    break;
            }
        } catch (TransportException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException | InvalidKeyException e) {
            e.printStackTrace();
        } catch (CryptoException e) {
            e.printStackTrace();
        }
    }

    public static void send(Pairing pairing, NetworkMessage message) throws TransportException {
        SQSTransport.sendMessage(pairing, message);
    }

    public synchronized void handle(Pairing pairing, Request request) throws CryptoException, TransportException, IOException, InvalidKeyException {
        Response response = Response.with(request);
        if (request.meRequest != null) {
            response.meResponse = new MeResponse(
                    new Profile(
                            "kevin@krypt.co",
                            KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG).publicKeySSHWireFormat()));
        }

        if (request.signRequest != null) {
            try {
                response.signResponse = new SignResponse();
                SSHKeyPair key = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
                if (MessageDigest.isEqual(request.signRequest.publicKeyFingerprint, key.publicKeyFingerprint())) {
                    response.signResponse.signature = key.signDigest(request.signRequest.digest);
                    Notifications.notify(context, request);
                } else {
                    Log.e(TAG, Base64.encodeAsString(request.signRequest.publicKeyFingerprint) + " != " + Base64.encodeAsString(key.publicKeyFingerprint()));
                    response.signResponse.error = "unknown key fingerprint";
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            }
        }

        response.snsEndpointARN = SNSTransport.getInstance(context).getEndpointARN();

        byte[] responseJson = JSON.toJson(response).getBytes();
        byte[] sealed = pairing.seal(responseJson);
        send(pairing, new NetworkMessage(NetworkMessage.Header.CIPHERTEXT, sealed));
    }

}
