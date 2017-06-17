package co.krypt.kryptonite.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import co.krypt.kryptonite.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SubpacketHeader extends Serializable {
    public final SubpacketLength length;
    public final SubpacketTypeWrapper type;

    public SubpacketHeader(SubpacketLength length, SubpacketTypeWrapper type) {
        this.length = length;
        this.type = type;
    }

    static SubpacketHeader parse(DataInputStream in) throws IOException, InvalidSubpacketLengthException {
        return new SubpacketHeader(
                SubpacketLength.parse(in),
                SubpacketTypeWrapper.parse(in.readByte())
        );
    }

    static SubpacketHeader fromLengthAndTypeCritical(long bodyLength, SubpacketType type) {
        return new SubpacketHeader(
                SubpacketLength.fromBodyLength(bodyLength),
                SubpacketTypeWrapper.critical(type)
        );
    }

    static SubpacketHeader fromLengthAndTypeIgnorable(long bodyLength, SubpacketType type) {
        return new SubpacketHeader(
                SubpacketLength.fromBodyLength(bodyLength),
                SubpacketTypeWrapper.ignorable(type)
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        length.serialize(out);
        type.serialize(out);
    }
}
