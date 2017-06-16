package co.krypt.kryptonite.crypto;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.PGPException;
import co.krypt.kryptonite.pgp.packet.HashAlgorithm;
import co.krypt.kryptonite.pgp.packet.Signature;
import co.krypt.kryptonite.pgp.publickey.PublicKeyData;
import co.krypt.kryptonite.pgp.publickey.PublicKeyPacketAttributes;

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

    boolean verifyDigest(String digest, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, InvalidKeySpecException, NoSuchProviderException;

    PublicKeyData pgpPublicKeyData();

    PublicKeyPacketAttributes pgpPublicKeyPacketAttributes();

    Signature pgpSign(HashAlgorithm hash, byte[] data) throws PGPException, NoSuchAlgorithmException, CryptoException, SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException;
}

