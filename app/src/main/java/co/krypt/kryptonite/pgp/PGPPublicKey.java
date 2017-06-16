package co.krypt.kryptonite.pgp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.List;

import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.packet.HashAlgorithm;
import co.krypt.kryptonite.pgp.packet.Serializable;
import co.krypt.kryptonite.pgp.packet.SignableUtils;
import co.krypt.kryptonite.pgp.publickey.PublicKeyPacket;
import co.krypt.kryptonite.pgp.publickey.SignedPublicKeySelfCertification;
import co.krypt.kryptonite.pgp.publickey.UnsignedPublicKeySelfCertification;
import co.krypt.kryptonite.pgp.subpacket.KeyFlagsSubpacket;

/**
 * Created by Kevin King on 6/16/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class PGPPublicKey extends Serializable {
    public final PublicKeyPacket publicKeyPacket;
    public final List<SignedPublicKeySelfCertification> signedIdentities;

    public PGPPublicKey(SSHKeyPairI kp, List<UserID> userIDs) throws PGPException {
        final HashAlgorithm hash = HashAlgorithm.SHA512;
        try {
            publicKeyPacket = PublicKeyPacket.fromKeyPair(kp);
            List<SignedPublicKeySelfCertification> signedIdentities = new LinkedList<>();

            for (UserID userID : userIDs) {
                UnsignedPublicKeySelfCertification unsignedIdentity = new UnsignedPublicKeySelfCertification(
                        publicKeyPacket,
                        userID,
                        hash,
                        System.currentTimeMillis() / 1000,
                        new KeyFlagsSubpacket.Flag[]{KeyFlagsSubpacket.Flag.SIGN_DATA}
                        );
                signedIdentities.add(
                        unsignedIdentity.sign(
                                kp.pgpSign(hash, SignableUtils.signableBytes(unsignedIdentity))
                        )
                );
            }

            this.signedIdentities = signedIdentities;
        } catch (IOException | CryptoException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException | InvalidKeySpecException e) {
            throw new PGPException(e.getMessage(), e);
        }
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        publicKeyPacket.serialize(out);
        for (SignedPublicKeySelfCertification identity: signedIdentities) {
            identity.certification.userIDPacket.serialize(out);
            identity.signature.serialize(out);
        }
    }
}
