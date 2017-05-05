package co.krypt.kryptonite.policy;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.silo.Notifications;
import co.krypt.kryptonite.silo.Silo;

/**
 * Created by Kevin King on 12/18/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Policy {
    private static final String TAG = "Policy";
    public static final String APPROVE_ONCE = "approve-once";
    public static final String APPROVE_TEMPORARILY = "approve-temporarily";
    public static final String REJECT = "reject";

    private static final HashMap<String, Pair<Pairing, Request>> pendingRequestCache = new HashMap<>();

    public static synchronized boolean requestApproval(Context context, Pairing pairing, Request request) {
        if (pendingRequestCache.get(request.requestID) != null) {
            return false;
        }
        pendingRequestCache.put(request.requestID, new Pair<>(pairing, request));
        Notifications.requestApproval(context, pairing, request);
        return true;
    }

    public static synchronized Pair<Pairing, Request> getPendingRequestAndPairing(String requestID) {
        return pendingRequestCache.get(requestID);
    }

    public static synchronized void onAction(final Context context, final String requestID, final String action) {
        Log.i(TAG, action + " requestID " + requestID);
        final Pair<Pairing, Request> pairingAndRequest = pendingRequestCache.remove(requestID);
        if (pairingAndRequest == null) {
            Log.e(TAG, "requestID " + requestID + " not pending");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Notifications.clearRequest(context, pairingAndRequest.second);
                switch (action) {
                    case APPROVE_ONCE:
                        try {
                            Silo.shared(context).respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                            new Analytics(context).postEvent("signature", "background approve", "once", null, false);
                        } catch (CryptoException | InvalidKeyException | IOException | TransportException | InvalidKeySpecException | NoSuchProviderException e) {
                            e.printStackTrace();
                        }
                        break;
                    case APPROVE_TEMPORARILY:
                        try {
                            Silo.shared(context).pairings().setApprovedUntil(pairingAndRequest.first.getUUIDString(), (System.currentTimeMillis() / 1000) + 3600);
                            Silo.shared(context).respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                            new Analytics(context).postEvent("signature", "background approve", "time", 3600, false);
                        } catch (CryptoException | InvalidKeyException | IOException | TransportException | NoSuchProviderException | InvalidKeySpecException e) {
                            e.printStackTrace();
                        }
                        break;
                    case REJECT:
                        try {
                            Silo.shared(context).respondToRequest(pairingAndRequest.first, pairingAndRequest.second, false);
                            new Analytics(context).postEvent("signature", "background reject", null, null, false);
                        } catch (CryptoException | InvalidKeyException | IOException | TransportException | InvalidKeySpecException | NoSuchProviderException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }).start();
    }

}
