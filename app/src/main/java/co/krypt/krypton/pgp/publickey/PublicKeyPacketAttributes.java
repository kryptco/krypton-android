package co.krypt.krypton.pgp.publickey;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import co.krypt.krypton.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class PublicKeyPacketAttributes extends Serializable {
    static final byte VERSION = 4;
    public final long created;
    public final PublicKeyAlgorithm algorithm;

    public PublicKeyPacketAttributes(long created, PublicKeyAlgorithm algorithm) {
        this.created = created;
        this.algorithm = algorithm;
    }

    public static PublicKeyPacketAttributes parse(DataInputStream in) throws IOException, UnsupportedPublicKeyVersionException, UnsupportedPublicKeyAlgorithmException {
        if (in.readByte() != VERSION) {
            throw new UnsupportedPublicKeyVersionException();
        }
        return new PublicKeyPacketAttributes(
                in.readInt(),
                PublicKeyAlgorithm.parse(in.readByte())
        );
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(VERSION);
        out.writeInt((int) created);
        algorithm.serialize(out);
    }
}
