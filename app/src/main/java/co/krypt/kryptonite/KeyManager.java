package co.krypt.kryptonite;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
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

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyManager {

    public static String LOG_TAG = "kryptonite";

    public static String MY_RSA_KEY_TAG = "RSA.me";

    public static SSHKeyPair loadOrGenerateKeyPair(String tag) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, NoSuchProviderException, InvalidAlgorithmParameterException {
        // The key pair can also be obtained from the Android Keystore any time as follows:
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
                        .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setKeySize(4096)
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(60*60*3)
                        .build());
        long genStart = System.currentTimeMillis();
        keyPair = keyPairGenerator.generateKeyPair();
        long genStop = System.currentTimeMillis();
        Log.i(LOG_TAG, "KeyGen took " + String.valueOf((genStop - genStart)));
        return new SSHKeyPair(keyPair);
    }

    public static boolean keyExists(String tag) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException {

        // The key pair can also be obtained from the Android Keystore any time as follows:
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.Entry privateKeyEntry = keyStore.getEntry(tag, null);
        if (privateKeyEntry instanceof KeyStore.PrivateKeyEntry) {
            return true;
        }
        return false;
    }

    public static void deleteKeyPair(String tag) throws Exception {
        // The key pair can also be obtained from the Android Keystore any time as follows:
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
