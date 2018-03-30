package co.krypt.krypton.ssh;

import java.io.IOException;

import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.SSHWireDataParser;

/**
 * Created by Kevin King on 2/14/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Wire {
    public static String publicKeyBytesToAuthorizedKeysFormat(byte[] publicKeyBytes) throws IOException {
        SSHWireDataParser parser = new SSHWireDataParser(publicKeyBytes);
        String keyType = parser.popString();
        return keyType + " " + Base64.encode(publicKeyBytes);
    }
}
