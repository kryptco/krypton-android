package co.krypt.kryptonite.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import co.krypt.kryptonite.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

//  https://tools.ietf.org/html/rfc4880#section-5.2.3.1
public class SubpacketLength extends Serializable {
    private final long bodyLengthIncludingType;
    final byte[] bytes;

    private SubpacketLength(long bodyLengthIncludingType, byte[] bytes) {
        this.bodyLengthIncludingType = bodyLengthIncludingType;
        this.bytes = bytes;
    }

    public long bodyLength() {
        return bodyLengthIncludingType - 1;
    }

    public static SubpacketLength parse(DataInputStream in) throws IOException, InvalidSubpacketLengthException {
        int first = in.readUnsignedByte();
        if (first < 192) {
            return new SubpacketLength(first, new byte[]{(byte) first});
        }

        if (first >= 192 && first < 255) {
            int second = in.readUnsignedByte();
            return new SubpacketLength(((first - 192) << 8) + (second) + 192, new byte[]{(byte) first, (byte) second});
        }

        if (first == 255) {
            long bodyLength = in.readInt();
            ByteBuffer lengthBytesBuf = ByteBuffer.allocate(5).put((byte) first).putInt((int) bodyLength);
            lengthBytesBuf.flip();
            return new SubpacketLength(
                    bodyLength,
                    lengthBytesBuf.array()
            );
        }

        throw new InvalidSubpacketLengthException();
    }

    public static SubpacketLength fromBodyLength(long bodyLength) {
        long bodyLengthIncludingType = bodyLength + 1;
        if (bodyLengthIncludingType <= 191) {
            return new SubpacketLength(bodyLengthIncludingType, new byte[]{(byte)bodyLengthIncludingType});
        }
        if (bodyLengthIncludingType <= 8383) {
            long shiftedLength = bodyLengthIncludingType - 192;
            return new SubpacketLength(
                    bodyLengthIncludingType,
                    new byte[]{(byte) (shiftedLength/255 + 192), (byte) (shiftedLength % 255 )}
            );
        }
        ByteBuffer lengthBytesBuf = ByteBuffer.allocate(5).put((byte) 0xff).putInt((int) bodyLengthIncludingType);
        lengthBytesBuf.flip();
        return new SubpacketLength(
                bodyLengthIncludingType,
                lengthBytesBuf.array()
                );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.write(bytes);
    }
}
