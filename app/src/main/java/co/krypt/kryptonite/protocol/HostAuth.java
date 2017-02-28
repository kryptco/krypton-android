package co.krypt.kryptonite.protocol;

import com.amazonaws.util.Base64;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 2/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostAuth {
    @SerializedName("host_key")
    public byte[] hostKey;

    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("host_names")
    public String[] hostNames;

    public static native boolean verifySessionID(final String pubkey, final String signature, final String sessionID);

    public boolean verifySessionID(final byte[] sessionID) {
        if (hostKey == null || signature == null || hostNames == null) {
            return false;
        }
        final String hostKey = Base64.encodeAsString(this.hostKey);
        final String signature = Base64.encodeAsString(this.signature);
        final String sessionIDB64 = Base64.encodeAsString(sessionID);
        return verifySessionID(hostKey, signature, sessionIDB64);
    }

    static {
        System.loadLibrary("sshwire");
    }
}
