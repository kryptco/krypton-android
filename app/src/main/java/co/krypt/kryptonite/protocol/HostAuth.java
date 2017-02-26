package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 2/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostAuth {
    @SerializedName("host_key")
    public byte[] hostKey;

    @SerializedName("signature")
    public byte[] signature;

    @SerializedName("host_names")
    public String[] hostNames;
}
