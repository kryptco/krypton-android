package co.krypt.kryptonite.silo;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import co.krypt.kryptonite.JSON;
import co.krypt.kryptonite.KeyManager;
import co.krypt.kryptonite.NetworkMessage;
import co.krypt.kryptonite.Pairing;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.protocol.MeResponse;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.Response;
import co.krypt.kryptonite.transport.SQSTransport;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Silo {

    public static void handle(Pairing pairing, Request request) throws CertificateException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, UnrecoverableEntryException, IOException, InvalidKeyException, CryptoException, TransportException {
        Response response = Response.with(request);
        if (request.meRequest != null) {

            response.meResponse = new MeResponse(
                    new Profile(
                            "kevin@krypt.co",
                            KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG).publicKeySSHWireFormat()));
        }

        byte[] responseJson = JSON.toJson(response).getBytes();
        byte[] sealed = pairing.seal(responseJson);
        SQSTransport.sendMessage(pairing, new NetworkMessage(NetworkMessage.Header.CIPHERTEXT, sealed));
    }

}
