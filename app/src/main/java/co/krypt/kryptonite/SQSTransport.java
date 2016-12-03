package co.krypt.kryptonite;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.util.Base64;

import co.krypt.kryptonite.exception.TransportException;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SQSTransport {
    private static String accessKey = "AKIAJMZJ3X6MHMXRF7QQ";
    private static String secretKey = "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";

    private static String sendQueueURL(String pairingUUID) {
        return "https://sqs.us-east-1.amazonaws.com/911777333295/" + pairingUUID;
    }

    private static String recvQueueURL(String pairingUUID) {
        return "https://sqs.us-east-1.amazonaws.com/911777333295/" + pairingUUID + "-responder";
    }

    public static void sendMessage(Pairing pairing, NetworkMessage message) throws TransportException {
        StaticCredentialsProvider creds = new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        AmazonSQSClient client = new AmazonSQSClient(creds);
        client.sendMessage(recvQueueURL(pairing.getUUID().toString().toUpperCase()), Base64.encodeAsString(message.bytes()));
    }
}
