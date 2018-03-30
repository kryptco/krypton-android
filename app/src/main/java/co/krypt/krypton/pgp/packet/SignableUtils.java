package co.krypt.krypton.pgp.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.PGPException;
import co.krypt.krypton.pgp.codesign.UnsignedBinaryDocument;

/**
 * Created by Kevin King on 6/15/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SignableUtils {
    public static byte[] signableBytes(Signable s) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream dataBuf = new DataOutputStream(buf);

        s.writeSignableData(dataBuf);
        dataBuf.close();
        return buf.toByteArray();
    }

    public static byte[] binarySignableBytes(BinarySignable s) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream dataBuf = new DataOutputStream(buf);

        s.writeSignableData(dataBuf);
        dataBuf.close();
        return buf.toByteArray();
    }

    public static int signableByteLength(Signable s) throws IOException {
        return signableBytes(s).length;
    }
    public static void writeLengthAndSignableData(Signable s, DataOutputStream out) throws IOException {
        out.writeShort(signableByteLength(s));
        s.writeSignableData(out);
    }

    public static byte[] hashSignable(HashAlgorithm hash, Signable s) throws IOException, NoSuchAlgorithmException {
        MessageDigest d = hash.digest();
        return d.digest(signableBytes(s));
    }

    public static short hashPrefix(HashAlgorithm hash, Signable s) throws IOException, NoSuchAlgorithmException {
        return new DataInputStream(new ByteArrayInputStream(hashSignable(hash, s))).readShort();
    }

    public static byte[] signBinaryDocument(BinarySignable bs, SSHKeyPairI key, HashAlgorithm hash) throws PGPException {
        try {
            UnsignedBinaryDocument unsignedBinary = new UnsignedBinaryDocument(
                    SignableUtils.binarySignableBytes(bs),
                    key,
                    hash
            );
            return unsignedBinary.sign(
                    key.pgpSign(hash, SignableUtils.signableBytes(unsignedBinary))
            ).serializedBytes();
        } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException | CryptoException | InvalidKeySpecException e) {
            throw new PGPException(e.getMessage(), e);
        }
    }
}
