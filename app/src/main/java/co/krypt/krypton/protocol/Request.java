package co.krypt.krypton.protocol;

import android.support.constraint.ConstraintLayout;
import android.widget.RemoteViews;

import com.amazonaws.util.Base32;
import com.github.zafarkhaja.semver.Version;
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

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pairing.Pairing;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Request {
    @SerializedName("request_id")
    @JSON.JsonRequired
    public String requestID;

    public String requestIDCacheKey(Pairing pairing) throws CryptoException {
        return Base32.encodeAsString(SHA256.digest((pairing.getUUIDString().toLowerCase() + requestID).getBytes())).toLowerCase().replace("=", "-");
    }

    @SerializedName("v")
    @JSON.JsonRequired
    public String version;

    @SerializedName("unix_seconds")
    @JSON.JsonRequired
    public Long unixSeconds;


    @SerializedName("a")
    @Nullable
    public Boolean sendACK;

    public RequestBody body;

    public Version semVer() {
        try {
            return Version.valueOf(version);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return Version.valueOf("0.0.0");
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        body.visit(new RequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(MeRequest meRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(SignRequest signRequest) throws RuntimeException {
                signRequest.fillRemoteViews(remoteViewsContainer, unixSeconds, approved, signature);
                return null;
            }

            @Override
            public Void visit(GitSignRequest gitSignRequest) throws RuntimeException {
                gitSignRequest.fillRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }

            @Override
            public Void visit(UnpairRequest unpairRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(HostsRequest hostsRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                return null;
            }
        });
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        body.visit(new RequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(MeRequest meRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(SignRequest signRequest) throws RuntimeException {
                signRequest.fillShortRemoteViews(remoteViewsContainer, unixSeconds, approved, signature);
                return null;
            }

            @Override
            public Void visit(GitSignRequest gitSignRequest) throws RuntimeException {
                gitSignRequest.fillShortRemoteViews(remoteViewsContainer, approved, signature);
                return null;
            }

            @Override
            public Void visit(UnpairRequest unpairRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(HostsRequest hostsRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                return null;
            }
        });
    }

    public void fillView(ConstraintLayout content) {
        body.visit(new RequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(MeRequest meRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(SignRequest signRequest) throws RuntimeException {
                signRequest.fillView(content);
                return null;
            }

            @Override
            public Void visit(GitSignRequest gitSignRequest) throws RuntimeException {
                gitSignRequest.fillView(content);
                return null;
            }

            @Override
            public Void visit(UnpairRequest unpairRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(HostsRequest hostsRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                readTeamRequest.fillView(content);
                return null;
            }

            @Override
            public Void visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                logDecryptionRequest.fillView(content);
                return null;
            }

            @Override
            public Void visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                teamOperationRequest.fillView(content);
                return null;
            }
        });
    }

    public String analyticsCategory() {
        return body.visit(new RequestBody.Visitor<String, RuntimeException>() {
            @Override
            public String visit(MeRequest meRequest) throws RuntimeException {
                return "me";
            }

            @Override
            public String visit(SignRequest signRequest) throws RuntimeException {
                return "signature";
            }

            @Override
            public String visit(GitSignRequest gitSignRequest) throws RuntimeException {
                return gitSignRequest.analyticsCategory();
            }

            @Override
            public String visit(UnpairRequest unpairRequest) throws RuntimeException {
                return "unpair";
            }

            @Override
            public String visit(HostsRequest hostsRequest) throws RuntimeException {
                return "hosts";
            }

            @Override
            public String visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                return "read_team_request";
            }

            @Override
            public String visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                return "log_decryption_request";
            }

            @Override
            public String visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                return "team_operation_request";
            }
        });
    }

    public static class Serializer implements JsonSerializer<Request> {
        @Override
        public JsonElement serialize(Request src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement j = JSON.gson.toJsonTree(src);
            JsonObject o = j.getAsJsonObject();
            src.body.visit(new RequestBody.Visitor<Void, RuntimeException>() {
                @Override
                public Void visit(MeRequest meRequest) {
                    o.add(MeRequest.FIELD_NAME, context.serialize(meRequest));
                    return null;
                }

                @Override
                public Void visit(SignRequest signRequest) {
                    o.add(SignRequest.FIELD_NAME, context.serialize(signRequest));
                    return null;
                }

                @Override
                public Void visit(GitSignRequest gitSignRequest) {
                    o.add(GitSignRequest.FIELD_NAME, context.serialize(gitSignRequest));
                    return null;
                }

                @Override
                public Void visit(UnpairRequest unpairRequest) {
                    o.add(UnpairRequest.FIELD_NAME, context.serialize(unpairRequest));
                    return null;
                }

                @Override
                public Void visit(HostsRequest hostsRequest) {
                    o.add(HostsRequest.FIELD_NAME, context.serialize(hostsRequest));
                    return null;
                }

                @Override
                public Void visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                    o.add(ReadTeamRequest.FIELD_NAME, context.serialize(readTeamRequest));
                    return null;
                }

                @Override
                public Void visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                    o.add(LogDecryptionRequest.FIELD_NAME, context.serialize(logDecryptionRequest));
                    return null;
                }

                @Override
                public Void visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                    o.add(TeamOperationRequest.FIELD_NAME, context.serialize(teamOperationRequest));
                    return null;
                }
            });
            return o;
        }
    }

    public static class Deserializer implements JsonDeserializer<Request> {
        @Override
        public Request deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Request request = JSON.gsonWithoutRequiredFields.fromJson(json, typeOfT);
            request.body = new RequestBody.Deserializer().deserialize(json, typeOfT, context);
            JSON.checkPojoRecursively(request);
            return request;
        }
    }
}
