package co.krypt.krypton.exception;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnhandledKeyTypeException extends IllegalStateException {
    public UnhandledKeyTypeException(String message) {
        super(message);
    }
}
