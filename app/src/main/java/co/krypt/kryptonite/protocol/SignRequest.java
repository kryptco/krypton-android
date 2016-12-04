package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignRequest {
    @SerializedName("digest")
    public byte[] digest;

    @SerializedName("public_key_fingerprint")
    public byte[] publicKeyFingerprint;

    @SerializedName("command")
    public String command;
}
