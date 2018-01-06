package co.krypt.krypton.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import co.krypt.krypton.exception.TransportException;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class NetworkMessage {
    public final Header header;
    public final byte[] message;

    public enum Header {
        CIPHERTEXT((byte) 0),
        WRAPPED_KEY((byte) 1),
        WRAPPED_PUBLIC_KEY((byte) 2);

        private final byte value;

        Header(final byte newValue) {
            value = newValue;
        }

        public byte getValue() { return value; }
    }

    public NetworkMessage(Header header, byte[] message) {
        this.header = header;
        this.message = message;
    }

    public static NetworkMessage parse(byte[] incoming) throws TransportException {
        if (incoming.length == 0) {
            throw new TransportException("empty incoming network message");
        }
        byte[] message = Arrays.copyOfRange(incoming, 1, incoming.length);
        for (Header h : Header.values()) {
            if (h.getValue() == incoming[0]) {
                return new NetworkMessage(h, message);
            }
        }
        throw new TransportException("unknown message header");
    }

    public byte[] bytes() throws TransportException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.getValue());
        try {
            out.write(message);
        } catch (IOException e) {
            throw new TransportException(e.getMessage());
        }
        return out.toByteArray();
    }

}
