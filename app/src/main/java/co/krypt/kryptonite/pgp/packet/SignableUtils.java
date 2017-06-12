package co.krypt.kryptonite.pgp.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
