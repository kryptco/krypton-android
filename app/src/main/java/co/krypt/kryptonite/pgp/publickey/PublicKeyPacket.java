package co.krypt.kryptonite.pgp.publickey;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import co.krypt.kryptonite.crypto.SHA1;
import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.packet.PacketHeader;
import co.krypt.kryptonite.pgp.packet.PacketType;
import co.krypt.kryptonite.pgp.packet.Serializable;
import co.krypt.kryptonite.pgp.packet.Signable;
import co.krypt.kryptonite.pgp.packet.SignableUtils;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class PublicKeyPacket extends Serializable implements Signable {
    public final PacketHeader header;
    public final PublicKeyPacketAttributes attributes;
    final PublicKeyData publicKeyData;

    public PublicKeyPacket(PacketHeader header, PublicKeyPacketAttributes attributes, PublicKeyData publicKeyData) {
        this.header = header;
        this.attributes = attributes;
        this.publicKeyData = publicKeyData;
    }

    public static PublicKeyPacket parse(PacketHeader header, DataInputStream in) throws IOException, InvalidEd25519PublicKeyFormatException, UnsupportedPublicKeyVersionException, UnsupportedPublicKeyAlgorithmException {
        PublicKeyPacketAttributes attributes = PublicKeyPacketAttributes.parse(in);
        switch(attributes.algorithm) {
            case RSA_ENCRYPT_OR_SIGN:
                return new PublicKeyPacket(
                        header,
                        attributes,
                        RSAPublicKeyData.parse(in)
                );
            case RSA_ENCRYPT_ONLY:
                return new PublicKeyPacket(
                        header,
                        attributes,
                        RSAPublicKeyData.parse(in)
                );
            case RSA_SIGN_ONLY:
                return new PublicKeyPacket(
                        header,
                        attributes,
                        RSAPublicKeyData.parse(in)
                );
            case ED25519:
                return new PublicKeyPacket(
                        header,
                        attributes,
                        Ed25519PublicKeyData.parse(in)
                );
        }
        throw new IllegalStateException();
    }

    public byte[] fingerprint() throws IOException, CryptoException {
        ByteArrayOutputStream toHashBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(toHashBuffer);
        out.writeByte(0x99);
        SignableUtils.writeLengthAndSignableData(this, out);
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
        header.serialize(out);
        attributes.serialize(out);
        publicKeyData.serialize(out);
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        attributes.serialize(out);
        publicKeyData.serialize(out);
    }

    public static PublicKeyPacket fromKeyPair(SSHKeyPairI kp) throws IOException {
        PublicKeyPacketAttributes attributes = kp.pgpPublicKeyPacketAttributes();
        PublicKeyData data = kp.pgpPublicKeyData();
        long length = attributes.serializedByteLength() + data.serializedByteLength();
        return new PublicKeyPacket(
                PacketHeader.withTypeAndLength(PacketType.PUBLIC_KEY, length),
                attributes,
                data
        );
    }
}
