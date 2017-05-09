package co.krypt.kryptonite;

import android.security.keystore.KeyProperties;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.crypto.SSHKeyPair;

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

    @Test
    public void keyGenerationAndDeletion_succeed() throws Exception {
        KeyManager.deleteKeyPair("test");
        SSHKeyPair kp1 = KeyManager.loadOrGenerateKeyPair("test");
        Log.i("TEST", kp1.publicKeyDERBase64());
        KeyManager.deleteKeyPair("test");
    }

    @Test
    public void keyGeneration_isIdempotent() throws Exception {
        SSHKeyPair key1 = KeyManager.loadOrGenerateKeyPair("test");
        SSHKeyPair key2 = KeyManager.loadOrGenerateKeyPair("test");

        assertEquals(key1, key2);
    }

    @Test
    public void sign_succeeds() throws Exception {
        for (SSHKeyPair key : new SSHKeyPair[]{KeyManager.loadOrGenerateKeyPair("test"), KeyManager.loadOrGenerateNoDigestKeyPair("testnodigest")}) {
            byte[] data = SecureRandom.getSeed(32);
            for (String digest : SUPPORTED_DIGESTS) {
                key.signDigest(digest, data);
            }
        }
    }

    @Test
    public void signAndVerify_succeed() throws Exception {
        for (SSHKeyPair key : new SSHKeyPair[]{KeyManager.loadOrGenerateKeyPair("test"), KeyManager.loadOrGenerateNoDigestKeyPair("testnodigest")}) {
            byte[] data = SecureRandom.getSeed(32);
            for (String digest : SUPPORTED_DIGESTS) {
                byte[] signature = key.signDigest(digest, data);
                assertTrue(key.verifyDigest(digest, signature, data));
            }
        }
    }

    @Test
    public void signTamperAndVerify_fails() throws Exception {
        for (SSHKeyPair key : new SSHKeyPair[]{KeyManager.loadOrGenerateKeyPair("test"), KeyManager.loadOrGenerateNoDigestKeyPair("testnodigest")}) {
            byte[] data = SecureRandom.getSeed(32);
            for (String digest : SUPPORTED_DIGESTS) {
                byte[] signature = key.signDigest(digest, data);
                signature[signature.length - 1] ^= 0x01;
                assertTrue(!key.verifyDigest(digest, signature, data));
            }
        }
    }
}
