package co.krypt.krypton;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;

import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.crypto.KeyType;
import co.krypt.krypton.crypto.RSAKeyManager;
import co.krypt.krypton.crypto.SSHKeyPairI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class KeyManagerInstrumentedTest {
    private static final String[] SUPPORTED_DIGESTS = new String[]{KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512};
    private static final KeyType[] SUPPORTED_KEY_TYPES = new KeyType[]{KeyType.RSA, KeyType.Ed25519};

    @Test
    public void keyGenerationAndDeletion_succeed() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        for (KeyType type: SUPPORTED_KEY_TYPES) {
            KeyManager.deleteKeyPair(context, type, "test");
            SSHKeyPairI kp1 = KeyManager.loadOrGenerateKeyPair(InstrumentationRegistry.getTargetContext(), type, "test");
            Log.i("TEST", kp1.publicKeyDERBase64());
            KeyManager.deleteKeyPair(context, type, "test");
        }
    }

    @Test
    public void keyGeneration_isIdempotent() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        for (KeyType type: SUPPORTED_KEY_TYPES) {
            SSHKeyPairI key1 = KeyManager.loadOrGenerateKeyPair(context, type, "test");
            SSHKeyPairI key2 = KeyManager.loadOrGenerateKeyPair(context, type, "test");

            assertEquals(key1, key2);
        }
    }

    @Test
    public void sign_succeeds() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        for (KeyType type: SUPPORTED_KEY_TYPES) {
            for (SSHKeyPairI key : new SSHKeyPairI[]{KeyManager.loadOrGenerateKeyPair(context, type, "test"), new RSAKeyManager(context).loadOrGenerateNoDigestKeyPair("testnodigest")}) {
                byte[] data = SecureRandom.getSeed(32);
                for (String digest : SUPPORTED_DIGESTS) {
                    key.signDigest(digest, data);
                }
            }
        }
    }

    @Test
    public void signAndVerify_succeed() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        for (KeyType type: SUPPORTED_KEY_TYPES) {
            for (SSHKeyPairI key : new SSHKeyPairI[]{KeyManager.loadOrGenerateKeyPair(context, type, "test"), new RSAKeyManager(context).loadOrGenerateNoDigestKeyPair("testnodigest")}) {
                byte[] data = SecureRandom.getSeed(32);
                for (String digest : SUPPORTED_DIGESTS) {
                    byte[] signature = key.signDigest(digest, data);
                    assertTrue(key.verifyDigest(digest, signature, data));
                }
            }
        }
    }

    @Test
    public void signTamperAndVerify_fails() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        for (KeyType type: SUPPORTED_KEY_TYPES) {
            for (SSHKeyPairI key : new SSHKeyPairI[]{KeyManager.loadOrGenerateKeyPair(context, type, "test"), new RSAKeyManager(context).loadOrGenerateNoDigestKeyPair("testnodigest")}) {
                byte[] data = SecureRandom.getSeed(32);
                for (String digest : SUPPORTED_DIGESTS) {
                    byte[] signature = key.signDigest(digest, data);
                    signature[signature.length - 1] ^= 0x01;
                    assertTrue(!key.verifyDigest(digest, signature, data));
                }
            }
        }
    }
}
