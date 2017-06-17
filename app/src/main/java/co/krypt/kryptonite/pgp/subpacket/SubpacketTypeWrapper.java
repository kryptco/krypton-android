package co.krypt.kryptonite.pgp.subpacket;

import java.io.DataOutputStream;
import java.io.IOException;

import co.krypt.kryptonite.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

//  stores whether a type is marked as critical
public class SubpacketTypeWrapper extends Serializable {
    static final byte CRITICAL_BIT = 0b01000000;

    public final SubpacketType type;
    public final boolean critical;

    public SubpacketTypeWrapper(SubpacketType type, boolean critical) {
        this.type = type;
        this.critical = critical;
    }

    public static SubpacketTypeWrapper critical(SubpacketType type) {
        return new SubpacketTypeWrapper(type, true);
    }

    public static SubpacketTypeWrapper ignorable(SubpacketType type) {
        return new SubpacketTypeWrapper(type, false);
    }

    public static SubpacketTypeWrapper parse(byte type) throws IOException {
        return new SubpacketTypeWrapper(
                SubpacketType.parse((byte) (type & ~CRITICAL_BIT)),
                (type & CRITICAL_BIT) == CRITICAL_BIT
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        byte b = (byte) type.v;
        if (critical) {
            b |= CRITICAL_BIT;
        }
        out.writeByte(b);
    }
}
