package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignRequest {
    @SerializedName("digest")
    byte[] Digest;

    @SerializedName("public_key_fingerprint")
    byte[] PublicKeyFingerprint;

    @SerializedName("command")
    String command;
}
