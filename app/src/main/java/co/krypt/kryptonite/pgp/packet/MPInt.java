package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class MPInt extends Serializable {
    public final int bitLength;
    public final byte[] body;

    private MPInt(int bitLength, byte[] body) {
        this.bitLength = bitLength;
        this.body = body;
    }

    public MPInt(byte[] body) {
        for (int i = 0; i < body.length; i++) {
            for (int b = 7; b >= 0; b--) {
                if ((body[i] & (1 << b)) == (1 << b)) {
                    this.bitLength = (b+1) + (body.length - (i + 1)) * 8;
                    this.body = Arrays.copyOfRange(body, i, body.length);
                    return;
                }
            }
        }
        this.bitLength = 0;
        this.body = new byte[]{};
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
