package co.krypt.kryptonite.pgp.subpacket;

import co.krypt.kryptonite.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class InvalidSubpacketLengthException extends PGPException {
    InvalidSubpacketLengthException(String message) {
        super(message);
    }
    InvalidSubpacketLengthException() {
        super();
    }
}
