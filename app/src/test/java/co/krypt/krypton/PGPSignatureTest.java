package co.krypt.krypton;

import junit.framework.Assert;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import co.krypt.krypton.pgp.asciiarmor.AsciiArmor;
import co.krypt.krypton.pgp.codesign.UnsignedBinaryDocument;
import co.krypt.krypton.pgp.packet.SignableUtils;
import co.krypt.krypton.pgp.packet.SignatureAttributesWithoutHashPrefix;
import co.krypt.krypton.pgp.packet.SignedSignatureAttributes;
import co.krypt.krypton.pgp.publickey.CertifiedPublicKey;
import co.krypt.krypton.pgp.publickey.UnsignedPublicKeySelfCertification;

import static co.krypt.krypton.PGPPublicKeyTest.testPubKey2;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class PGPSignatureTest {
    public static final String testSig1 = "-----BEGIN PGP SIGNATURE-----\n" +
            "iQIcBAABCgAGBQJZILCJAAoJEO31bzzC5uMwvgQQAItcIWAsEgcq+IEbDzGapLDu\n" +
            "BWj1O67IIvS9/kswH1HfCptQSSQwEb7WU8/DnyYoQ3dVZYSpP6WOP1zRU5PE+4mF\n" +
            "6TUzbM9ribYbmZsac1HUgF2XWxQBcA6IBSQWlIo/Gvkd3rprMVoyZxZgeq4Tbiby\n" +
            "bYssdF/7DoagDyy+2czZ8GYqxa3z2josHw6sx+OYmOWOn8AWyRq5eM+A5QRESkUl\n" +
            "frRrG/ncQkZ6BU0ATqi706VSYEGPtP6Ia6tKjEYTZjN56akfgxfswjUCpSIoZd6C\n" +
            "KUEi+qsZQnO54zZYbajyUo2ozmUOppTqMPsZPl+answEnQfZqVFv/mBOlEFVe9tO\n" +
            "U5UL+KarHkDhUD51B/Sg4zSAYZM17j/FI47wiT1g0AzrCUsc5tLqWk7d119hrI5M\n" +
            "jNAG76XiIsirj0MJRY0p3O/K6HhKWM+jmYq4rSuX5/pCTPqnedgHgNeZh6HcymjS\n" +
            "cfqqkIzrjbjd3+6PuSIwNy0cOmHubitCj4+YG/p8lzVaLc/uCmxIGM2ouCJiDRaA\n" +
            "sD8iyb2jsiwYxBHlvE1dk7sIMUwywI/ufjc488vRsVCNI2KKmWBCCZ4GIZS3iP7p\n" +
            "TQA+FLWhvtWuNxFD+pUd/hxX0MODE+6gDMuNmIY2+Uhegjvhr1HgWFLSb2HeMi2/\n" +
            "BCUDirSuE9bXfNTmOYa6\n" +
            "=zIXq\n" +
            "-----END PGP SIGNATURE-----\n";

    public static final String signedData1 = "tree f21ec9de97e3ff0a66a068682a176020e91dcf29\n" +
            "parent 9d66027f0cbff220fdce8c2f9ab61aad4c65d6fa\n" +
            "author Kevin King <4kevinking@gmail.com> 1495034240 -0400\n" +
            "committer Kevin King <4kevinking@gmail.com> 1495314475 -0400\n" +
            "\n" +
            "add krgpg dir\n";

    @Test
    public void signedPublicKeyHashPrefixAndReserializedBytes_match() throws Exception {
        byte[] signedKeyData = AsciiArmor.parse(testPubKey2).data;
        CertifiedPublicKey pk = CertifiedPublicKey.parse(new DataInputStream(new ByteArrayInputStream(signedKeyData)));
        SignatureAttributesWithoutHashPrefix att = pk.identities.get(0).second.get(0).attributes.attributes;
        UnsignedPublicKeySelfCertification certification = new UnsignedPublicKeySelfCertification(
                pk.publicKeyPacket,
                pk.identities.get(0).first.userID,
                att.getHashAlgorithm(),
                att.hashedSubpackets.created.created,
                null  //  this pubkey has no flags
        );
        Assert.assertTrue(SignableUtils.hashPrefix(att.hashAlgorithm, certification) == pk.identities.get(0).second.get(0).attributes.hashPrefix);

        ByteArrayOutputStream reserialized = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(reserialized);
        pk.publicKeyPacket.serialize(out);
        pk.identities.get(0).first.serialize(out);
        pk.identities.get(0).second.get(0).serialize(out);
        out.close();

        Assert.assertTrue(Arrays.equals(reserialized.toByteArray(), signedKeyData));
    }

    @Test
    public void signedBinaryDocumentHashPrefix_matches() throws Exception {
        byte[] signatureBytes = AsciiArmor.parse(testSig1).data;
        SignedSignatureAttributes sig = SignedSignatureAttributes.parse(new DataInputStream(new ByteArrayInputStream(signatureBytes)));

        UnsignedBinaryDocument doc = new UnsignedBinaryDocument(
                signedData1.getBytes("UTF-8"),
                sig.attributes.attributes
        );

        Assert.assertTrue(SignableUtils.hashPrefix(sig.attributes.attributes.hashAlgorithm, doc) == sig.attributes.hashPrefix);

        ByteArrayOutputStream reserialized = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(reserialized);
        sig.serialize(out);
        out.close();

        Assert.assertTrue(Arrays.equals(reserialized.toByteArray(), signatureBytes));
    }
}