package co.krypt.kryptonite;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;
import java.security.SignatureException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class KeyManagerInstrumentedTest {
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
        SSHKeyPair key = KeyManager.loadOrGenerateKeyPair("test");
        byte[] data = SecureRandom.getSeed(32);
        key.signDigest(SHA256.digest(data));
    }

    @Test
    public void signAndVerify_succeed() throws Exception {
        SSHKeyPair key = KeyManager.loadOrGenerateKeyPair("test");
        byte[] data = SecureRandom.getSeed(32);
        byte[] digest = SHA256.digest(data);
        byte[] signature = key.signDigest(digest);
        assertTrue(key.verifyDigest(signature, digest));
    }

    @Test
    public void signTamperAndVerify_fails() throws Exception {
        SSHKeyPair key = KeyManager.loadOrGenerateKeyPair("test");
        byte[] data = SecureRandom.getSeed(32);
        byte[] digest = SHA256.digest(data);
        byte[] signature = key.signDigest(digest);
        signature[signature.length - 1] ^= 0x01;
        assertTrue(!key.verifyDigest(signature, digest));
    }
}
