package co.krypt.krypton.pgp;

import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.exception.Unrecoverable;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class PGPException extends Unrecoverable {
    public PGPException(String message) {
        super(message);
    }
    public PGPException(String message, Throwable e) {
        super(message, e);
    }
    public PGPException() { }
}
