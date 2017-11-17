package co.krypt.kryptonite.protocol;

import com.amazonaws.util.Base64;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class JSON {
    public static <T> T fromJson(byte[] json, Class<T> classOfT) throws JsonSyntaxException {
        try {
            return gson.fromJson(new String(json, "UTF-8"), classOfT);
        } catch (UnsupportedEncodingException e) {
            throw new JsonSyntaxException(e.getMessage());
        }
    }

    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return gson.fromJson(json, classOfT);
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .registerTypeAdapter(Request.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(MeRequest.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(SignRequest.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(UnpairRequest.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(HostsRequest.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(GitSignRequest.class, new AnnotatedDeserializer<>())
            .registerTypeAdapter(HostAuth.class, new AnnotatedDeserializer<>())
            .create();

    private static final Gson gsonWithoutRequiredFields = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .create();

    // Using Android's base64 libraries. This can be replaced with any base64 library.
    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.encodeAsString(src));
        }
    }

    //  from https://stackoverflow.com/questions/21626690/gson-optional-and-required-fields
    private static class AnnotatedDeserializer<T> implements JsonDeserializer<T>
    {

        public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException
        {
            T pojo = gsonWithoutRequiredFields.fromJson(je, type);
            checkPojoRecursively(pojo);
            return pojo;
        }

        private void checkPojoRecursively(Object pojo) {
            Field[] fields = pojo.getClass().getDeclaredFields();
            for (Field f : fields)
            {
                if (f.getAnnotation(JsonRequired.class) != null)
                {
                    try
                    {
                        f.setAccessible(true);
                        if (f.get(pojo) == null)
                        {
                            throw new JsonParseException("Missing field in JSON: " + f.getName());
                        }
                    }
                    catch (IllegalArgumentException ex)
                    {
                        Logger.getLogger(AnnotatedDeserializer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    catch (IllegalAccessException ex)
                    {
                        Logger.getLogger(AnnotatedDeserializer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                try {
                    f.setAccessible(true);
                    if (f.get(pojo) != null && f.getAnnotation(SerializedName.class) != null) {
                        checkPojoRecursively(f.get(pojo));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface JsonRequired
    {
    }
}
