package co.krypt.kryptonite.pgp.packet;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class PacketTag extends Serializable {
    public static final byte LEADING_ONE = (byte) 0b10000000;
    public static final byte NEW_FORMAT = (byte) 0b01000000;
    private static final byte OLD_LENGTH_TYPE_MASK = (byte) 0b00000011;

    public final byte tag;
    public final OldPacketLengthType lengthType;
    public final PacketType packetType;

    public PacketTag(byte tag, OldPacketLengthType lengthType, PacketType packetType) {
        this.tag = tag;
        this.lengthType = lengthType;
        this.packetType = packetType;
    }

    public static PacketTag parse(byte tag) throws InvalidPacketTagException, UnsupportedNewFormatException, UnsupportedOldPacketLengthTypeException {
        if ((tag & LEADING_ONE) != LEADING_ONE) {
            throw new InvalidPacketTagException();
        }
        if ((tag & NEW_FORMAT) == NEW_FORMAT) {
            throw new UnsupportedNewFormatException();
        }
        return new PacketTag(
                tag,
                OldPacketLengthType.valueOf(tag & OLD_LENGTH_TYPE_MASK),
                PacketType.fromTag(tag)
        );
    }

    public static PacketTag oldWithTypeAndLength(PacketType type, long length) {
        OldPacketLengthType lengthType = OldPacketLengthType.fromLength((int) length);
        return new PacketTag(
                (byte) (LEADING_ONE | type.toTagFlags() | lengthType.getValue()),
                lengthType,
                type
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.write(tag);
    }
}
