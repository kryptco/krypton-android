package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Profile {
    @SerializedName("email")
    public String email;
    @SerializedName("rsa_public_key_wire")
    public byte[] sshWirePublicKey;

    public Profile() { }

    public Profile(String email, byte[] sshWirePublicKey) {
        this.email = email;
        this.sshWirePublicKey = sshWirePublicKey;
    }
}
