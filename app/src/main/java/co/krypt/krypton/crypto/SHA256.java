package co.krypt.krypton.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 12/1/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SHA256 {
    public static byte[] digest(byte[] data) throws CryptoException {
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        }
        return sha256.digest(data);
    }

    //  https://golang.org/src/crypto/rsa/pkcs1v15.go
    private static final byte[] PKCS1_PADDING = new byte[]{0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte)/* XXX fails to compile without this cast */ 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20};

    public static byte[] digestPrependingOID(byte[] data) throws CryptoException {
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        }

        ByteArrayOutputStream oidWithDigest = new ByteArrayOutputStream();
        try {
            oidWithDigest.write(PKCS1_PADDING);
            oidWithDigest.write(sha256.digest(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oidWithDigest.toByteArray();
    }
}
