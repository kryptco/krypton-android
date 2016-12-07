package co.krypt.kryptonite;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.security.InvalidKeyException;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;


public class MeActivity extends AppCompatActivity {
    public static String LOG_TAG = "kryptonite";

    TextView sshKeyTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);
        sshKeyTextView = (TextView) findViewById(R.id.sshKeyTextView);
        try {
            SSHKeyPair sk = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
            Log.i(LOG_TAG, String.valueOf(sk.isKeyStoredInSecureHardware()));
            sshKeyTextView.setText(Base64.encodeToString(sk.publicKeySSHWireFormat(), Base64.DEFAULT));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CryptoException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
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
