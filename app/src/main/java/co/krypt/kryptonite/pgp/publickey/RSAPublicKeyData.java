package co.krypt.kryptonite.pgp.publickey;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import co.krypt.kryptonite.pgp.packet.MPInt;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class RSAPublicKeyData extends PublicKeyData {
    final MPInt n;
    final MPInt e;

    public RSAPublicKeyData(MPInt n, MPInt e) {
        this.n = n;
        this.e = e;
    }

    public static RSAPublicKeyData parse(DataInputStream in) throws IOException {
        return new RSAPublicKeyData(
                MPInt.parse(in),
                MPInt.parse(in)
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        n.serialize(out);
        e.serialize(out);
    }
}
