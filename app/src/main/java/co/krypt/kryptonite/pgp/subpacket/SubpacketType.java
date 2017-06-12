package co.krypt.kryptonite.pgp.subpacket;

import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public enum SubpacketType {
    UNKNOWN(-1),
    CREATED(2),
    KEY_EXPIRES(9),
    ISSUER(16),
    KEY_FLAGS(27);

    public final int v;
    SubpacketType(int v) {
        this.v = v;
    }

    public static SubpacketType parse(byte type) throws IOException {
        switch (type) {
            case 2:
                return CREATED;
            case 9:
                return KEY_EXPIRES;
            case 16:
                return ISSUER;
            case 27:
                return KEY_FLAGS;
        }
        return UNKNOWN;
    }
}
