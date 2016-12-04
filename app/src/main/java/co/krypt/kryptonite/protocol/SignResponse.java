package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignResponse {
    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("error")
    public String error;
}
