package co.krypt.kryptonite;

import org.junit.Test;
import org.libsodium.jni.Sodium;

import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SealInstrumentedTest {
    @Test
    public void seal_inverts() throws Exception {
        byte[] pubKey = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] privKey = new byte[Sodium.crypto_box_secretkeybytes()];
        assertTrue(0 == Sodium.crypto_box_seed_keypair(pubKey, privKey, SecureRandom.getSeed(Sodium.crypto_box_seedbytes())));
        Pairing pairing = Pairing.generate(pubKey, "workstation");
        for (int i = 0; i < 1024; i++) {
            byte[] message = SecureRandom.getSeed(i);
            byte[] ciphertext = pairing.seal(message);
            byte[] unsealed = pairing.unseal(ciphertext);
            assertTrue(Arrays.equals(message, unsealed));
        }
    }
    static {
        System.loadLibrary("sodiumjni");
    }
}