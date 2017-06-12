package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/14/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class RSASignature extends Signature {
    final MPInt s;

    public RSASignature(MPInt s) {
        this.s = s;
    }

    public static RSASignature parse(DataInputStream in) throws IOException {
        return new RSASignature(MPInt.parse(in));
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        s.serialize(out);
    }
}
