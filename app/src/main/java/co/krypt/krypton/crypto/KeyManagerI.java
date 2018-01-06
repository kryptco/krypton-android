package co.krypt.krypton.crypto;

import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 5/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public interface KeyManagerI {
    SSHKeyPairI loadOrGenerateKeyPair(String tag) throws CryptoException;

    boolean keyExists(String tag) throws CryptoException;

    void deleteKeyPair(String tag) throws Exception;
}
