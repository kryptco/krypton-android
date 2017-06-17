package co.krypt.kryptonite.pgp.codesign;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.packet.HashAlgorithm;
import co.krypt.kryptonite.pgp.packet.Signable;
import co.krypt.kryptonite.pgp.packet.SignableUtils;
import co.krypt.kryptonite.pgp.packet.Signature;
import co.krypt.kryptonite.pgp.packet.SignatureAttributes;
import co.krypt.kryptonite.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.kryptonite.pgp.packet.SignatureType;
import co.krypt.kryptonite.pgp.packet.SignedSignatureAttributes;
import co.krypt.kryptonite.pgp.publickey.PublicKeyPacket;
import co.krypt.kryptonite.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.kryptonite.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.kryptonite.pgp.subpacket.IssuerSubpacket;
import co.krypt.kryptonite.pgp.subpacket.SignatureCreationTimeSubpacket;
import co.krypt.kryptonite.pgp.subpacket.Subpacket;
import co.krypt.kryptonite.pgp.subpacket.SubpacketList;

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
