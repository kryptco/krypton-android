package co.krypt.krypton.protocol;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

import co.krypt.krypton.git.CommitInfo;
import co.krypt.krypton.git.TagInfo;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2017. KryptCo, Inc.
 */

public abstract class GitSignRequestBody {
    public abstract <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E;

    public static abstract class Visitor<T, E extends Throwable> {
        public abstract T visit(CommitInfo commit) throws E;
        public abstract T visit(TagInfo tag) throws E;
    }

    public static class Deserializer implements JsonDeserializer<GitSignRequestBody> {
        @Override
        public GitSignRequestBody deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ArrayList<GitSignRequestBody> parsedVariants =  new ArrayList<>();
            if (!json.isJsonObject()) {
                throw new JsonParseException("expected JSON object");
            }
            JsonObject o = json.getAsJsonObject();
            if (o.has(CommitInfo.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(CommitInfo.FIELD_NAME), CommitInfo.class));
            }
            if (o.has(TagInfo.FIELD_NAME)) {
                parsedVariants.add(JSON.gson.fromJson(o.get(TagInfo.FIELD_NAME), TagInfo.class));
            }
            if (parsedVariants.size() != 1) {
                throw new JsonParseException("wrong number of enum variants: " + parsedVariants.size());
            }
            return parsedVariants.get(0);
        }
    }
}
