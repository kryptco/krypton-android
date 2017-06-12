package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UnparsedPacket {
    public final PacketHeader header;
    public final DataInputStream body;

    private UnparsedPacket(PacketHeader header, DataInputStream body) {
        this.header = header;
        this.body = body;
    }

    public UnparsedPacket parseHeader(DataInputStream in) throws UnsupportedNewFormatException, UnsupportedOldPacketLengthTypeException, InvalidPacketTagException, IOException {
        return new UnparsedPacket(
                PacketHeader.parse(in),
                in
        );
    }

}
