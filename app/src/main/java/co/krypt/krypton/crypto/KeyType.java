package co.krypt.krypton.crypto;

import android.content.Context;

import co.krypt.krypton.exception.UnhandledKeyTypeException;


/**
 * Created by Kevin King on 5/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public enum KeyType {
    RSA, Ed25519;

    public String keyTag(String tag) {
        return name() + "." + tag;
    }

    public String me() {
        return keyTag("me");
    }

    public KeyManagerI keyManager(Context context) {
        switch (this) {
            case RSA:
                return new RSAKeyManager(context);
            case Ed25519:
                return new EdKeyManager(context);
        }
        throw new UnhandledKeyTypeException(this.name());
    }
}
