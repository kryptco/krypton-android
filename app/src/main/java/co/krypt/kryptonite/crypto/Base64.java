package co.krypt.kryptonite.crypto;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class Base64 {
    public static byte[] decode(String s) throws CryptoException {
        try {
            return com.amazonaws.util.Base64.decode(s);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(e.getMessage());
        }
    }
    public static String encode(byte[] b) {
        return com.amazonaws.util.Base64.encodeAsString(b);
    }
}
