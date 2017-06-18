package co.krypt.kryptonite.git;

import com.google.gson.annotations.SerializedName;

import java.io.DataOutputStream;
import java.io.IOException;

import co.krypt.kryptonite.pgp.packet.BinarySignable;
import co.krypt.kryptonite.protocol.JSON;

/**
 * Created by Kevin King on 6/17/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class TagInfo implements BinarySignable {
    @SerializedName("object")
    @JSON.JsonRequired
    public String object;

    @JSON.JsonRequired
    @SerializedName("type")
    public String type;

    @SerializedName("tag")
    @JSON.JsonRequired
    public String tag;

    @SerializedName("tagger")
    @JSON.JsonRequired
    public String tagger;

    @SerializedName("message")
    @JSON.JsonRequired
    public byte[] message;

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        out.write("object ".getBytes("UTF-8"));
        out.write(object.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        out.write("type ".getBytes("UTF-8"));
        out.write(type.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        out.write("tag ".getBytes("UTF-8"));
        out.write(tag.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        out.write("tagger ".getBytes("UTF-8"));
        out.write(tagger.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        out.write(message);
    }
}
