package co.krypt.kryptonite.pgp.publickey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.packet.HashAlgorithm;
import co.krypt.kryptonite.pgp.packet.Signable;
import co.krypt.kryptonite.pgp.packet.SignableUtils;
import co.krypt.kryptonite.pgp.packet.Signature;
import co.krypt.kryptonite.pgp.packet.SignatureAttributes;
import co.krypt.kryptonite.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.kryptonite.pgp.packet.SignatureType;
import co.krypt.kryptonite.pgp.packet.SignedSignatureAttributes;
import co.krypt.kryptonite.pgp.UserID;
import co.krypt.kryptonite.pgp.packet.UserIDPacket;
import co.krypt.kryptonite.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.kryptonite.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.kryptonite.pgp.subpacket.IssuerSubpacket;
import co.krypt.kryptonite.pgp.subpacket.KeyFlagsSubpacket;
import co.krypt.kryptonite.pgp.subpacket.SignatureCreationTimeSubpacket;
import co.krypt.kryptonite.pgp.subpacket.Subpacket;
import co.krypt.kryptonite.pgp.subpacket.SubpacketDataTooLongException;
import co.krypt.kryptonite.pgp.subpacket.SubpacketList;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

/*
    Certify that a public key corresponds to a specific user id.
 */
public class UnsignedPublicKeySelfCertification implements Signable {
    final PublicKeyPacket publicKeyPacket;
    final UserIDPacket userIDPacket;
    final SignatureCreationTimeSubpacket createdSubpacket;
    @Nullable
    final KeyFlagsSubpacket keyFlagsSubpacket;
    final IssuerSubpacket issuerSubpacket;
    final SignatureAttributesWithoutHashPrefix signatureAttributesWithoutHashPrefix;

    public UnsignedPublicKeySelfCertification(PublicKeyPacket publicKeyPacket, UserID userID, HashAlgorithm hashAlgorithm, long created, @Nullable KeyFlagsSubpacket.Flag[] keyFlags) throws IOException, CryptoException, SubpacketDataTooLongException, NoSuchAlgorithmException, DuplicateSubpacketException, InvalidSubpacketLengthException {
        this.publicKeyPacket = publicKeyPacket;
        this.userIDPacket = UserIDPacket.fromUserID(userID);
        this.createdSubpacket = SignatureCreationTimeSubpacket.fromTime(created);

        if (keyFlags != null) {
            this.keyFlagsSubpacket = KeyFlagsSubpacket.withFlags(keyFlags);
        } else {
            this.keyFlagsSubpacket = null;
        }

        this.issuerSubpacket = IssuerSubpacket.fromIssuerKeyID(publicKeyPacket.keyID());

        List<Subpacket> hashedSubpacketList = new LinkedList<>();
        hashedSubpacketList.add(createdSubpacket);
        if (keyFlagsSubpacket != null) {
            hashedSubpacketList.add(keyFlagsSubpacket);
        }
        SubpacketList hashedSubpackets = SubpacketList.fromList(hashedSubpacketList);
        SubpacketList unhashedSubpackets = SubpacketList.fromList(
                Arrays.<Subpacket>asList(issuerSubpacket)
        );

        signatureAttributesWithoutHashPrefix = new SignatureAttributesWithoutHashPrefix(
                SignatureType.POSITIVE_USER_ID,
                publicKeyPacket.attributes.algorithm,
                hashAlgorithm,
                hashedSubpackets,
                unhashedSubpackets
        );
    }

    public SignedPublicKeySelfCertification sign(Signature signature) throws IOException, NoSuchAlgorithmException {
        SignatureAttributes signatureAttributes = new SignatureAttributes(
                signatureAttributesWithoutHashPrefix,
                SignableUtils.hashPrefix(signatureAttributesWithoutHashPrefix.hashAlgorithm, this)
        );
        return new SignedPublicKeySelfCertification(
                this,
                new SignedSignatureAttributes(signatureAttributes, signature)
        );
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        out.writeByte(0x99);
        SignableUtils.writeLengthAndSignableData(publicKeyPacket, out);

        out.writeByte(0xB4);
        userIDPacket.writeSignableData(out);

        signatureAttributesWithoutHashPrefix.writeSignableData(out);
    }
}
