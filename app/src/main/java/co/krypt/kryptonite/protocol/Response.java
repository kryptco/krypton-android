package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Response {
    @SerializedName("request_id")
    public String requestID;

    @SerializedName("me_response")
    public MeResponse meResponse;

    @SerializedName("sign_response")
    public SignResponse signResponse;

    @SerializedName("unpair_response")
    public UnpairResponse unpairResponse;

    @SerializedName("ack_response")
    public AckResponse ackResponse;

    @SerializedName("sns_endpoint_arn")
    public String snsEndpointARN;

    @SerializedName("tracking_id")
    public String trackingID;

    public static Response with(Request request) {
        Response response = new Response();
        response.requestID = request.requestID;
        return response;
    }
}
