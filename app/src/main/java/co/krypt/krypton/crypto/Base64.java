package co.krypt.krypton.crypto;

import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class Base64 {
    public static byte[] decode(String s) throws CryptoException {
        try {
            return com.google.crypto.tink.subtle.Base64.decode(s);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(e.getMessage());
        }
    }
    public static byte[] decodeURLSafe(String s) throws CryptoException {
        try {
            return com.google.crypto.tink.subtle.Base64.urlSafeDecode(s);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(e.getMessage());
        }
    }
    public static String encode(byte[] b) {
        return com.google.crypto.tink.subtle.Base64.encode(b);
    }
    public static String encodeURLSafe(byte[] b) {
        return com.google.crypto.tink.subtle.Base64.urlSafeEncode(b);
    }
}
