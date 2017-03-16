package co.krypt.kryptonite;

import org.junit.Test;
import org.libsodium.jni.Sodium;

import java.security.SecureRandom;
import java.util.Arrays;

import co.krypt.kryptonite.pairing.Pairing;

import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SodiumInstrumentedTest {
    @Test
    public void wrap_isCorrect() throws Exception {
        byte[] pubKey = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] privKey = new byte[Sodium.crypto_box_secretkeybytes()];
        assertTrue(0 == Sodium.crypto_box_seed_keypair(pubKey, privKey, SecureRandom.getSeed(Sodium.crypto_box_seedbytes())));
        byte[] enclavePubKey = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] enclavePrivKey = new byte[Sodium.crypto_box_secretkeybytes()];
        assertTrue(0 == Sodium.crypto_box_seed_keypair(pubKey, privKey, SecureRandom.getSeed(Sodium.crypto_box_seedbytes())));
        Pairing pairing = new Pairing(pubKey, enclavePrivKey, enclavePubKey, "workstation");
        byte[] ciphertext = pairing.wrapKey();

        byte[] unwrapped = new byte[ciphertext.length - Sodium.crypto_box_sealbytes()];
        assertTrue(0 == Sodium.crypto_box_seal_open(unwrapped, ciphertext, ciphertext.length, pubKey, privKey));
        assertTrue(Arrays.equals(unwrapped, enclavePrivKey));
    }
    static {
        System.loadLibrary("sodiumjni");
    }
}