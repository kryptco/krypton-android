/*
 * Copyright (c) 2018. KryptCo, Inc.
 */

package co.krypt.krypton.protocol;

import com.google.gson.annotations.SerializedName;

public class U2FRegisterResponse {
    @SerializedName("public_key")
    public byte[] publicKey;

    @SerializedName("attestation_certificate")
    public byte[] attestationCertificate;

    @SerializedName("key_handle")
    public byte[] keyHandle;

    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("error")
    public String error;

    public U2FRegisterResponse(byte[] publicKey, byte[] attestationCertificate, byte[] keyHandle, byte[] signature) {
        this.publicKey = publicKey;
        this.attestationCertificate = attestationCertificate;
        this.keyHandle = keyHandle;
        this.signature = signature;
    }
}
