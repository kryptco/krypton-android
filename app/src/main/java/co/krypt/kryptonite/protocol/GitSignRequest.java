package co.krypt.kryptonite.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

import co.krypt.kryptonite.R;
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

    @Nullable
    public View fillView(ConstraintLayout container) {
        if (commit != null) {
            return commit.fillView(container, null, null);
        }
        if (tag != null) {
            return tag.fillView(container, null, null);
        }
        return null;
    }
    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        if (commit != null) {
            commit.fillRemoteViews(remoteViewsContainer, approved, signature);
        }

        if (tag != null) {
            tag.fillRemoteViews(remoteViewsContainer, approved, signature);
        }
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        if (commit != null) {
            commit.fillShortRemoteViews(remoteViewsContainer, approved, signature);
        }

        if (tag != null) {
            tag.fillShortRemoteViews(remoteViewsContainer, approved, signature);
        }
    }
}
