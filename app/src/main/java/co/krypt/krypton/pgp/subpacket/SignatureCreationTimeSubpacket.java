package co.krypt.krypton.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SignatureCreationTimeSubpacket extends Subpacket {
    public final SubpacketHeader header;
    public final long created;

    public SignatureCreationTimeSubpacket(SubpacketHeader header, long created) {
        this.header = header;
        this.created = created;
    }

    public static SignatureCreationTimeSubpacket parse(SubpacketHeader header, DataInputStream in) throws IOException {
        return new SignatureCreationTimeSubpacket(header, in.readInt());
    }

    public static SignatureCreationTimeSubpacket fromTime(long created) {
        return new SignatureCreationTimeSubpacket(
                SubpacketHeader.fromLengthAndTypeIgnorable(4, SubpacketType.CREATED),
                created
        );
    }

    public static SignatureCreationTimeSubpacket now() {
        return fromTime(System.currentTimeMillis() / 1000);
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.writeInt((int) created);
    }
}
