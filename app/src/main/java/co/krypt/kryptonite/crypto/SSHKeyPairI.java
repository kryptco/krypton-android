package co.krypt.kryptonite.crypto;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 5/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public interface SSHKeyPairI {

    String publicKeyDERBase64();

    byte[] publicKeySSHWireFormat() throws InvalidKeyException, IOException;

    byte[] publicKeyFingerprint() throws IOException, InvalidKeyException, CryptoException;

    byte[] signDigest(String digest, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, NoSuchProviderException, InvalidKeySpecException;

    byte[] signDigestAppendingPubkey(byte[] data, String algo) throws SignatureException, IOException, InvalidKeyException, NoSuchAlgorithmException, CryptoException, NoSuchProviderException, InvalidKeySpecException;

//    Pair<Signature, byte[]> getSignerAndPrepareData(String digest, byte[] data) throws CryptoException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;

    boolean verifyDigest(String digest, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, InvalidKeySpecException, NoSuchProviderException;
}

