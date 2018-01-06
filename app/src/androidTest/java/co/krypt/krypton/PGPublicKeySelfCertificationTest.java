package co.krypt.krypton;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Collections;

import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.crypto.KeyType;
import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.pgp.PGPPublicKey;
import co.krypt.krypton.pgp.UserID;
import co.krypt.krypton.pgp.publickey.CertifiedPublicKey;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PGPublicKeySelfCertificationTest {
    @Test
    public void keySigning_succeeds() throws Exception {
        for (KeyType keyType : new KeyType[]{KeyType.RSA, KeyType.Ed25519}) {
            SSHKeyPairI kp1 = KeyManager.loadOrGenerateKeyPair(InstrumentationRegistry.getTargetContext(), keyType, "test");
            PGPPublicKey pubkey = new PGPPublicKey(kp1, Collections.singletonList(new UserID("Kevin King", "kevin@krypt.co")));

            CertifiedPublicKey parsedPubkey = CertifiedPublicKey.parse(
                    new DataInputStream(new ByteArrayInputStream(pubkey.serializedBytes()))
            );
            Assert.assertTrue(pubkey.signedIdentities.size() == parsedPubkey.identities.size());

            Assert.assertFalse(pubkey.signedIdentities.get(0).signature.attributes.attributes.unhashedSubpackets.issuer.header.type.critical);
        }
    }
}
