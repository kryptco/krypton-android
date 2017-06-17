package co.krypt.kryptonite;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.KeyType;
import co.krypt.kryptonite.crypto.RSAKeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.pgp.PGPManager;
import co.krypt.kryptonite.pgp.PGPPublicKey;
import co.krypt.kryptonite.pgp.UserID;
import co.krypt.kryptonite.pgp.publickey.CertifiedPublicKey;
import co.krypt.kryptonite.pgp.publickey.SignedPublicKeySelfCertification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PGPublicKeySelfCertificationTest {
    @Test
    public void keySigning_succeeds() throws Exception {
        SSHKeyPairI kp1 = KeyManager.loadOrGenerateKeyPair(InstrumentationRegistry.getTargetContext(), KeyType.RSA, "test");
        PGPPublicKey pubkey = new PGPPublicKey(kp1, Collections.singletonList(new UserID("Kevin King", "kevin@krypt.co")));

        CertifiedPublicKey parsedPubkey = CertifiedPublicKey.parse(
                new DataInputStream(new ByteArrayInputStream(pubkey.serializedBytes()))
        );
        Assert.assertTrue(pubkey.signedIdentities.size() == parsedPubkey.identities.size());

        Assert.assertFalse(pubkey.signedIdentities.get(0).signature.attributes.attributes.unhashedSubpackets.issuer.header.type.critical);
    }
}
