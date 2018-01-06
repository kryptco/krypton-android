package co.krypt.krypton.pgp.packet;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnsupportedSignatureVersionException extends PGPException {
    UnsupportedSignatureVersionException(String message) {
        super(message);
    }
    UnsupportedSignatureVersionException() {
        super();
    }
}
