package co.krypt.krypton.pgp;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class PGPException extends Exception {
    public PGPException(String message) {
        super(message);
    }
    public PGPException(String message, Throwable e) {
        super(message, e);
    }
    public PGPException() { }
}
