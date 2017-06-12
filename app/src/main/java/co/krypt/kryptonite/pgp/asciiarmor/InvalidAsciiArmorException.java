package co.krypt.kryptonite.pgp.asciiarmor;

import co.krypt.kryptonite.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class InvalidAsciiArmorException extends PGPException {
    InvalidAsciiArmorException(String message) {
        super(message);
    }
    InvalidAsciiArmorException(String message, Throwable e) {
        super(message, e);
    }
    InvalidAsciiArmorException() {
        super();
    }
}
