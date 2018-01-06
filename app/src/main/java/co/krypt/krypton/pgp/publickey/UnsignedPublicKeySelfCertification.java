package co.krypt.krypton.pgp.publickey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.UserID;
import co.krypt.krypton.pgp.packet.HashAlgorithm;
import co.krypt.krypton.pgp.packet.Signable;
import co.krypt.krypton.pgp.packet.SignableUtils;
import co.krypt.krypton.pgp.packet.Signature;
import co.krypt.krypton.pgp.packet.SignatureAttributes;
import co.krypt.krypton.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.krypton.pgp.packet.SignatureType;
import co.krypt.krypton.pgp.packet.SignedSignatureAttributes;
import co.krypt.krypton.pgp.packet.UserIDPacket;
import co.krypt.krypton.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.krypton.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.krypton.pgp.subpacket.IssuerSubpacket;
import co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket;
import co.krypt.krypton.pgp.subpacket.SignatureCreationTimeSubpacket;
import co.krypt.krypton.pgp.subpacket.Subpacket;
import co.krypt.krypton.pgp.subpacket.SubpacketDataTooLongException;
import co.krypt.krypton.pgp.subpacket.SubpacketList;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

/*
    Certify that a public key corresponds to a specific user id.
 */
public class UnsignedPublicKeySelfCertification implements Signable {
    public final PublicKeyPacket publicKeyPacket;
    public final UserIDPacket userIDPacket;
    public final SignatureCreationTimeSubpacket createdSubpacket;
    @Nullable
    public final KeyFlagsSubpacket keyFlagsSubpacket;
    public final IssuerSubpacket issuerSubpacket;
    public final SignatureAttributesWithoutHashPrefix signatureAttributesWithoutHashPrefix;

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
