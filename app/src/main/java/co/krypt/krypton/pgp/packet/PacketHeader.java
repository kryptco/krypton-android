package co.krypt.krypton.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class PacketHeader extends Serializable {
    public final PacketTag tag;
    public final PacketLength length;

    public PacketHeader(PacketTag tag, long length) {
        this.tag = tag;
        this.length = new PacketLength(tag.lengthType, length);
    }

    public static PacketHeader parse(DataInputStream in) throws IOException, UnsupportedOldPacketLengthTypeException, InvalidPacketTagException, UnsupportedNewFormatException {
        PacketTag tag = PacketTag.parse(in.readByte());


        byte[] lengthBuffer = new byte[4];
        in.readFully(lengthBuffer, 4 - tag.lengthType.lengthLength(), tag.lengthType.lengthLength());

        return new PacketHeader(
                tag,
                java.nio.ByteBuffer.wrap(lengthBuffer).getInt()
        );
    }

    public static PacketHeader withTypeAndLength(PacketType type, long length) {
        return new PacketHeader(
                PacketTag.oldWithTypeAndLength(type, length),
                length
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        tag.serialize(out);
        length.serialize(out);
    }
}
