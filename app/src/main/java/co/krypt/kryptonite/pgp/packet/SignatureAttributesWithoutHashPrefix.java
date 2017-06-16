package co.krypt.kryptonite.pgp.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import co.krypt.kryptonite.pgp.publickey.PublicKeyAlgorithm;
import co.krypt.kryptonite.pgp.publickey.UnsupportedPublicKeyAlgorithmException;
import co.krypt.kryptonite.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.kryptonite.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.kryptonite.pgp.subpacket.SubpacketList;
import co.krypt.kryptonite.pgp.subpacket.UnsupportedCriticalSubpacketTypeException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SignatureAttributesWithoutHashPrefix extends Serializable implements Signable {
    public static final byte VERSION = 4;
    public final PacketHeader header;
    public final SignatureType type;
    public final PublicKeyAlgorithm pkAlgorithm;
    public final HashAlgorithm hashAlgorithm;
    public final SubpacketList hashedSubpackets;
    public final SubpacketList unhashedSubpackets;

    public SignatureAttributesWithoutHashPrefix(PacketHeader header, SignatureType type, PublicKeyAlgorithm pkAlgorithm, HashAlgorithm hashAlgorithm, SubpacketList hashedSubpackets, SubpacketList unhashedSubpackets) throws IOException, NoSuchAlgorithmException {
        this.header = header;
        this.type = type;
        this.pkAlgorithm = pkAlgorithm;
        this.hashAlgorithm = hashAlgorithm;
        this.hashedSubpackets = hashedSubpackets;
        this.unhashedSubpackets = unhashedSubpackets;
    }

    public static SignatureAttributesWithoutHashPrefix parse(PacketHeader header, DataInputStream in) throws UnsupportedSignatureVersionException, IOException, UnsupportedPublicKeyAlgorithmException, InvalidSubpacketLengthException, UnsupportedCriticalSubpacketTypeException, UnsupportedHashAlgorithmException, NoSuchAlgorithmException, DuplicateSubpacketException {
        if (in.readByte() != VERSION) {
            throw new UnsupportedSignatureVersionException();
        }
        return new SignatureAttributesWithoutHashPrefix(
                header,
                SignatureType.parse(in),
                PublicKeyAlgorithm.parse(in.readByte()),
                HashAlgorithm.parse(in.readByte()),
                SubpacketList.parse(in),
                SubpacketList.parse(in)
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.writeByte(VERSION);
        type.serialize(out);
        pkAlgorithm.serialize(out);
        hashAlgorithm.serialize(out);
        hashedSubpackets.serialize(out);
        unhashedSubpackets.serialize(out);
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream buf = new DataOutputStream(outBuf);

        buf.writeByte(VERSION);
        type.serialize(buf);
        pkAlgorithm.serialize(buf);
        hashAlgorithm.serialize(buf);
        hashedSubpackets.serialize(buf);

        buf.close();

        byte[] preTrailerData = outBuf.toByteArray();

        out.write(preTrailerData);
        out.writeByte(VERSION);
        out.writeByte(0xFF);
        out.writeInt(preTrailerData.length);
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }
}
