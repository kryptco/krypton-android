package co.krypt.krypton.exception;

/**
 * Created by Kevin King on 3/15/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Unrecoverable extends Exception {
    public Unrecoverable(String message) {super(message);}
    public Unrecoverable(Throwable t) {super(t);}
    public Unrecoverable(String message, Throwable e) { super(message, e); }
    public Unrecoverable() {}
}
