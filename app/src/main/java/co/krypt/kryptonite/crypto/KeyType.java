package co.krypt.kryptonite.crypto;

import android.content.Context;

import co.krypt.kryptonite.exception.UnhandledKeyTypeException;


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
                return new RSAKeyManager();
            case Ed25519:
                return new EdKeyManager(context);
        }
        throw new UnhandledKeyTypeException(this.name());
    }
}
