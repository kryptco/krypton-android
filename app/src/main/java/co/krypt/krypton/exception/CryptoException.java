package co.krypt.krypton.exception;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class CryptoException extends Unrecoverable {
    public CryptoException(String message) {
        super(message);
    }
    public CryptoException(Throwable t) {
        super(t);
    }
}
