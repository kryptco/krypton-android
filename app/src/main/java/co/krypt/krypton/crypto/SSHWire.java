package co.krypt.krypton.crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 12/1/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SSHWire {
    public static byte[] encode(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        dout.writeInt(data.length);
        dout.write(data);
        return out.toByteArray();
    }
}
