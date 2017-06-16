package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public enum SignatureType {
    UNKNOWN(-1),
    BINARY(0x00),
    USER_ID(0x10),
    POSITIVE_USER_ID(0x13),
    SUBKEY(0x18),
    PRIMARY_KEY(0x19);

    private final int v;
    SignatureType(int v) {
        this.v = v;
    }

    public static SignatureType parse(DataInputStream in) throws IOException {
        byte b = in.readByte();
        switch (b) {
            case 0x00:
                return BINARY;
            case 0x10:
                return USER_ID;
            case 0x13:
                return POSITIVE_USER_ID;
            case 0x18:
                return SUBKEY;
            case 0x19:
                return PRIMARY_KEY;
        }
        return UNKNOWN;
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(v);
    }

    public int serializedByteLength() {
        return 1;
    }
}
