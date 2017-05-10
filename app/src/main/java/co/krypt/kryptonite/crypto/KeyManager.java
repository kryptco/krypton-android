package co.krypt.kryptonite.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

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
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyManager {

    public static String LOG_TAG = "kryptonite";

    public static String MY_RSA_KEY_TAG = "RSA.me";

    public static synchronized SSHKeyPair loadOrGenerateKeyPair(String tag) throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
            if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                return new SSHKeyPair(new KeyPair(((KeyStore.PrivateKeyEntry) privateKeyEntry).getCertificate().getPublicKey(), ((KeyStore.PrivateKeyEntry) privateKeyEntry).getPrivateKey()));
            } else {
                Log.w(LOG_TAG, "Not an instance of a PrivateKeyEntry");
            }


            KeyPair keyPair = null;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            tag, KeyProperties.PURPOSE_SIGN)
                            .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                            .setKeySize(3072)
                            .setUserAuthenticationRequired(false)
                            .build());
            long genStart = System.currentTimeMillis();
            keyPair = keyPairGenerator.generateKeyPair();
            long genStop = System.currentTimeMillis();
            Log.i(LOG_TAG, "KeyGen took " + String.valueOf((genStop - genStart)));
            return new SSHKeyPair(keyPair);
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        } catch (CertificateException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        } catch (UnrecoverableEntryException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchProviderException e) {
            throw new CryptoException(e.getMessage());
        } catch (KeyStoreException e) {
            throw new CryptoException(e.getMessage());
        }
    }

    /*
    For backwards compatibility testing
     */
    @Deprecated
    public static synchronized SSHKeyPair loadOrGenerateNoDigestKeyPair(String tag) throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
            if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                return new SSHKeyPair(new KeyPair(((KeyStore.PrivateKeyEntry) privateKeyEntry).getCertificate().getPublicKey(), ((KeyStore.PrivateKeyEntry) privateKeyEntry).getPrivateKey()));
            } else {
                Log.w(LOG_TAG, "Not an instance of a PrivateKeyEntry");
            }


            KeyPair keyPair = null;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            tag, KeyProperties.PURPOSE_SIGN)
                            .setDigests(KeyProperties.DIGEST_NONE)
                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                            .setKeySize(3072)
                            .setUserAuthenticationRequired(false)
                            .build());
            long genStart = System.currentTimeMillis();
            keyPair = keyPairGenerator.generateKeyPair();
            long genStop = System.currentTimeMillis();
            Log.i(LOG_TAG, "KeyGen took " + String.valueOf((genStop - genStart)));
            return new SSHKeyPair(keyPair);
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        } catch (CertificateException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        } catch (UnrecoverableEntryException e) {
            throw new CryptoException(e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchProviderException e) {
            throw new CryptoException(e.getMessage());
        } catch (KeyStoreException e) {
            throw new CryptoException(e.getMessage());
        }
    }

    public static synchronized boolean keyExists(String tag) throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
            if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                return true;
            }
        } catch (IOException e) {
            throw new CryptoException(e.getMessage());
        } catch (CertificateException e) {
            throw new CryptoException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e.getMessage());
        } catch (UnrecoverableEntryException e) {
            throw new CryptoException(e.getMessage());
        } catch (KeyStoreException e) {
            throw new CryptoException(e.getMessage());
        }
        return false;
    }

    public static synchronized void deleteKeyPair(String tag) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(tag);
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

}
