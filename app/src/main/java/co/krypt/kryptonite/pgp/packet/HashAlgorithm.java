package co.krypt.kryptonite.pgp.packet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public enum HashAlgorithm {
    MD5(1),
    SHA1(2),
    RIPE_MD160(3),
    SHA256(8),
    SHA384(9),
    SHA512(10),
    SHA224(11),
    ;

    private final byte v;

    HashAlgorithm(int v) {
        this.v = (byte) v;
    }

    public static HashAlgorithm parse(byte type) throws UnsupportedHashAlgorithmException {
        switch (type) {
            case 1:
                return MD5;
            case 2:
                return SHA1;
            case 3:
                return RIPE_MD160;
            case 8:
                return SHA256;
            case 9:
                return SHA384;
            case 10:
                return SHA512;
            case 11:
                return SHA224;
        }
        throw new UnsupportedHashAlgorithmException();
    }

    public MessageDigest digest() throws NoSuchAlgorithmException {
        switch (this) {
            case MD5:
                return MessageDigest.getInstance("MD5");
            case SHA1:
                return MessageDigest.getInstance("SHA-1");
            case RIPE_MD160:
                throw new NoSuchAlgorithmException();
            case SHA256:
                return MessageDigest.getInstance("SHA-256");
            case SHA384:
                return MessageDigest.getInstance("SHA-384");
            case SHA512:
                return MessageDigest.getInstance("SHA-512");
            case SHA224:
                return MessageDigest.getInstance("SHA-224");
        }
        throw new NoSuchAlgorithmException();
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(v);
    }
}
