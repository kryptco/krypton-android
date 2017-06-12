package co.krypt.kryptonite.pgp.subpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.kryptonite.pgp.packet.Serializable;

/**
 * Created by Kevin King on 6/14/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SubpacketList extends Serializable {
    public final List<Subpacket> subpackets;
    @Nullable
    public final SignatureCreationTimeSubpacket created;
    @Nullable
    public final KeyExpirationSubpacket expires;
    @Nullable
    public final IssuerSubpacket issuer;
    @Nullable
    public final KeyFlagsSubpacket keyFlags;

    private SubpacketList(List<Subpacket> subpackets, SignatureCreationTimeSubpacket created, KeyExpirationSubpacket expires, IssuerSubpacket issuer, KeyFlagsSubpacket keyFlags) throws IOException, InvalidSubpacketLengthException {
        this.subpackets = subpackets;
        this.created = created;
        this.expires = expires;
        this.issuer = issuer;
        this.keyFlags = keyFlags;

        if (Serializable.serializedByteLength(subpackets) > Short.MAX_VALUE) {
            throw new InvalidSubpacketLengthException();
        }
    }

    public static SubpacketList fromList(List<Subpacket> subpackets) throws DuplicateSubpacketException, IOException, InvalidSubpacketLengthException {
        SignatureCreationTimeSubpacket created = null;
        KeyExpirationSubpacket expires = null;
        IssuerSubpacket issuer = null;
        KeyFlagsSubpacket keyFlags = null;

        for (Subpacket s: subpackets) {
            if (s instanceof SignatureCreationTimeSubpacket) {
                if (created != null) {
                    throw new DuplicateSubpacketException();
                }
                created = (SignatureCreationTimeSubpacket) s;
            }
            if (s instanceof KeyExpirationSubpacket) {
                if (expires != null) {
                    throw new DuplicateSubpacketException();
                }
                expires = (KeyExpirationSubpacket) s;
            }
            if (s instanceof IssuerSubpacket) {
                if (issuer != null) {
                    throw new DuplicateSubpacketException();
                }
                issuer = (IssuerSubpacket) s;
            }
            if (s instanceof KeyFlagsSubpacket) {
                if (keyFlags != null) {
                    throw new DuplicateSubpacketException();
                }
                keyFlags = (KeyFlagsSubpacket) s;
            }
        }
        return new SubpacketList(
                subpackets,
                created,
                expires,
                issuer,
                keyFlags
        );
    }

    public static SubpacketList parse(DataInputStream in) throws IOException, InvalidSubpacketLengthException, UnsupportedCriticalSubpacketTypeException, DuplicateSubpacketException {
        List<Subpacket> subpackets = new LinkedList<>();

        final int length = in.readUnsignedShort();
        int readLength = 0;
        while (readLength < length) {
            SubpacketHeader header = SubpacketHeader.parse(in);
            readLength += header.serializedByteLength();
            switch (header.type.type) {
                case UNKNOWN:
                    if (header.type.critical) {
                        throw new UnsupportedCriticalSubpacketTypeException();
                    }
                    subpackets.add(
                            UnparsedSubpacket.parse(header, in)
                    );
                    break;
                case CREATED:
                    subpackets.add(SignatureCreationTimeSubpacket.parse(header, in));
                    break;
                case KEY_EXPIRES:
                    subpackets.add(KeyExpirationSubpacket.parse(header, in));
                    break;
                case ISSUER:
                    subpackets.add(IssuerSubpacket.parse(header, in));
                    break;
                case KEY_FLAGS:
                    subpackets.add(KeyFlagsSubpacket.parse(header, in));
                    break;
            }
            readLength += header.length.bodyLength();
        }
        return fromList(subpackets);
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeShort((int) Serializable.serializedByteLength(subpackets));
        Serializable.serializeList(subpackets, out);
    }
}
