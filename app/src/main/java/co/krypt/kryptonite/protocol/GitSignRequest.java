package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

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

    @Nullable
    @SerializedName("commit")
    public CommitInfo commit;

    @Nullable
    @SerializedName("tag")
    public TagInfo tag;

    public String display() {
        if (commit != null) {
            return commit.display();
        }
        if (tag != null) {
            return tag.display();
        }
        return "invalid git sign request";
    }

    public String title() {
        if (commit != null) {
            return "Commit Signature";
        }
        if (tag != null) {
            return "Tag Signature";
        }
        return "invalid git sign request";
    }
}
