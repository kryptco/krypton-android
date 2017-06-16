package co.krypt.kryptonite.pgp;

import java.io.IOException;
import java.util.List;

import co.krypt.kryptonite.crypto.SSHKeyPairI;

/**
 * Created by Kevin King on 6/16/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class PGPManager {
    public static byte[] publicKeyWithIdentities(SSHKeyPairI kp, List<UserID> userIDs) throws PGPException, IOException {
        return new PGPPublicKey(kp, userIDs).serializedBytes();
    }
}
