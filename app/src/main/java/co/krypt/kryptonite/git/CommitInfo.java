package co.krypt.kryptonite.git;

import com.amazonaws.util.Base16;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import co.krypt.kryptonite.crypto.SHA1;
import co.krypt.kryptonite.exception.CryptoException;
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

    @Nullable
    public Long committerTime() {
        return GitUtils.getUnixSecondsAfterEmail(committer);
    }

    public String display() {
        StringBuilder s = new StringBuilder();

        s.append(validatedMessageStringOrError().trim()).append("\n");

        s.append("TREE ").append(tree).append("\n");
        s.append("PARENT ").append(parent).append("\n");

        String authorNameAndEmail = authorNameAndEmail();
        String committerNameAndEmail = committerNameAndEmail();

        if (authorNameAndEmail == null) {
            s.append("AUTHOR ").append(this.author).append("\n");
        } else {
            if (!authorNameAndEmail.equals(committerNameAndEmail)) {
                s.append("AUTHOR ").append(authorNameAndEmail).append("\n");
            }
        }

        if (committerNameAndEmail == null) {
            s.append("COMMITTER ").append(this.committer).append("\n");
        } else {
            s.append("COMMITTER ").append(committerNameAndEmail).append("\n");

            String committerTime = GitUtils.getTimeAfterEmail(this.committer);
            if (committerTime != null) {
                s.append(committerTime);
            } else {
                s.append("invalid time");
            }
        }

        return s.toString();
    }

    public String shortHash(String asciiArmorSignature) {
        try {
            byte[] hash = commitHash(asciiArmorSignature);
            String hashHex = Base16.encodeAsString(hash).toLowerCase();
            if (hashHex.length() < 7) {
                return "";
            }
            return hashHex.substring(0, 7);
        } catch (IOException | CryptoException e) {
            e.printStackTrace();
            return "";
        }

    }

    public byte[] commitHash(String asciiArmorSignature) throws IOException, CryptoException {
        ByteArrayOutputStream commitBuf = new ByteArrayOutputStream();
        DataOutputStream commitDataBuf = new DataOutputStream(commitBuf);
        commitDataBuf.write("tree ".getBytes("UTF-8"));
        commitDataBuf.write(tree.getBytes("UTF-8"));
        commitDataBuf.write("\n".getBytes("UTF-8"));

        if (parent != null) {
            commitDataBuf.write("parent ".getBytes("UTF-8"));
            commitDataBuf.write(parent.getBytes("UTF-8"));
            commitDataBuf.write("\n".getBytes("UTF-8"));
        }

        commitDataBuf.write("author ".getBytes("UTF-8"));
        commitDataBuf.write(author.getBytes("UTF-8"));
        commitDataBuf.write("\n".getBytes("UTF-8"));

        commitDataBuf.write("committer ".getBytes("UTF-8"));
        commitDataBuf.write(committer.getBytes());
        commitDataBuf.write("\n".getBytes("UTF-8"));

        commitDataBuf.write("gpgsig ".getBytes("UTF-8"));
        commitDataBuf.write(asciiArmorSignature.replaceAll("\n", "\n ").getBytes("UTF-8"));
        commitDataBuf.write("\n".getBytes("UTF-8"));

        commitDataBuf.write(message);

        commitDataBuf.close();

        ByteArrayOutputStream fullBuf = new ByteArrayOutputStream();
        DataOutputStream fullDataBuf = new DataOutputStream(fullBuf);

        fullDataBuf.write("commit ".getBytes("UTF-8"));
        fullDataBuf.write(String.valueOf(commitBuf.toByteArray().length).getBytes("UTF-8"));
        fullDataBuf.writeByte(0x00);
        fullDataBuf.write(commitBuf.toByteArray());

        fullDataBuf.close();

        return SHA1.digest(fullBuf.toByteArray());
    }

    public String validatedMessageStringOrError() {
        return GitUtils.validatedStringOrPrefixError(message, "invalid message encoding");
    }

    @Nullable public String authorNameAndEmail() {
        return GitUtils.getNameAndEmail(author);
    }

    @Nullable public String committerNameAndEmail() {
        return GitUtils.getNameAndEmail(committer);
    }

}
