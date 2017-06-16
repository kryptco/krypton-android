package co.krypt.kryptonite.pgp.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import co.krypt.kryptonite.pgp.publickey.UnsupportedPublicKeyAlgorithmException;
import co.krypt.kryptonite.pgp.subpacket.DuplicateSubpacketException;
import co.krypt.kryptonite.pgp.subpacket.InvalidSubpacketLengthException;
import co.krypt.kryptonite.pgp.subpacket.UnsupportedCriticalSubpacketTypeException;

/**
 * Created by Kevin King on 6/12/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class SignatureAttributes extends Serializable implements Signable {
    public final SignatureAttributesWithoutHashPrefix attributes;
    public final short hashPrefix;

    public SignatureAttributes(SignatureAttributesWithoutHashPrefix attributes, short hashPrefix) {
        this.attributes = attributes;
        this.hashPrefix = hashPrefix;
    }

    public static SignatureAttributes parse(DataInputStream in) throws UnsupportedSignatureVersionException, IOException, UnsupportedPublicKeyAlgorithmException, InvalidSubpacketLengthException, UnsupportedCriticalSubpacketTypeException, UnsupportedHashAlgorithmException, NoSuchAlgorithmException, DuplicateSubpacketException {
        return new SignatureAttributes(
                SignatureAttributesWithoutHashPrefix.parse(in),
                in.readShort()
        );
    }

    @Override
    public void serialize(DataOutputStream out) throws IOException {
        attributes.serialize(out);
        out.writeShort(hashPrefix);
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        attributes.writeSignableData(out);
    }
}
