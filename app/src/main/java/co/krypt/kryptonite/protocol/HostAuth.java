package co.krypt.kryptonite.protocol;

import android.util.Log;

import com.amazonaws.util.Base64;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 2/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostAuth {
    private static final String TAG = "HostAuth";
    @SerializedName("host_key")
    public byte[] hostKey;

    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("host_names")
    public String[] hostNames;

    private static volatile boolean hasSSHWire = false;
    public static native boolean verifySessionID(final String pubkey, final String signature, final String sessionID);

    public boolean verifySessionID(final byte[] sessionID) {
        if (!hasSSHWire) {
            return false;
        }
        if (hostKey == null || signature == null || hostNames == null) {
            return false;
        }
        final String hostKey = Base64.encodeAsString(this.hostKey);
        final String signature = Base64.encodeAsString(this.signature);
        final String sessionIDB64 = Base64.encodeAsString(sessionID);
        return verifySessionID(hostKey, signature, sessionIDB64);
    }

    static {
        try {
            System.loadLibrary("sshwire");
            hasSSHWire = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
            FirebaseCrash.report(e);
        }
    }
}
