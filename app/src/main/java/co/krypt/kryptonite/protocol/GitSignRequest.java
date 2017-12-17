package co.krypt.kryptonite.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.git.CommitInfo;
import co.krypt.kryptonite.git.TagInfo;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2017. KryptCo, Inc.
 */

public class GitSignRequest extends RequestBody {
    public static final String FIELD_NAME = "git_sign_request";

    @SerializedName("user_id")
    @JSON.JsonRequired
    public String userID;

    public GitSignRequestBody body;

    public String display() {
        return body.visit(new GitSignRequestBody.Visitor<String, RuntimeException>() {
            @Override
            public String visit(CommitInfo commit) {
                return commit.display();
            }

            @Override
            public String visit(TagInfo tag) {
                return tag.display();
            }
        });
    }

    public String title() {
        return body.visit(new GitSignRequestBody.Visitor<String, RuntimeException>() {
            @Override
            public String visit(CommitInfo commit) {
                return "Commit Signature";
            }

            @Override
            public String visit(TagInfo tag) {
                return "Tag Signature";
            }
        });
    }

    public String analyticsCategory() {
        return body.visit(new GitSignRequestBody.Visitor<String, RuntimeException>() {
            @Override
            public String visit(CommitInfo commit) {
                return "git-commit-signature";
            }

            @Override
            public String visit(TagInfo tag) {
                return "git-tag-signature";
            }
        });
    }

    @Nullable
    public View fillView(ConstraintLayout container) {
        return body.visit(new GitSignRequestBody.Visitor<View, RuntimeException>() {
            @Override
            public View visit(CommitInfo commit) {
                return commit.fillView(container, null, null);
            }

            @Override
            public View visit(TagInfo tag) {
                return tag.fillView(container, null, null);
            }
        });
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        body.visit(new GitSignRequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(CommitInfo commit) {
                commit.fillRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }

            @Override
            public Void visit(TagInfo tag) {
                tag.fillRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }
        });
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        body.visit(new GitSignRequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(CommitInfo commit) {
                commit.fillShortRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }

            @Override
            public Void visit(TagInfo tag) {
                tag.fillShortRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }
        });
    }

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public static class Serializer implements JsonSerializer<GitSignRequest> {
        @Override
        public JsonElement serialize(GitSignRequest src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement j = JSON.gson.toJsonTree(src);
            JsonObject o = j.getAsJsonObject();
            src.body.visit(new GitSignRequestBody.Visitor<Void, RuntimeException>() {
                @Override
                public Void visit(CommitInfo commit) {
                    o.add(CommitInfo.FIELD_NAME, context.serialize(commit));
                    return null;
                }

                @Override
                public Void visit(TagInfo tag) {
                    o.add(TagInfo.FIELD_NAME, context.serialize(tag));
                    return null;
                }
            });
            return o;
        }
    }

    public static class Deserializer implements JsonDeserializer<GitSignRequest> {
        @Override
        public GitSignRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            GitSignRequest request = JSON.gsonWithoutRequiredFields.fromJson(json, typeOfT);
            request.body = new GitSignRequestBody.Deserializer().deserialize(json, typeOfT, context);
            JSON.checkPojoRecursively(request);
            return request;
        }
    }

}
