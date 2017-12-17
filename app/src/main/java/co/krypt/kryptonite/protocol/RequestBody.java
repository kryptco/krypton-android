package co.krypt.kryptonite.protocol;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by Kevin King on 12/16/17.
 * Copyright 2017. KryptCo, Inc.
 */

public abstract class RequestBody {
    public abstract <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E;

    public static abstract class Visitor<T, E extends Throwable> {
        public abstract T visit(MeRequest meRequest) throws E;
        public abstract T visit(SignRequest signRequest) throws E;
        public abstract T visit(GitSignRequest gitSignRequest) throws E;
        public abstract T visit(UnpairRequest unpairRequest) throws E;
        public abstract T visit(HostsRequest hostsRequest) throws E;
    }

    public static class Deserializer implements JsonDeserializer<RequestBody> {
        @Override
        public RequestBody deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ArrayList<RequestBody> parsedVariants =  new ArrayList<>();
            if (!json.isJsonObject()) {
                throw new JsonParseException("expected JSON object");
            }
            JsonObject o = json.getAsJsonObject();
            if (o.has(MeRequest.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(MeRequest.FIELD_NAME), MeRequest.class));
            }
            if (o.has(SignRequest.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(SignRequest.FIELD_NAME), SignRequest.class));
            }
            if (o.has(GitSignRequest.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(GitSignRequest.FIELD_NAME), GitSignRequest.class));
            }
            if (o.has(UnpairRequest.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(UnpairRequest.FIELD_NAME), UnpairRequest.class));
            }
            if (o.has(HostsRequest.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(HostsRequest.FIELD_NAME), HostsRequest.class));
            }
            if (parsedVariants.size() != 1) {
                throw new JsonParseException("wrong number of enum variants: " + parsedVariants.size());
            }
            return parsedVariants.get(0);
        }
    }
}
