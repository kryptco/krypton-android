package co.krypt.krypton.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

/**
 * Created by Kevin King on 2/19/18.
 * Copyright 2018. KryptCo, Inc.
 *
 * Serializes as the inner type on success, or as a tagged error on error,
 * e.g. {\"error\": \"error message here\"}.
 */
@JsonAdapter(SuccessOrTaggedErrorResult.Serializer.class)
public class SuccessOrTaggedErrorResult<T> {
    public T success;
    public String error;

    public static class Serializer<T> implements JsonSerializer<SuccessOrTaggedErrorResult<T>> {
        @Override
        public JsonElement serialize(SuccessOrTaggedErrorResult<T> src, Type typeOfSrc, JsonSerializationContext context) {
            if (src.success != null) {
                return context.serialize(src.success);
            }
            JsonObject o = new JsonObject();
            o.addProperty("error", src.error);
            return o;
        }
    }
}
