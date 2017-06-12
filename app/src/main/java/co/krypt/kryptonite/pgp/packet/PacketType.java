package co.krypt.kryptonite.pgp.packet;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-4.3
public enum PacketType {
    SIGNATURE(2),
    SECRET_KEY(5),
    PUBLIC_KEY(6),
    USER_ID(13),
    PUBLIC_SUBKEY(14),
    UNKNOWN(-1)
    ;

    private static final byte OLD_FORMAT_TAG_MASK = 0b1111;

    //  Indeterminate length unsupported
    private final int type;

    PacketType(int type) {
        this.type = type;
    }

    public int getValue() {
        return type;
    }

    public static PacketType fromTag(byte packetTag) {
        switch ((packetTag >> 2) & OLD_FORMAT_TAG_MASK) {
            case 2: return SIGNATURE;
            case 5: return SECRET_KEY;
            case 6: return PUBLIC_KEY;
            case 13: return USER_ID;
            case 14: return PUBLIC_SUBKEY;
        }
        return UNKNOWN;
    }

    public int toTagFlags() {
        return getValue() << 2;
    }
}
