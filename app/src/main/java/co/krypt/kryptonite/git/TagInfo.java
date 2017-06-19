package co.krypt.kryptonite.git;

import com.google.gson.annotations.SerializedName;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

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

    public TagInfo(String object, String type, String tag, String tagger, byte[] message) {
        this.object = object;
        this.type = type;
        this.tag = tag;
        this.tagger = tagger;
        this.message = message;
    }

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

    public String display() {
        StringBuilder s = new StringBuilder();

        if (message.length > 0) {
            s.append(validatedMessageStringOrError().trim()).append("\n");
        }

        s.append("TAG ").append(tag).append("\n");
        s.append("HASH ").append(object).append("\n");
        if (!type.equals("commit")) {
            s.append("TYPE ").append(type).append("\n");
        }

        String taggerNameAndEmail = taggerNameAndEmail();

        if (taggerNameAndEmail == null) {
            s.append("TAGGER ").append(this.tagger).append("\n");
        } else {
            s.append("TAGGER ").append(taggerNameAndEmail).append("\n");

            String committerTime = GitUtils.getTimeAfterEmail(this.tagger);
            if (committerTime != null) {
                s.append(committerTime);
            } else {
                s.append("invalid time");
            }
        }

        return s.toString();
    }

    public String validatedMessageStringOrError() {
        return GitUtils.validatedStringOrPrefixError(message, "invalid message encoding");
    }

    @Nullable
    public String taggerNameAndEmail() {
        return GitUtils.getNameAndEmail(tagger);
    }

    @Nullable
    public Long taggerTime() {
        return GitUtils.getUnixSecondsAfterEmail(tagger);
    }
}
