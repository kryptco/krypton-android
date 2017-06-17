package co.krypt.kryptonite.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class IssuerSubpacket extends Subpacket {
    public final SubpacketHeader header;
    public final long issuerKeyID;

    public IssuerSubpacket(SubpacketHeader header, long issuerKeyID) {
        this.header = header;
        this.issuerKeyID = issuerKeyID;
    }

    public static IssuerSubpacket parse(SubpacketHeader header, DataInputStream in) throws IOException {
        return new IssuerSubpacket(header, in.readLong());
    }

    public static IssuerSubpacket fromIssuerKeyID(long issuerKeyID) {
        return new IssuerSubpacket(
                SubpacketHeader.fromLengthAndTypeIgnorable(8, SubpacketType.ISSUER),
                issuerKeyID
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.writeLong(issuerKeyID);
    }
}
