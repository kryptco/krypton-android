package co.krypt.krypton.pgp.publickey;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public enum PublicKeyAlgorithm {
    RSA_ENCRYPT_OR_SIGN(1),
    RSA_ENCRYPT_ONLY(2),
    RSA_SIGN_ONLY(3),
    ED25519(22);

    private final byte v;

    PublicKeyAlgorithm(int v) {
        this.v = (byte) v;
    }

    public static PublicKeyAlgorithm parse(byte type) throws UnsupportedPublicKeyAlgorithmException {
        switch (type) {
            case 1:
                return RSA_ENCRYPT_OR_SIGN;
            case 2:
                return RSA_ENCRYPT_ONLY;
            case 3:
                return RSA_SIGN_ONLY;
            case 22:
                return ED25519;
        }
        throw new UnsupportedPublicKeyAlgorithmException(String.valueOf(type));
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(v);
    }

    public int serializedByteLength() {
        return 1;
    }
}
