/*
 * Copyright (c) 2018. KryptCo, Inc.
 */

package co.krypt.krypton.protocol;

import com.google.gson.annotations.SerializedName;

public class U2FAuthenticateResponse {
    @SerializedName("public_key")
    public byte[] publicKey;

    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("counter")
    public long counter;

    public U2FAuthenticateResponse(byte[] publicKey, byte[] signature, long counter) {
        this.publicKey = publicKey;
        this.signature = signature;
        this.counter = counter;
    }
}
