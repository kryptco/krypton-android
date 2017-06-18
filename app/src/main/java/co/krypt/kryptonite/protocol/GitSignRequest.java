package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

import co.krypt.kryptonite.git.CommitInfo;
import co.krypt.kryptonite.git.TagInfo;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2017. KryptCo, Inc.
 */

public class GitSignRequest {
    @SerializedName("user_id")
    @JSON.JsonRequired
    public String userID;

    @SerializedName("commit")
    public CommitInfo commit;

    @SerializedName("tag")
    public TagInfo tag;
}
