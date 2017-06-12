package co.krypt.kryptonite;

import android.content.Context;
import android.security.keystore.KeyProperties;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.SecureRandom;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.KeyType;
import co.krypt.kryptonite.crypto.RSAKeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPairI;

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
    }
}
