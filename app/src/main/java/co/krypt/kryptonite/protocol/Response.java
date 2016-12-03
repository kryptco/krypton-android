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
}
