package co.krypt.kryptonite;

import android.security.keystore.KeyInfo;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SSHKeyPair {
    private final @NonNull KeyPair keyPair;

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

    public byte[] signDigest(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (data.length != SHA256.BLOCK_SIZE) {
            throw new SignatureException("Invalid length of digest.");
        }
        Signature s = Signature.getInstance("NONEwithRSA");
        s.initSign(keyPair.getPrivate());
        s.update(data);
        return s.sign();
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
