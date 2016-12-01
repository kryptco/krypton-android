package co.krypt.kryptonite;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Kevin King on 12/1/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SHA256 {
    public static final int BLOCK_SIZE = 32;
    public static byte[] digest(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(data);
    }
}
