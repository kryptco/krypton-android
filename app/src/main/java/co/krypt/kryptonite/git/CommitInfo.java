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

public class CommitInfo implements BinarySignable {
    @SerializedName("tree")
    @JSON.JsonRequired
    public String tree;

    @Nullable
    @SerializedName("parent")
    public String parent;

    @SerializedName("author")
    @JSON.JsonRequired
    public String author;

    @SerializedName("committer")
    @JSON.JsonRequired
    public String committer;

    @SerializedName("message")
    @JSON.JsonRequired
    public byte[] message;

    public CommitInfo(String tree, @Nullable String parent, String author, String committer, byte[] message) {
        this.tree = tree;
        this.parent = parent;
        this.author = author;
        this.committer = committer;
        this.message = message;
    }

    @Override
    public void writeSignableData(DataOutputStream out) throws IOException {
        out.write("tree ".getBytes("UTF-8"));
        out.write(tree.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        if (parent != null) {
            out.write("parent ".getBytes("UTF-8"));
            out.write(parent.getBytes("UTF-8"));
            out.write("\n".getBytes("UTF-8"));
        }

        out.write("author ".getBytes("UTF-8"));
        out.write(author.getBytes("UTF-8"));
        out.write("\n".getBytes("UTF-8"));

        out.write("committer ".getBytes("UTF-8"));
        out.write(committer.getBytes());
        out.write("\n".getBytes("UTF-8"));

        out.write(message);
    }

    public String display() {
        StringBuilder s = new StringBuilder();

        s.append(new String(message).trim()).append("\n");

        s.append("t ").append(tree).append("\n");
        s.append("p ").append(parent).append("\n");

        String authorNameAndEmail = authorNameAndEmail();
        String committerNameAndEmail = committerNameAndEmail();

        if (authorNameAndEmail == null) {
            s.append("a ").append(this.author).append("\n");
        } else {
            if (!authorNameAndEmail.equals(committerNameAndEmail)) {
                s.append("a ").append(authorNameAndEmail).append("\n");
            }
        }

        if (committerNameAndEmail == null) {
            s.append("c ").append(this.committer).append("\n");
        } else {
            s.append("c ").append(committerNameAndEmail).append("\n");

            String committerTime = GitUtils.getTimeAfterEmail(this.committer);
            if (committerTime != null) {
                s.append(committerTime);
            } else {
                s.append("invalid time");
            }
        }

        return s.toString();
    }

    @Nullable public String authorNameAndEmail() {
        return GitUtils.getNameAndEmail(author);
    }

    @Nullable public String committerNameAndEmail() {
        return GitUtils.getNameAndEmail(committer);
    }

}
