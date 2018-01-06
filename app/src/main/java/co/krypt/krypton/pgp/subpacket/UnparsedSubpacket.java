package co.krypt.krypton.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class UnparsedSubpacket extends Subpacket {
    final SubpacketHeader header;
    final byte[] body;

    public UnparsedSubpacket(SubpacketHeader header, byte[] body) {
        this.header = header;
        this.body = body;
    }

    public static UnparsedSubpacket parse(SubpacketHeader header, DataInputStream in) throws IOException {
        byte[] body = new byte[(int) header.length.bodyLength()];
        in.readFully(body);
        return new UnparsedSubpacket(header, body);
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.write(body);
    }
}
