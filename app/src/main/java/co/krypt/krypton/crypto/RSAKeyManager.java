package co.krypt.krypton.crypto;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.security.ProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.PGPPublicKey;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class RSAKeyManager implements KeyManagerI {

    private final Object lock = new Object();

    public static String LOG_TAG = "krypton";
    private static final String SSH_KEYPAIR_CREATED_KEY = "SSH_KEY.created";
    private final SharedPreferences preferences;

    public RSAKeyManager(Context context) {
        preferences = context.getSharedPreferences("RSA_KEY_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public SSHKeyPairI loadOrGenerateKeyPair(String tag) throws CryptoException {
        synchronized (lock) {
            try {
                Long created;
                if (preferences.contains(SSH_KEYPAIR_CREATED_KEY + "." + tag)) {
                    created = preferences.getLong(SSH_KEYPAIR_CREATED_KEY + "." + tag, 0);
                } else {
                    created = PGPPublicKey.currentTimeBackdatedByClockSkewTolerance();
                    preferences.edit().putLong(SSH_KEYPAIR_CREATED_KEY + "." + tag, created).apply();
                }

                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.Entry privateKeyEntry;
                try {
                    //  Exception java.lang.NullPointerException: chain == null
                    //  java.security.KeyStore$PrivateKeyEntry.<init> (KeyStore.java:1206)
                    //  java.security.KeyStoreSpi.engineGetEntry (KeyStoreSpi.java:374)
                    //  java.security.KeyStore.getEntry (KeyStore.java:645)
                    //  co.krypt.krypton.crypto.RSAKeyManager.loadOrGenerateKeyPair (RSAKeyManager.java:58)
                    //  co.krypt.krypton.crypto.KeyManager.loadOrGenerateKeyPair (KeyManager.java:16)
                    //  co.krypt.krypton.onboarding.GenerateFragment$3.run (GenerateFragment.java:105)
                    //  java.lang.Thread.run (Thread.java:818)
                    privateKeyEntry = keyStore.getEntry(tag, null);
                } catch (NullPointerException npe) {
                    throw new CryptoException(npe.getMessage());
                }
                if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                    return new RSASSHKeyPair(
                            new KeyPair(((KeyStore.PrivateKeyEntry) privateKeyEntry).getCertificate().getPublicKey(), ((KeyStore.PrivateKeyEntry) privateKeyEntry).getPrivateKey()),
                            created
                    );
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
                return new RSASSHKeyPair(keyPair, created);
            } catch (CertificateException | UnsupportedOperationException | UnrecoverableEntryException | ProviderException | NoSuchProviderException | NoSuchAlgorithmException | KeyStoreException | IOException | InvalidAlgorithmParameterException e) {
                throw new CryptoException(e.getMessage());
            }
        }
    }

    /*
    For backwards compatibility testing
     */
    @Deprecated
    public SSHKeyPairI loadOrGenerateNoDigestKeyPair(String tag) throws CryptoException {
        synchronized (lock) {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
                if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                    return new RSASSHKeyPair(new KeyPair(((KeyStore.PrivateKeyEntry) privateKeyEntry).getCertificate().getPublicKey(), ((KeyStore.PrivateKeyEntry) privateKeyEntry).getPrivateKey()), 0);
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
                return new RSASSHKeyPair(keyPair, 0);
            } catch (CertificateException | UnsupportedOperationException | UnrecoverableEntryException | ProviderException | NoSuchProviderException | NoSuchAlgorithmException | KeyStoreException | IOException | InvalidAlgorithmParameterException e) {
                throw new CryptoException(e.getMessage());
            }
        }
    }

    public boolean keyExists(String tag) throws CryptoException {
        synchronized (lock) {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
                if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
                    return true;
                }
            } catch (CertificateException | IOException | KeyStoreException | ProviderException | NoSuchAlgorithmException | UnrecoverableEntryException | UnsupportedOperationException e) {
                throw new CryptoException(e.getMessage());
            }
            return false;
        }
    }

    public void deleteKeyPair(String tag) throws CryptoException {
        try {
            synchronized (lock) {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                keyStore.deleteEntry(tag);
            }
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            e.printStackTrace();
            throw new CryptoException(e);
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

}
