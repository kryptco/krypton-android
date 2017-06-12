package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class MPInt extends Serializable {
    public final int bitLength;
    public final byte[] body;

    public MPInt(int bitLength, byte[] body) {
        this.bitLength = bitLength;
        this.body = body;
    }

    public static MPInt parse(DataInputStream in) throws IOException {
        int bitLength = in.readUnsignedShort();
        int byteLength = (bitLength + 7) / 8;
        byte[] body = new byte[byteLength];
        in.readFully(body);
        return new MPInt(
                bitLength,
                body
        );
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeShort(bitLength);
        out.write(body);
    }
}
