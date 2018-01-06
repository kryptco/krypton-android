package co.krypt.krypton.pgp.packet;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-4.2.1
//  Old format only supported
public class PacketLength extends Serializable {
    final OldPacketLengthType lengthType;
    public final long bodyLength;

    public int lengthLength() {
        return lengthType.lengthLength();
    }

    public PacketLength(OldPacketLengthType lengthType, long bodyLength) {
        this.lengthType = lengthType;
        this.bodyLength = bodyLength;
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        switch (lengthType) {
            case ONE_OCTET:
                out.writeByte((byte) bodyLength);
                break;
            case TWO_OCTET:
                out.writeShort((int) bodyLength);
                break;
            case FOUR_OCTET:
                out.writeInt((int) bodyLength);
                break;
        }
    }
}
