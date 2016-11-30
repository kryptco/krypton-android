package co.krypt.kryptonite;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyManager {

    public static String LOG_TAG = "kryptonite";

    public static PrivateKey generateKeyPair() {
        try {
            // The key pair can also be obtained from the Android Keystore any time as follows:
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
//            keyStore.deleteEntry("PRIVATEKEYRSA");
//            KeyStore.Entry keystoreEntry = keyStore.getEntry("PRIVATEKEYRSA", null);
//            if (keystoreEntry instanceof KeyStore.PrivateKeyEntry) {
//                return ((KeyStore.PrivateKeyEntry) keystoreEntry).getPrivateKey();
//            }
//            else {
//                Log.w(LOG_TAG, "Not an instance of a PrivateKeyEntry");
//            }
            KeyPair keyPair = null;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            "PRIVATEKEYRSA", KeyProperties.PURPOSE_SIGN)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                            .setKeySize(4096)
                            .setUserAuthenticationRequired(true)
                            .setUserAuthenticationValidityDurationSeconds(300)
                            .build());
            long genStart = System.currentTimeMillis();
            keyPair = keyPairGenerator.generateKeyPair();
            long genStop = System.currentTimeMillis();
            Log.i(LOG_TAG, "KeyGen took " + String.valueOf((genStop - genStart)));
            return keyPair.getPrivate();

        } catch (GeneralSecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return null;
    }

}
