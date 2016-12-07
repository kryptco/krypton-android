package co.krypt.kryptonite.transport;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.silo.Silo;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SQSPoller {
    private final Context context;
    private final Pairing pairing;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private static final String TAG = "SQSPoller";
    private static String accessKey = "AKIAJMZJ3X6MHMXRF7QQ";
    private static String secretKey = "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";

    public SQSPoller(final Context context, final Pairing pairing) {
        this.context = context;
        this.pairing = pairing;
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
                            Silo.shared(context).onMessage(pairing.getUUIDString(), message);
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
