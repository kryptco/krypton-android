package co.krypt.krypton.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/14/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class Ed25519Signature extends Signature {
    final MPInt r;
    final MPInt s;

    public Ed25519Signature(MPInt r, MPInt s) {
        this.r = r;
        this.s = s;
    }

    public static Ed25519Signature parse(DataInputStream in) throws IOException {
        return new Ed25519Signature(
                MPInt.parse(in),
                MPInt.parse(in)
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        r.serialize(out);
        s.serialize(out);
    }
}
