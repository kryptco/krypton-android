package co.krypt.krypton.pgp.packet;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnsupportedHeaderLineException extends PGPException {
    UnsupportedHeaderLineException(String message) {
        super(message);
    }
    UnsupportedHeaderLineException(String message, Throwable e) {
        super(message, e);
    }
    public UnsupportedHeaderLineException() {
        super();
    }
}
