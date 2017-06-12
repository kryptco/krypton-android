package co.krypt.kryptonite.pgp.packet;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-4.2.1
public enum OldPacketLengthType {
    ONE_OCTET(0),
    TWO_OCTET(1),
    FOUR_OCTET(2);
    //  Indeterminate length unsupported
    private final int type;

    OldPacketLengthType(int type) {
        this.type = type;
    }

    public int getValue() {
        return type;
    }

    public int lengthLength() {
        switch(this) {
            case ONE_OCTET:
                return 1;
            case TWO_OCTET:
                return 2;
            case FOUR_OCTET:
                return 4;
        }
        throw new IllegalArgumentException();
    }

    public static OldPacketLengthType valueOf(int type) throws UnsupportedOldPacketLengthTypeException {
        switch (type) {
            case 0:
                return ONE_OCTET;
            case 1:
                return TWO_OCTET;
            case 2:
                return FOUR_OCTET;
        }
        throw new UnsupportedOldPacketLengthTypeException();
    }

    public static OldPacketLengthType fromLength(int length) {
        if (length <= 0xff) {
            return ONE_OCTET;
        }
        if (length <= 0xffff) {
            return TWO_OCTET;
        }
        return FOUR_OCTET;
    }
}
