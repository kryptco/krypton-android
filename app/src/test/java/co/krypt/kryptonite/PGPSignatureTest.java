package co.krypt.kryptonite;

import junit.framework.Assert;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import co.krypt.kryptonite.pgp.asciiarmor.AsciiArmor;
import co.krypt.kryptonite.pgp.packet.SignableUtils;
import co.krypt.kryptonite.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.kryptonite.pgp.publickey.CertifiedPublicKey;
import co.krypt.kryptonite.pgp.publickey.UnsignedPublicKeySelfCertification;

import static co.krypt.kryptonite.PGPPublicKeyTest.testPubKey2;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PGPSignatureTest {
    @Test
    public void signedPublicKeyHashPrefix_matches() throws Exception {
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(AsciiArmor.parse(testPubKey2).data)));
        SignatureAttributesWithoutHashPrefix att = pk.identities.get(0).second.get(0).attributes.attributes;
        UnsignedPublicKeySelfCertification certification = new UnsignedPublicKeySelfCertification(
                pk.publicKeyPacket,
                pk.identities.get(0).first.userID,
                att.getHashAlgorithm(),
                att.hashedSubpackets.created.created,
                null  //  this pubkey has no flags
        );
        Assert.assertTrue(SignableUtils.hashPrefix(att.hashAlgorithm, certification) == pk.identities.get(0).second.get(0).attributes.hashPrefix);
    }
}