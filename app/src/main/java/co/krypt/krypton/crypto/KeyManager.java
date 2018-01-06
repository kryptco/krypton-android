package co.krypt.krypton.crypto;

import android.content.Context;

import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 5/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyManager {
    public static final String ME_TAG = "me";

    public static synchronized SSHKeyPairI loadOrGenerateKeyPair(Context context, KeyType type, String tag) throws CryptoException {
        return type.keyManager(context).loadOrGenerateKeyPair(type.keyTag(tag));
    }

    public static synchronized void deleteAllMeKeyPairs(Context context) throws CryptoException {
        for (KeyType type: new KeyType[]{KeyType.Ed25519, KeyType.RSA}) {
            try {
                type.keyManager(context).deleteKeyPair(type.me());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized SSHKeyPairI loadMeRSAOrEdKeyPair(Context context) throws CryptoException {
        for (KeyType type: new KeyType[]{KeyType.RSA, KeyType.Ed25519}) {
            if (type.keyManager(context).keyExists(type.keyTag(ME_TAG))) {
                return type.keyManager(context).loadOrGenerateKeyPair(type.keyTag(ME_TAG));
            }
        }
        throw new CryptoException("no key pair exists");
    }

    public static synchronized void deleteKeyPair(Context context, KeyType type, String tag) throws Exception {
        type.keyManager(context).deleteKeyPair(tag);
    }
}
