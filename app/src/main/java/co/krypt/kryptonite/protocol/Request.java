package co.krypt.kryptonite.protocol;

import com.amazonaws.util.Base16;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Request {
    @SerializedName("request_id")
    @JSON.JsonRequired
    public String requestID;

    public String requestIDCacheKey() {
        String lowercaseBase16 = new String(Base16.encode(requestID.getBytes())).toLowerCase();
        return lowercaseBase16;
    }

    @SerializedName("unix_seconds")
    @JSON.JsonRequired
    public Long unixSeconds;

    @SerializedName("me_request")
    public MeRequest meRequest;

    @SerializedName("sign_request")
    public SignRequest signRequest;

    @SerializedName("unpair_request")
    public UnpairRequest unpairRequest;

    @SerializedName("a")
    public Boolean sendACK;
}
