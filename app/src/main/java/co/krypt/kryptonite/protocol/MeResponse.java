package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class MeResponse {
    @SerializedName("me")
    public Profile me;

    public MeResponse() {}
    public MeResponse(Profile me) {
        this.me = me;
    }
}
