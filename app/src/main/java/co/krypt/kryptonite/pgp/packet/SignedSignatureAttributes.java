package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import co.krypt.kryptonite.pgp.publickey.UnsupportedPublicKeyAlgorithmException;
import co.krypt.kryptonite.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.kryptonite.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.kryptonite.pgp.subpacket.UnsupportedCriticalSubpacketTypeException;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SignedSignatureAttributes extends Serializable {
    final PacketHeader header;
    public final SignatureAttributes attributes;
    public final Signature signature;

    public SignedSignatureAttributes(PacketHeader header, SignatureAttributes attributes, Signature signature) {
        this.header = header;
        this.attributes = attributes;
        this.signature = signature;
    }

    public SignedSignatureAttributes(SignatureAttributes attributes, Signature signature) throws IOException {
        this.attributes = attributes;
        this.signature = signature;

        this.header = PacketHeader.withTypeAndLength(
                PacketType.SIGNATURE,
                attributes.serializedByteLength() + signature.serializedByteLength()
                );
    }

    public static SignedSignatureAttributes parse(PacketHeader header, DataInputStream in) throws IOException, UnsupportedPublicKeyAlgorithmException, UnsupportedCriticalSubpacketTypeException, DuplicateSubpacketException, NoSuchAlgorithmException, UnsupportedHashAlgorithmException, UnsupportedSignatureVersionException, InvalidSubpacketLengthException {
        SignatureAttributes payload = SignatureAttributes.parse(in);
        switch (payload.attributes.pkAlgorithm) {
            case RSA_ENCRYPT_OR_SIGN:
                return new SignedSignatureAttributes(header, payload, RSASignature.parse(in));
            case RSA_ENCRYPT_ONLY:
                throw new UnsupportedPublicKeyAlgorithmException();
            case RSA_SIGN_ONLY:
                return new SignedSignatureAttributes(header, payload, RSASignature.parse(in));
            case ED25519:
                return new SignedSignatureAttributes(header, payload, Ed25519Signature.parse(in));
            default:
                throw new UnsupportedPublicKeyAlgorithmException();
        }
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        attributes.serialize(out);
        signature.serialize(out);
    }
}
