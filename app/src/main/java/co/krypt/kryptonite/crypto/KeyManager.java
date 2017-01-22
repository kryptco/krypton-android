package co.krypt.kryptonite.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.amazonaws.util.Base64;

import org.libsodium.jni.Sodium;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyManager {
    private static final Object lock = new Object();
    private static final String SSH_KEYPAIR_KEY = "SSH_KEY";
    private final SharedPreferences preferences;

    public static String LOG_TAG = "kryptonite";

    public static String MY_ED25519_KEY_TAG = "ED25519.me";

    public KeyManager(Context context) {
        preferences = context.getSharedPreferences("KEY_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public SSHKeyPair loadOrGenerateKeyPair(String tag) throws CryptoException {
        synchronized (lock) {
            String skB64 = preferences.getString(SSH_KEYPAIR_KEY + "." + tag, null);
            if (skB64 == null) {
                byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
                byte[] sk = new byte[Sodium.crypto_sign_ed25519_secretkeybytes()];
                int result = Sodium.crypto_sign_ed25519_seed_keypair(pk, sk, SecureRandom.getSeed(Sodium.crypto_sign_ed25519_seedbytes()));
                if (result != 0) {
                    throw new CryptoException("non-zero sodium result: " + result);
                }
                preferences.edit().putString(SSH_KEYPAIR_KEY + "." + tag, Base64.encodeAsString(sk)).commit();
                return new SSHKeyPair(pk, sk);
            }
            byte[] sk = Base64.decode(skB64);
            byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
            int result = Sodium.crypto_sign_ed25519_sk_to_pk(pk, sk);
            if (result != 0) {
                throw new CryptoException("non-zero sodium result: " + result);
            }
            return new SSHKeyPair(pk, sk);
        }
    }

    public boolean keyExists(String tag) {
        synchronized (lock) {
            return preferences.contains(SSH_KEYPAIR_KEY + "." + tag);
        }
    }

    public void deleteKeyPair(String tag) throws Exception {
        synchronized (lock) {
            preferences.edit().putString(SSH_KEYPAIR_KEY + "." + tag, null).commit();
        }
    }

    public static void logKeyInfo(PrivateKey sk) {
        try {
            KeyInfo keyInfo;
            KeyFactory factory = KeyFactory.getInstance(sk.getAlgorithm(), "AndroidKeyStore");
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

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }

}
