package co.krypt.krypton.pgp.packet;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnsupportedHashAlgorithmException extends PGPException {
    UnsupportedHashAlgorithmException(String message) {
        super(message);
    }
    public UnsupportedHashAlgorithmException() {
        super();
    }
}
