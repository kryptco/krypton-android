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

public class SHA512 {
    //  https://golang.org/src/crypto/rsa/pkcs1v15.go
    private static final byte[] PKCS1_PADDING = new byte[]{0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte)/* XXX fails to compile without this cast */ 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x05, 0x00, 0x04, 0x40};

    public static byte[] digestPrependingOID(byte[] data) throws CryptoException {
        MessageDigest sha512 = null;
        try {
            sha512 = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        }

        ByteArrayOutputStream oidWithDigest = new ByteArrayOutputStream();
        try {
            oidWithDigest.write(PKCS1_PADDING);
            oidWithDigest.write(sha512.digest(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oidWithDigest.toByteArray();
    }
}
