package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostsResponse {
    @Nullable
    @SerializedName("host_info")
    public HostInfo hostInfo;

    @Nullable
    @SerializedName("error")
    public String error;
}
