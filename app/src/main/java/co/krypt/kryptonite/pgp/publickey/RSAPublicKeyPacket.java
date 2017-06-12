package co.krypt.kryptonite.pgp.publickey;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import co.krypt.kryptonite.crypto.SHA1;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.packet.MPInt;
import co.krypt.kryptonite.pgp.packet.PacketHeader;
import co.krypt.kryptonite.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class RSAPublicKeyPacket extends Serializable {
    public final PacketHeader header;
    public final PublicKeyPacketAttributes attributes;
    final MPInt n;
    final MPInt e;

    public RSAPublicKeyPacket(PacketHeader header, PublicKeyPacketAttributes attributes, MPInt n, MPInt e) {
        this.header = header;
        this.attributes = attributes;
        this.n = n;
        this.e = e;
    }

    public static RSAPublicKeyPacket parse(PacketHeader header, PublicKeyPacketAttributes attributes, DataInputStream in) throws IOException {
        return new RSAPublicKeyPacket(
                header,
                attributes,
                MPInt.parse(in),
                MPInt.parse(in)
        );
    }

    public byte[] fingerprint() throws IOException, CryptoException {
        ByteArrayOutputStream toHashBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(toHashBuffer);
        out.writeByte(0x99);
        out.writeShort((short) serializedByteLength());
        serialize(out);
        out.close();
        return SHA1.digest(toHashBuffer.toByteArray());
    }

    public long keyID() throws IOException, CryptoException {
        byte[] fingerprint = this.fingerprint();
        return ByteBuffer.wrap(
                Arrays.copyOfRange(fingerprint, fingerprint.length - 8, fingerprint.length))
                .getLong();
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        attributes.serialize(out);
        n.serialize(out);
        e.serialize(out);
    }
}
