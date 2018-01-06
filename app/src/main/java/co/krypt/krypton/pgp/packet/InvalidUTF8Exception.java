package co.krypt.krypton.pgp.packet;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class InvalidUTF8Exception extends PGPException {
    InvalidUTF8Exception(String message) {
        super(message);
    }
    public InvalidUTF8Exception(String message, Throwable e) {
        super(message, e);
    }
    InvalidUTF8Exception() {
        super();
    }
}
