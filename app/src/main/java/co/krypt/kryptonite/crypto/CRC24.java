package co.krypt.kryptonite.crypto;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2017. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-6.1
public class CRC24 {
    private static final int CRC24_INIT = (int) 0xB704CEL;
    private static final int CRC24_POLY = (int) 0x1864CFBL;
    public static int compute(byte[] data) {
        int crc24 = CRC24_INIT;
        for (int i = 0; i < data.length; i++) {
            crc24 ^= data[i] << 16;
            for (int j = 0; j < 8; j++){
                crc24 <<= 1;
                if ((crc24 & 0x01000000) != 0) {
                    crc24 ^= CRC24_POLY;
                }
            }
        }
        return crc24 & 0xFFFFFF;
    }

    public static byte[] computeBytes(byte[] data) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(compute(data)).flip();
        byte[] dst = new byte[4];
        buf.get(dst);
        return Arrays.copyOfRange(dst, 1, 4);
    }
}
