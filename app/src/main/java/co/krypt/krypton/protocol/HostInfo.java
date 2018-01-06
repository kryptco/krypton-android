package co.krypt.krypton.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 2/18/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostInfo {
    @JSON.JsonRequired
    @SerializedName("pgp_user_ids")
    public String[] pgpUserIDs;

    @JSON.JsonRequired
    @SerializedName("hosts")
    public UserAndHost[] hosts;
}
