package co.krypt.kryptonite.crypto;

import android.security.keystore.KeyInfo;
import android.support.annotation.NonNull;
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

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SSHKeyPair {
    private final @NonNull KeyPair keyPair;
    private static final String TAG = "SSHKeyPair";

    SSHKeyPair(@NonNull KeyPair keyPair) {
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

    public byte[] signDigest(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        long start = System.currentTimeMillis();
        Signature s = Signature.getInstance("NONEwithRSA");
        s.initSign(keyPair.getPrivate());
        s.update(data);
        byte[] signature = s.sign();
        long stop = System.currentTimeMillis();
        Log.d(TAG, "signature took " + String.valueOf((stop - start) / 1000.0) + " seconds");
        return signature;
    }

    public byte[] signDigestAppendingPubkey(byte[] data) throws SignatureException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        ByteArrayOutputStream dataWithPubkey = new ByteArrayOutputStream();
        dataWithPubkey.write(data);
        dataWithPubkey.write(SSHWire.encode(publicKeySSHWireFormat()));

        byte[] signaturePayload = dataWithPubkey.toByteArray();

        return signDigest(signaturePayload);
    }

    public boolean verifyDigest(byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature s = Signature.getInstance("NONEwithRSA");
        s.initVerify(keyPair.getPublic());
        s.update(data);
        return s.verify(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SSHKeyPair that = (SSHKeyPair) o;

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
