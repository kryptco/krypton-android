package co.krypt.krypton.pgp.publickey;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class InvalidEd25519PublicKeyFormatException extends PGPException {
    public InvalidEd25519PublicKeyFormatException(String message) {
        super(message);
    }
    InvalidEd25519PublicKeyFormatException() {
        super();
    }
}
