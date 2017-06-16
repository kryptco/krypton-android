package co.krypt.kryptonite.pgp.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2016. KryptCo, Inc.
 */

public abstract class Serializable {
    public abstract void serialize(DataOutputStream out) throws IOException;

    public byte[] serializedBytes() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        serialize(out);
        out.close();
        return buf.toByteArray();
    }

    public long serializedByteLength() throws IOException {
        return serializedBytes().length;
    }

    public static <T extends Serializable> void serializeList(List<T> serializableList, DataOutputStream out) throws IOException {
        for (Serializable s: serializableList) {
            s.serialize(out);
        }
    }

    public static <T extends Serializable> long serializedByteLength(List<T> serializableList) throws IOException {
        long length = 0;
        for (Serializable s: serializableList) {
            length += s.serializedByteLength();
        }
        return length;
    }
}
