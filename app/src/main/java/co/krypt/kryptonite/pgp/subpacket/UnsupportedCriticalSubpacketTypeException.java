package co.krypt.kryptonite.pgp.subpacket;

import co.krypt.kryptonite.pgp.PGPException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnsupportedCriticalSubpacketTypeException extends PGPException {
    UnsupportedCriticalSubpacketTypeException(String message) {
        super(message);
    }
    UnsupportedCriticalSubpacketTypeException() {
        super();
    }
}
