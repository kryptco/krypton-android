package co.krypt.kryptonite.pgp.packet;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-4.2.1
//  Old format only supported
public class PacketLength {
    final OldPacketLengthType lengthType;
    final int bodyLength;

    public int lengthLength() {
        return lengthType.lengthLength();
    }

    public PacketLength(OldPacketLengthType lengthType, int bodyLength) {
        this.lengthType = lengthType;
        this.bodyLength = bodyLength;
    }
}
