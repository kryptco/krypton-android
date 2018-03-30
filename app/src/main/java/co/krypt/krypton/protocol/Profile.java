package co.krypt.krypton.protocol;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import javax.annotation.Nullable;

import co.krypt.krypton.ssh.Wire;
import co.krypt.krypton.team.Sigchain;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Profile {
    @SerializedName("email")
    public String email;
    @SerializedName("public_key_wire")
    public byte[] sshWirePublicKey;
    @SerializedName("pgp_pk")
    @Nullable
    public byte[] pgpPublicKey;

    @SerializedName("team_checkpoint")
    @Nullable
    public Sigchain.TeamCheckpoint teamCheckpoint;

    public Profile() { }

    public Profile(String email, byte[] sshWirePublicKey, @Nullable byte[] pgpPublicKey, @Nullable Sigchain.TeamCheckpoint teamCheckpoint) {
        this.email = email;
        this.sshWirePublicKey = sshWirePublicKey;
        this.pgpPublicKey = pgpPublicKey;
        this.teamCheckpoint = teamCheckpoint;
    }

    public String authorizedKeysFormat() {
        if (sshWirePublicKey == null) {
            return "";
        }
        try {
            return Wire.publicKeyBytesToAuthorizedKeysFormat(sshWirePublicKey) + " " + email;
        } catch (IOException e) {
            e.printStackTrace();
            return "failed to export public key";
        }
    }

    public String shareText() {
        return "This is my SSH public key:\n\n" + authorizedKeysFormat() + "\n\nStore your SSH key with Krypton! https://krypt.co";
    }
}
