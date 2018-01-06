package co.krypt.krypton.pgp.subpacket;

import co.krypt.krypton.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class DuplicateSubpacketException extends PGPException {
    DuplicateSubpacketException(String message) {
        super(message);
    }
    DuplicateSubpacketException() {
        super();
    }
}
