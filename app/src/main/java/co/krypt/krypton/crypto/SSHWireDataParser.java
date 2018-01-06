package co.krypt.krypton.crypto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Kevin King on 5/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class SSHWireDataParser {
    private final DataInputStream dataInputStream;
    public SSHWireDataParser(byte[] bytes) {
        dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    public byte popByte() throws IOException {
        return (byte) dataInputStream.read();
    }

    public boolean popBoolean() throws IOException {
        return dataInputStream.readBoolean();
    }

    public byte[] popByteArray() throws IOException {
        int len = dataInputStream.readInt();
        byte[] buf = new byte[len];
        dataInputStream.readFully(buf);
        return buf;
    }

    public String popString() throws IOException {
        byte[] bytes = popByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
