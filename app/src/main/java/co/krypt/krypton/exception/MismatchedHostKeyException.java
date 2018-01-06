package co.krypt.krypton.exception;

/**
 * Created by Kevin King on 5/8/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class MismatchedHostKeyException extends Exception {
    public MismatchedHostKeyException(String message) {
        super(message);
    }
}
