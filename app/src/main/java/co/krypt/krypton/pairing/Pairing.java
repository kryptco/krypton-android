package co.krypt.krypton.pairing;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

import android.support.annotation.NonNull;

import org.libsodium.jni.Sodium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.exception.SodiumException;
import co.krypt.krypton.protocol.PairingQR;

public class Pairing {
    public final byte[] workstationPublicKey;
    final byte[] enclaveSecretKey;
    final byte[] enclavePublicKey;
    public final String workstationName;
    public final UUID uuid;

    public Pairing(@NonNull byte[] workstationPublicKey, @NonNull byte[] enclaveSecretKey, @NonNull byte[] enclavePublicKey, String workstationName) throws CryptoException {
        if (workstationPublicKey.length != Sodium.crypto_box_publickeybytes()) {
            throw new SodiumException("workstation public key invalid");
        }
        this.workstationPublicKey = workstationPublicKey;
        this.enclaveSecretKey = enclaveSecretKey;
        this.enclavePublicKey = enclavePublicKey;
        this.workstationName = workstationName;

        byte[] hash = SHA256.digest(workstationPublicKey);
        DataInputStream bytes = new DataInputStream(new ByteArrayInputStream(hash));
        long msb = 0;
        long lsb = 0;
        try {
            msb = bytes.readLong();
            lsb = bytes.readLong();
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        }
        this.uuid = new UUID(msb, lsb);
    }

    public static Pairing generate(@NonNull byte[] workstationPublicKey, String workstationName) throws CryptoException {
        byte[] enclavePublicKey = new byte[Sodium.crypto_box_publickeybytes()];
        byte[] enclaveSecretKey = new byte[Sodium.crypto_box_secretkeybytes()];
        if (0 != Sodium.crypto_box_keypair(enclavePublicKey, enclaveSecretKey)) {
            throw new SodiumException("keypair generate failed");
        }

        return new Pairing(workstationPublicKey, enclaveSecretKey, enclavePublicKey, workstationName);
    }

    public static Pairing generate(PairingQR pairingQR) throws CryptoException {
        return Pairing.generate(pairingQR.workstationPublicKey, pairingQR.workstationName);
    }

    public byte[] wrapKey() throws CryptoException {
        byte[] ciphertext = new byte[enclavePublicKey.length + Sodium.crypto_box_sealbytes()];
        if (0 != Sodium.crypto_box_seal(ciphertext, enclavePublicKey, enclavePublicKey.length, workstationPublicKey)) {
            throw new SodiumException("crypto_box_seal failed");
        }
        return ciphertext;
    }

    public byte[] seal(byte[] message) throws CryptoException {
        byte[] nonce = SecureRandom.getSeed(Sodium.crypto_box_noncebytes());
        byte[] sealed = new byte[message.length + Sodium.crypto_box_macbytes()];
        if (0 != Sodium.crypto_box_easy(sealed, message, message.length, nonce, workstationPublicKey, enclaveSecretKey)) {
            throw new SodiumException("crypto_box_easy failed");
        }
        ByteArrayOutputStream nonceAndSealed = new ByteArrayOutputStream();
        try {
            nonceAndSealed.write(nonce);
            nonceAndSealed.write(sealed);
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        }
        return nonceAndSealed.toByteArray();
    }

    public byte[] unseal(byte[] sealed) throws CryptoException {
        if (sealed.length < Sodium.crypto_box_noncebytes() + Sodium.crypto_box_macbytes()) {
            throw new SodiumException("ciphertext shorter than nonce and mac");
        }
        byte[] nonce = Arrays.copyOfRange(sealed, 0, Sodium.crypto_box_noncebytes());
        byte[] ciphertext = Arrays.copyOfRange(sealed, Sodium.crypto_box_noncebytes(), sealed.length);
        byte[] unsealed = new byte[ciphertext.length - Sodium.crypto_box_macbytes()];
        if (0 != Sodium.crypto_box_open_easy(unsealed, ciphertext, ciphertext.length, nonce, workstationPublicKey, enclaveSecretKey)) {
            throw new SodiumException("crypto_box_open_easy failed");
        }
        return unsealed;
    }

    public String getUUIDString(){
        return uuid.toString().toUpperCase();
    }


        // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pairing pairing = (Pairing) o;

        if (!Arrays.equals(workstationPublicKey, pairing.workstationPublicKey)) return false;
        if (!Arrays.equals(enclavePublicKey, pairing.enclavePublicKey)) return false;
        return workstationName.equals(pairing.workstationName);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(workstationPublicKey);
        result = 31 * result + Arrays.hashCode(enclavePublicKey);
        result = 31 * result + workstationName.hashCode();
        return result;
    }
}
