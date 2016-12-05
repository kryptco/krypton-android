package co.krypt.kryptonite.transport;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;

import java.util.HashMap;

/**
 * Created by Kevin King on 12/5/16.
 * Copyright 2016. KryptCo, Inc.
 */
public class SNSTransport {
    private static final String TAG = "SNSTransport";
    private static final String PLATFORM_APPLICATION_ARN = "arn:aws:sns:us-east-1:911777333295:app/GCM/kryptonite-gcm";
    private Context context;
    private String token;
    private String endpointARN;
    private static String accessKey = "AKIAJMZJ3X6MHMXRF7QQ";
    private static String secretKey = "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";

    private static AmazonSNSClient getClient() {
        StaticCredentialsProvider creds = new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        return new AmazonSNSClient(creds);
    }

    private static SNSTransport ourInstance;

    private SNSTransport(Context context) {
        this.context = context;
        token = getToken();
        endpointARN = getEndpointARN();
    }

    public static synchronized SNSTransport getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new SNSTransport(context);
        }
        return ourInstance;
    }

    private synchronized SharedPreferences preferences() {
        return context.getSharedPreferences("SNS_TRANSPORT_PREFERENCES", Context.MODE_PRIVATE);
    }

    private synchronized String getToken() {
        return preferences().getString("TOKEN", null);
    }

    private synchronized void setToken(String token) {
        preferences().edit().putString("TOKEN", token).apply();
        this.token = token;
    }

    public synchronized String getEndpointARN() {
        return preferences().getString("ENDPOINT_ARN", null);
    }

    private synchronized void setEndpointARN(String arn) {
        Log.i(TAG, "set endpoint ARN: " + arn);
        preferences().edit().putString("ENDPOINT_ARN", arn).apply();
        this.endpointARN = arn;
    }

    public synchronized void setDeviceToken(String token) {
        Log.i(TAG, "refreshed device token: " + token);
        setToken(token);
        registerWithAWS();
    }

    public synchronized void registerWithAWS() {
        AmazonSNSClient client = getClient();
        CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(PLATFORM_APPLICATION_ARN)
                .withToken(token);
        CreatePlatformEndpointResult result = client.createPlatformEndpoint(request);
        setEndpointARN(result.getEndpointArn());

        HashMap<String, String> enabledAttribute = new HashMap<>();
        enabledAttribute.put("Enabled", "true");
        SetEndpointAttributesRequest enableRequest = new SetEndpointAttributesRequest()
                .withEndpointArn(endpointARN)
                .withAttributes(enabledAttribute);
        client.setEndpointAttributes(enableRequest);
    }
}
