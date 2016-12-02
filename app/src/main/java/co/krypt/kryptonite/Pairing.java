package co.krypt.kryptonite;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

import android.support.annotation.NonNull;

import org.libsodium.jni.Sodium;

import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.SodiumException;

public class Pairing {
    byte[] workstationPublicKey;

    public Pairing(@NonNull byte[] workstationPublicKey) throws CryptoException {
        if (workstationPublicKey.length != Sodium.crypto_box_publickeybytes()) {
            throw new SodiumException("workstation public key invalid");
        }
        this.workstationPublicKey = workstationPublicKey;
    }

    public byte[] wrapKey(byte[] symmetricKey) throws CryptoException {
        byte[] ciphertext = new byte[symmetricKey.length + Sodium.crypto_box_sealbytes()];
        if (0 != Sodium.crypto_box_seal(ciphertext, symmetricKey, symmetricKey.length, workstationPublicKey)) {
            throw new SodiumException("crypto_box_seal failed");
        }
        return ciphertext;
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }
}
