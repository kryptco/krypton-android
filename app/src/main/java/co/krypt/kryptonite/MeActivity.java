package co.krypt.kryptonite;

import android.os.Bundle;
import android.security.keystore.KeyInfo;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;


public class MeActivity extends AppCompatActivity {
    public static String LOG_TAG = "kryptonite";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);
        PrivateKey sk = KeyManager.generateKeyPair();
        try {
            KeyInfo keyInfo;
            KeyFactory factory  = KeyFactory.getInstance(sk.getAlgorithm(), "AndroidKeyStore");
            keyInfo = factory.getKeySpec(sk, KeyInfo.class);
            Log.i(LOG_TAG, String.valueOf(keyInfo.isInsideSecureHardware()));
            Log.i(LOG_TAG, String.valueOf(keyInfo.isUserAuthenticationRequired()));
            Log.i(LOG_TAG, String.valueOf(keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware()));
        } catch (InvalidKeySpecException e) {
            // Not an Android KeyStore key.
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
