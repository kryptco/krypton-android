package co.krypt.krypton.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.AUTHENTICATION;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.CERTIFY_OTHER_KEYS;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.ENCRYPT_COMMUNICATIONS;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.ENCRYPT_STORAGE;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.POTENTIALLY_POSSESSED_BY_MULTIPLE;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.POTENTIALLY_SECRET_SHARED;
import static co.krypt.krypton.pgp.subpacket.KeyFlagsSubpacket.Flag.SIGN_DATA;

/**
 * Created by Kevin King on 6/13/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class KeyFlagsSubpacket extends Subpacket {

    public enum Flag {
        CERTIFY_OTHER_KEYS(0x01),
        SIGN_DATA(0x02),
        ENCRYPT_COMMUNICATIONS(0x04),
        ENCRYPT_STORAGE(0x08),
        POTENTIALLY_SECRET_SHARED(0x10),
        AUTHENTICATION(0x20),
        POTENTIALLY_POSSESSED_BY_MULTIPLE(0x80),
        ;
        private final int f;

        Flag(int f) {
            this.f = f;
        }
    }

    public final SubpacketHeader header;
    public final byte[] flags;

    public KeyFlagsSubpacket(SubpacketHeader header, byte[] flags) {
        this.header = header;
        this.flags = flags;
    }

    public boolean checkFirstByteFlag(Flag flag) {
        if (flags.length < 1) {
            return false;
        }
        return (flags[0] & flag.f) == flag.f;
    }

    public static KeyFlagsSubpacket parse(SubpacketHeader header, DataInputStream in) throws IOException {
        byte[] flags = new byte[(int) header.length.bodyLength()];
        in.readFully(flags);
        return new KeyFlagsSubpacket(header, flags);
    }

    public static KeyFlagsSubpacket withFlags(Flag[] flags) {
        byte f = 0x00;
        for (Flag flag: flags) {
            f |= flag.f;
        }
        return new KeyFlagsSubpacket(
                SubpacketHeader.fromLengthAndTypeIgnorable(
                        1,
                        SubpacketType.KEY_FLAGS
                ),
                new byte[]{f}
        );
    }

    public Flag[] toFlags() {
        List<Flag> flagList = new LinkedList<>();
        if (flags.length > 0) {
            for (Flag f : new Flag[]{
                    CERTIFY_OTHER_KEYS,
                    SIGN_DATA,
                    ENCRYPT_COMMUNICATIONS,
                    ENCRYPT_STORAGE,
                    POTENTIALLY_SECRET_SHARED,
                    AUTHENTICATION,
                    POTENTIALLY_POSSESSED_BY_MULTIPLE,
            }) {
                if ((flags[0] & f.f) == f.f) {
                    flagList.add(f);
                }
            }
        }
        return flagList.toArray(null);
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        header.serialize(out);
        out.write(flags);
    }
}
