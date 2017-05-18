package co.krypt.kryptonite.crypto;

import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class RSASSHKeyPair implements SSHKeyPairI {
    private final @NonNull KeyPair keyPair;
    private static final String TAG = "SSHKeyPair";

    RSASSHKeyPair(@NonNull KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public String publicKeyDERBase64() {
        return Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);
    }

    public byte[] publicKeySSHWireFormat() throws InvalidKeyException, IOException {
        if (!(keyPair.getPublic() instanceof RSAPublicKey)) {
            throw new InvalidKeyException("Only RSA Supported");
        }
        RSAPublicKey rsaPub = (RSAPublicKey) keyPair.getPublic();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(SSHWire.encode("ssh-rsa".getBytes()));
        out.write(SSHWire.encode(rsaPub.getPublicExponent().toByteArray()));
        out.write(SSHWire.encode(rsaPub.getModulus().toByteArray()));

        return out.toByteArray();
    }

    public byte[] publicKeyFingerprint() throws IOException, InvalidKeyException, CryptoException {
        return SHA256.digest(publicKeySSHWireFormat());
    }

    public byte[] signDigest(String digest, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, NoSuchProviderException, InvalidKeySpecException {
        long start = System.currentTimeMillis();
        byte[] signature;
        Pair<Signature, byte[]> signerAndData = getSignerAndPrepareData(digest, data);
        Signature signer = signerAndData.first;
        data = signerAndData.second;

        signer.initSign(keyPair.getPrivate());
        signer.update(data);
        signature = signer.sign();
        long stop = System.currentTimeMillis();

        Log.d(TAG, "signature took " + String.valueOf((stop - start) / 1000.0) + " seconds");
        return signature;
    }

    public byte[] signDigestAppendingPubkey(byte[] data, String algo) throws SignatureException, IOException, InvalidKeyException, NoSuchAlgorithmException, CryptoException, NoSuchProviderException, InvalidKeySpecException {
        ByteArrayOutputStream dataWithPubkey = new ByteArrayOutputStream();
        dataWithPubkey.write(data);
        dataWithPubkey.write(SSHWire.encode(publicKeySSHWireFormat()));

        byte[] signaturePayload = dataWithPubkey.toByteArray();

        String digest = getDigestForAlgo(algo);
        return signDigest(digest, signaturePayload);
    }

    private String getDigestForAlgo(String algo) throws CryptoException {
        switch (algo) {
            case "ssh-rsa":
                return KeyProperties.DIGEST_SHA1;
            case "rsa-sha2-256":
                return KeyProperties.DIGEST_SHA256;
            case "rsa-sha2-512":
                return KeyProperties.DIGEST_SHA512;
            default:
                throw new CryptoException("unsuported algo: " + algo);
        }
    }

    public Pair<Signature, byte[]> getSignerAndPrepareData(String digest, byte[] data) throws CryptoException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeyFactory factory = KeyFactory.getInstance(keyPair.getPrivate().getAlgorithm(), "AndroidKeyStore");
        KeyInfo keyInfo;
        keyInfo = factory.getKeySpec(keyPair.getPrivate(), KeyInfo.class);

        Signature signer;
        if (Arrays.asList(keyInfo.getDigests()).contains(digest)) {
            switch (digest) {
                case KeyProperties.DIGEST_SHA1:
                    signer = Signature.getInstance("SHA1withRSA");
                    break;
                case KeyProperties.DIGEST_SHA256:
                    signer = Signature.getInstance("SHA256withRSA");
                    break;
                case KeyProperties.DIGEST_SHA512:
                    signer = Signature.getInstance("SHA512withRSA");
                    break;
                default:
                    throw new CryptoException("Unsupported digest: " + digest);
            }
        } else {
            //  fall back to NONEwithRSA for backwards compatibility
            signer = Signature.getInstance("NONEwithRSA");
            switch (digest) {
                case KeyProperties.DIGEST_SHA1:
                    data = SHA1.digestPrependingOID(data);
                    break;
                case KeyProperties.DIGEST_SHA256:
                    data = SHA256.digestPrependingOID(data);
                    break;
                case KeyProperties.DIGEST_SHA512:
                    data = SHA512.digestPrependingOID(data);
                    break;
                default:
                    throw new CryptoException("Unsupported digest: " + digest);
            }
        }
        return new Pair<>(signer, data);
    }

    public boolean verifyDigest(String digest, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, InvalidKeySpecException, NoSuchProviderException {
        Pair<Signature, byte[]> signerAndData = getSignerAndPrepareData(digest, data);
        Signature s = signerAndData.first;
        data = signerAndData.second;
        s.initVerify(keyPair.getPublic());
        s.update(data);
        return s.verify(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RSASSHKeyPair that = (RSASSHKeyPair) o;

        return publicKeyDERBase64().equals(that.publicKeyDERBase64());
    }

    public boolean isKeyStoredInSecureHardware() {
        try {
            KeyInfo keyInfo;
            KeyFactory factory = KeyFactory.getInstance(keyPair.getPrivate().getAlgorithm(), "AndroidKeyStore");
            keyInfo = factory.getKeySpec(keyPair.getPrivate(), KeyInfo.class);
            return keyInfo.isInsideSecureHardware();
        } catch (InvalidKeySpecException e) {
            // Not an Android KeyStore key.
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }
}
