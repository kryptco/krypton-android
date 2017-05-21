package co.krypt.kryptonite.crypto;

import android.content.Context;
import android.content.SharedPreferences;

import com.amazonaws.util.Base64;

import org.libsodium.jni.Sodium;

import java.security.SecureRandom;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class EdKeyManager implements KeyManagerI {
    private static final Object lock = new Object();
    private static final String SSH_KEYPAIR_KEY = "SSH_KEY";
    private final SharedPreferences preferences;

    public static String LOG_TAG = "EdKeyManager";

    public EdKeyManager(Context context) {
        preferences = context.getSharedPreferences("KEY_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public SSHKeyPairI loadOrGenerateKeyPair(String tag) throws CryptoException {
        synchronized (lock) {
            String skB64 = preferences.getString(SSH_KEYPAIR_KEY + "." + tag, null);
            if (skB64 == null) {
                byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
                byte[] sk = new byte[Sodium.crypto_sign_ed25519_secretkeybytes()];
                int result = Sodium.crypto_sign_ed25519_seed_keypair(pk, sk, SecureRandom.getSeed(Sodium.crypto_sign_ed25519_seedbytes()));
                if (result != 0) {
                    throw new CryptoException("non-zero sodium result: " + result);
                }
                preferences.edit().putString(SSH_KEYPAIR_KEY + "." + tag, Base64.encodeAsString(sk)).apply();
                return new EdSSHKeyPair(pk, sk);
            }
            byte[] sk = Base64.decode(skB64);
            byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
            int result = Sodium.crypto_sign_ed25519_sk_to_pk(pk, sk);
            if (result != 0) {
                throw new CryptoException("non-zero sodium result: " + result);
            }
            return new EdSSHKeyPair(pk, sk);
        }
    }

    public boolean keyExists(String tag) {
        synchronized (lock) {
            return preferences.contains(SSH_KEYPAIR_KEY + "." + tag);
        }
    }

    public void deleteKeyPair(String tag) throws Exception {
        synchronized (lock) {
            preferences.edit().putString(SSH_KEYPAIR_KEY + "." + tag, null).apply();
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }

}
