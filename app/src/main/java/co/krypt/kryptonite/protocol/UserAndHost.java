package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 2/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class UserAndHost {
    @JSON.JsonRequired
    @SerializedName("user")
    public String user;

    @JSON.JsonRequired
    @SerializedName("host")
    public String host;
}
