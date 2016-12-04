package co.krypt.kryptonite.transport;

import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import co.krypt.kryptonite.NetworkMessage;
import co.krypt.kryptonite.Pairing;
import co.krypt.kryptonite.silo.Silo;

import static co.krypt.kryptonite.transport.SQSTransport.receiveMessages;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SQSPoller {
    Pairing pairing;
    AtomicBoolean stopped = new AtomicBoolean(false);
    private static final String TAG = "SQSPoller";

    public SQSPoller(final Pairing pairing) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (stopped.get()) {
                        Log.d(TAG, "stopped polling " + pairing.workstationName);
                        return;
                    }
                    try {
                        Log.d(TAG, "read " + pairing.workstationName);
                        for (byte[] message : SQSTransport.receiveMessages(pairing)) {
                            NetworkMessage networkMessage = NetworkMessage.parse(message);
                            Silo.onMessage(pairing, networkMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            return;
                        }
                    }
                }
            }
        }).start();
    }

    public void stop() {
        stopped.set(true);
    }
}
