package co.krypt.krypton.pgp.publickey;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import co.krypt.krypton.pgp.packet.MPInt;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class Ed25519PublicKeyData extends PublicKeyData {
    private static final byte[] CURVE_OID = new byte[]{(byte) 0x2B, 0x06, 0x01, 0x04, 0x01, (byte) 0xDA, 0x47, 0x0F, 0x01};
    private static final byte PUBLIC_KEY_PREFIX_BYTE = 0x40;

    public final MPInt q;
    //  strip prefix byte
    public final byte[] pk;

    private Ed25519PublicKeyData(MPInt q) {
        this.q = q;
        pk = Arrays.copyOfRange(q.body, 1, q.body.length);
    }

    public Ed25519PublicKeyData(byte[] pk) {
        ByteBuffer prefixedPKBuf = ByteBuffer.allocate(1 + pk.length);
        prefixedPKBuf.put(PUBLIC_KEY_PREFIX_BYTE).put(pk).flip();
        byte[] prefixedPK = prefixedPKBuf.array();
        this.q = new MPInt(prefixedPK);
        this.pk = pk;
    }

    public static Ed25519PublicKeyData parse(DataInputStream in) throws IOException, InvalidEd25519PublicKeyFormatException {
        int len = in.readUnsignedByte();
        if (len == 0 || len == 0xFF) {
            throw new InvalidEd25519PublicKeyFormatException("invalid oid length byte");
        }
        byte[] oidBuf = new byte[len];
        in.readFully(oidBuf);
        if (!Arrays.equals(oidBuf, CURVE_OID)) {
            throw new InvalidEd25519PublicKeyFormatException("wrong curve OID");
        }
        return new Ed25519PublicKeyData(
                MPInt.parse(in)
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeByte(CURVE_OID.length);
        out.write(CURVE_OID);
        q.serialize(out);
    }
}
