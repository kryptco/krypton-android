package co.krypt.krypton.crypto;

import android.content.Context;
import android.content.SharedPreferences;

import com.amazonaws.util.Base64;

import org.libsodium.jni.Sodium;

import java.security.SecureRandom;

import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.PGPPublicKey;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class EdKeyManager implements KeyManagerI {
    private static final Object lock = new Object();
    private static final String SSH_KEYPAIR_KEY = "SSH_KEY";
    private static final String SSH_KEYPAIR_CREATED_KEY = "SSH_KEY.created";
    private final SharedPreferences preferences;

    public static String LOG_TAG = "EdKeyManager";

    public EdKeyManager(Context context) {
        preferences = context.getSharedPreferences("KEY_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public SSHKeyPairI loadOrGenerateKeyPair(String tag) throws CryptoException {
        synchronized (lock) {
            Long created;
            if (preferences.contains(SSH_KEYPAIR_CREATED_KEY + "." + tag)) {
                created = preferences.getLong(SSH_KEYPAIR_CREATED_KEY + "." + tag, 0);
            } else {
                created = PGPPublicKey.currentTimeBackdatedByClockSkewTolerance();
                preferences.edit().putLong(SSH_KEYPAIR_CREATED_KEY + "." + tag, created).apply();
            }

            String skB64 = preferences.getString(SSH_KEYPAIR_KEY + "." + tag, null);
            if (skB64 == null) {
                byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
                byte[] sk = new byte[Sodium.crypto_sign_ed25519_secretkeybytes()];
                int result = Sodium.crypto_sign_ed25519_seed_keypair(pk, sk, SecureRandom.getSeed(Sodium.crypto_sign_ed25519_seedbytes()));
                if (result != 0) {
                    throw new CryptoException("non-zero sodium result: " + result);
                }
                preferences.edit().putString(SSH_KEYPAIR_KEY + "." + tag, Base64.encodeAsString(sk)).apply();
                return new EdSSHKeyPair(pk, sk, created);
            }
            byte[] sk = Base64.decode(skB64);
            byte[] pk = new byte[Sodium.crypto_sign_ed25519_publickeybytes()];
            int result = Sodium.crypto_sign_ed25519_sk_to_pk(pk, sk);
            if (result != 0) {
                throw new CryptoException("non-zero sodium result: " + result);
            }

            return new EdSSHKeyPair(pk, sk, created);
        }
    }

    public boolean keyExists(String tag) {
        synchronized (lock) {
            return preferences.contains(SSH_KEYPAIR_KEY + "." + tag);
        }
    }

    public void deleteKeyPair(String tag) throws Exception {
        synchronized (lock) {
            preferences.edit()
                    .putString(SSH_KEYPAIR_KEY + "." + tag, null)
                    .remove(SSH_KEYPAIR_CREATED_KEY + "." + tag)
                    .apply();
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }

}
