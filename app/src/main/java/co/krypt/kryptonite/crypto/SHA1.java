package co.krypt.kryptonite.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 12/1/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SHA1 {
    //  https://golang.org/src/crypto/rsa/pkcs1v15.go
    private static final byte[] PKCS1_PADDING = new byte[]{0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    public static byte[] digestPrependingOID(byte[] data) throws CryptoException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        }

        ByteArrayOutputStream oidWithDigest = new ByteArrayOutputStream();
        try {
            oidWithDigest.write(PKCS1_PADDING);
            oidWithDigest.write(sha1.digest(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oidWithDigest.toByteArray();
    }

    public static byte[] digest(byte[] data) throws CryptoException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        }
        return sha1.digest(data);
    }
}
