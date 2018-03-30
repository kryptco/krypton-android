package co.krypt.krypton.protocol;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.BuildConfig;
import co.krypt.krypton.team.Sigchain;

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

    @SerializedName("git_sign_response")
    public GitSignResponse gitSignResponse;

    @SerializedName("unpair_response")
    public UnpairResponse unpairResponse;

    @SerializedName("hosts_response")
    public HostsResponse hostsResponse;

    @SerializedName("read_team_response")
    public SuccessOrTaggedErrorResult<JsonObject> readTeamResponse;

    @SerializedName("log_decryption_response")
    public SuccessOrTaggedErrorResult<JsonObject> logDecryptionResponse;

    @SerializedName("team_operation_response")
    public SuccessOrTaggedErrorResult<Sigchain.TeamOperationResponse> teamOperationResponse;

    @SerializedName("ack_response")
    public AckResponse ackResponse;

    @SerializedName("sns_endpoint_arn")
    public String snsEndpointARN;

    @SerializedName("tracking_id")
    public String trackingID;

    @SerializedName("v")
    public String version;

    public static Response with(Request request) {
        Response response = new Response();
        response.requestID = request.requestID;
        response.version = BuildConfig.VERSION_NAME;
        return response;
    }
}
