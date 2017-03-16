package co.krypt.kryptonite.protocol;

import com.amazonaws.util.Base64;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Profile {
    @SerializedName("email")
    public String email;
    @SerializedName("public_key_wire")
    public byte[] sshWirePublicKey;

    public Profile() { }

    public Profile(String email, byte[] sshWirePublicKey) {
        this.email = email;
        this.sshWirePublicKey = sshWirePublicKey;
    }

    public String authorizedKeysFormat() {
        if (sshWirePublicKey == null) {
            return "";
        }
        return "ssh-rsa " + Base64.encodeAsString(sshWirePublicKey) + " " + email;
    }

    public String shareText() {
        return "This is my SSH public key:\n\n" + authorizedKeysFormat() + "\n\nStore your SSH key with Kryptonite! https://krypt.co";
    }
}
