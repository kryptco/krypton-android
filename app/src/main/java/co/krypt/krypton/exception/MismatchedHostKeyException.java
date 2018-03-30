package co.krypt.krypton.exception;

import java.util.List;

/**
 * Created by Kevin King on 5/8/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class MismatchedHostKeyException extends Exception {
    public final List<String> pinnedPublicKeys;
    public MismatchedHostKeyException(List<String> pinnedPublicKeys, String message) {
        super(message);
        this.pinnedPublicKeys = pinnedPublicKeys;
    }
}
