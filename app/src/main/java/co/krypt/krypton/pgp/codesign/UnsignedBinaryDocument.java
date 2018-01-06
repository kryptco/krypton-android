package co.krypt.krypton.pgp.codesign;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.packet.HashAlgorithm;
import co.krypt.krypton.pgp.packet.Signable;
import co.krypt.krypton.pgp.packet.SignableUtils;
import co.krypt.krypton.pgp.packet.Signature;
import co.krypt.krypton.pgp.packet.SignatureAttributes;
import co.krypt.krypton.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.krypton.pgp.packet.SignatureType;
import co.krypt.krypton.pgp.packet.SignedSignatureAttributes;
import co.krypt.krypton.pgp.publickey.PublicKeyPacket;
import co.krypt.krypton.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.krypton.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.krypton.pgp.subpacket.IssuerSubpacket;
import co.krypt.krypton.pgp.subpacket.SignatureCreationTimeSubpacket;
import co.krypt.krypton.pgp.subpacket.Subpacket;
import co.krypt.krypton.pgp.subpacket.SubpacketList;

/**
 * Created by Kevin King on 6/17/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class UnsignedBinaryDocument implements Signable {
    public final byte[] data;
    public final SignatureAttributesWithoutHashPrefix attributes;

    public UnsignedBinaryDocument(byte[] data, SignatureAttributesWithoutHashPrefix attributes) {
        this.data = data;
        this.attributes = attributes;
    }

    public UnsignedBinaryDocument(byte[] data, SSHKeyPairI kp, HashAlgorithm hash) throws InvalidSubpacketLengthException, DuplicateSubpacketException, IOException, NoSuchAlgorithmException, CryptoException {
        this.data = data;
        this.attributes = new SignatureAttributesWithoutHashPrefix(
                SignatureType.BINARY,
                kp.pgpPublicKeyPacketAttributes().algorithm,
                hash,
                SubpacketList.fromList(
                        Collections.<Subpacket>singletonList(
                                SignatureCreationTimeSubpacket.fromTime(System.currentTimeMillis()/1000)
                        )
                ),
                SubpacketList.fromList(
                        Collections.<Subpacket>singletonList(
                                IssuerSubpacket.fromIssuerKeyID(PublicKeyPacket.fromKeyPair(kp).keyID())
                        )
                )
        );

    }

    public SignedSignatureAttributes sign(Signature signature) throws IOException, NoSuchAlgorithmException {
        return new SignedSignatureAttributes(
                new SignatureAttributes(
                        attributes,
                        SignableUtils.hashPrefix(attributes.hashAlgorithm, this)
                ),
                signature
        );
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        out.write(data);

        attributes.writeSignableData(out);
    }
}
