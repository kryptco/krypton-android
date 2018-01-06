package co.krypt.krypton.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyExpirationSubpacket extends Subpacket {
    final SubpacketHeader header;
    final long seconds;

    public KeyExpirationSubpacket(SubpacketHeader header, long seconds) {
        this.header = header;
        this.seconds = seconds;
    }

    public static KeyExpirationSubpacket parse(SubpacketHeader header, DataInputStream in) throws IOException {
        return new KeyExpirationSubpacket(header, in.readInt());
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.writeInt((int) seconds);
    }
}
