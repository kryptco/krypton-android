package co.krypt.kryptonite;

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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.SodiumException;

public class Pairing {
    final byte[] workstationPublicKey;
    final byte[] symmetricSecretKey;
    final String workstationName;
    final UUID uuid;

    private static final int AES_256_KEY_LENGTH = 32;
    private static final int AES_256_IV_LENGTH = 16;
    private static final int AES_256_BLOCK_SIZE = 16;

    public Pairing(@NonNull byte[] workstationPublicKey, @NonNull byte[] symmetricSecretKey, String workstationName) throws CryptoException {
        if (workstationPublicKey.length != Sodium.crypto_box_publickeybytes()) {
            throw new SodiumException("workstation public key invalid");
        }
        this.workstationPublicKey = workstationPublicKey;
        if (symmetricSecretKey.length != AES_256_KEY_LENGTH) {
            throw new CryptoException("wrong AES key length");
        }
        this.symmetricSecretKey = symmetricSecretKey;
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
        byte[] symmetricKey = SecureRandom.getSeed(AES_256_KEY_LENGTH);
        return new Pairing(workstationPublicKey, symmetricKey, workstationName);
    }

    public static Pairing generate(PairingQR pairingQR) throws CryptoException {
        return Pairing.generate(pairingQR.workstationPublicKey, pairingQR.workstationName);
    }

    public byte[] wrapKey(byte[] symmetricKey) throws CryptoException {
        byte[] ciphertext = new byte[symmetricKey.length + Sodium.crypto_box_sealbytes()];
        if (0 != Sodium.crypto_box_seal(ciphertext, symmetricKey, symmetricKey.length, workstationPublicKey)) {
            throw new SodiumException("crypto_box_seal failed");
        }
        return ciphertext;
    }

    public byte[] seal(byte[] message) throws CryptoException {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(symmetricSecretKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(SecureRandom.getSeed(AES_256_IV_LENGTH));
            c.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] ciphertext = c.doFinal(message);
            ByteArrayOutputStream ivCiphertext = new ByteArrayOutputStream();
            ivCiphertext.write(iv.getIV());
            ivCiphertext.write(ciphertext);
            return ivCiphertext.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new CryptoException(e.getMessage());
        } catch (BadPaddingException e) {
            throw new CryptoException(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new CryptoException(e.getMessage());
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        }
    }

    public byte[] unseal(byte[] ivCiphertext) throws CryptoException {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(symmetricSecretKey, "AES");
            if (ivCiphertext.length < AES_256_IV_LENGTH + AES_256_BLOCK_SIZE) {
                throw new CryptoException("ivCiphertext shorter than IV");
            }
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(ivCiphertext, 0, AES_256_IV_LENGTH));
            byte[] ciphertext = Arrays.copyOfRange(ivCiphertext, AES_256_IV_LENGTH, ivCiphertext.length);
            c.init(Cipher.DECRYPT_MODE, key, iv);
            return c.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidKeyException e) {
            throw new CryptoException(e.getMessage());
        } catch (BadPaddingException e) {
            throw new CryptoException(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new CryptoException(e.getMessage());
        }

    }

    public UUID getUUID(){
        return uuid;
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("sodiumjni");
    }
}
