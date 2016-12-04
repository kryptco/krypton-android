package co.krypt.kryptonite.silo;

import android.content.Context;
import android.util.Log;

import com.amazonaws.util.Base64;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import co.krypt.kryptonite.JSON;
import co.krypt.kryptonite.KeyManager;
import co.krypt.kryptonite.NetworkMessage;
import co.krypt.kryptonite.Pairing;
import co.krypt.kryptonite.Pairings;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.protocol.MeResponse;
import co.krypt.kryptonite.protocol.PairingQR;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.Response;
import co.krypt.kryptonite.transport.SQSPoller;
import co.krypt.kryptonite.transport.SQSTransport;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Silo {
    private static final String TAG = "Silo";

    private static Silo singleton;

    private Pairings pairingStorage;
    private HashSet<Pairing> activePairings;
    private HashMap<Pairing, SQSPoller> pollers;

    private Silo(Context context) {
        pairingStorage = new Pairings(context);
        activePairings = pairingStorage.loadAll();
        pollers = new HashMap<>();
    }

    public static synchronized Silo shared(Context context) {
        if (singleton == null) {
            singleton = new Silo(context);
        }
        return singleton;
    }

    public synchronized void start() {
        for (Pairing pairing : activePairings) {
            Log.i(TAG, "starting "+ Base64.encodeAsString(pairing.workstationPublicKey));
            pollers.put(pairing, new SQSPoller(pairing));
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
        if (activePairings.contains(pairing)) {
            Log.w(TAG, "already paired with " + pairing.workstationName);
            return;
        }
        byte[] wrappedKey = pairing.wrapKey();
        NetworkMessage wrappedKeyMessage = new NetworkMessage(NetworkMessage.Header.WRAPPED_KEY, wrappedKey);
        send(pairing, wrappedKeyMessage);

        pairingStorage.pair(pairing);
        activePairings.add(pairing);
        pollers.put(pairing, new SQSPoller(pairing));
    }

    public synchronized void unpair(Pairing pairing) {
        //  TODO: send unpair response
        pairingStorage.unpair(pairing);
        activePairings.remove(pairing);
        SQSPoller poller = pollers.remove(pairing);
        poller.stop();
    }

    public synchronized void unpairAll() {
        List<Pairing> toDelete = new ArrayList<>(activePairings);
        for (Pairing pairing: toDelete) {
            unpair(pairing);
        }
    }

    public static synchronized void onMessage(Pairing pairing, NetworkMessage message) {
        try {
            switch (message.header) {
                case CIPHERTEXT:
                    byte[] json = pairing.unseal(message.message);
                    Log.i(TAG, "got JSON " + new String(json, "UTF-8"));
                    Request request = JSON.fromJson(json, Request.class);
                    Silo.handle(pairing, request);
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

    public static synchronized void handle(Pairing pairing, Request request) throws CryptoException, TransportException, IOException, InvalidKeyException {
        Response response = Response.with(request);
        if (request.meRequest != null) {
            response.meResponse = new MeResponse(
                    new Profile(
                            "kevin@krypt.co",
                            KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG).publicKeySSHWireFormat()));
        }

        byte[] responseJson = JSON.toJson(response).getBytes();
        byte[] sealed = pairing.seal(responseJson);
        send(pairing, new NetworkMessage(NetworkMessage.Header.CIPHERTEXT, sealed));
    }

}
