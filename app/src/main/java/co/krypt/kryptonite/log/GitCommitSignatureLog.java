package co.krypt.kryptonite.log;

import android.support.constraint.ConstraintLayout;
import android.view.View;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import co.krypt.kryptonite.git.CommitInfo;
import co.krypt.kryptonite.pairing.Pairing;

/**
 * Created by Kevin King on 12/15/16.
 * Copyright 2017. KryptCo, Inc.
 */
@DatabaseTable(tableName = "git_commit_signature_log")
public class GitCommitSignatureLog implements Log {
    @DatabaseField(generatedId = true)
    private Long id;

    //  default true
    @SerializedName("allowed")
    @DatabaseField(columnName = "allowed")
    public Boolean allowed;


    @SerializedName("unix_seconds")
    @DatabaseField(columnName = "unix_seconds")
    public long unixSeconds;

    @SerializedName("tree")
    @DatabaseField(columnName = "tree")
    @Nullable
    public String tree;

    @Nullable
    @SerializedName("parent")
    @DatabaseField(columnName = "parent")
    public String parent;

    @SerializedName("author")
    @DatabaseField(columnName = "author")
    public String author;

    @SerializedName("committer")
    @DatabaseField(columnName = "committer")
    public String committer;

    @SerializedName("message")
    @DatabaseField(columnName = "message", dataType = DataType.BYTE_ARRAY)
    public byte[] message;

    @SerializedName("message_string")
    @DatabaseField(columnName = "message_string")
    public String messageString;

    @SerializedName("pairing_uuid")
    @DatabaseField(columnName = "pairing_uuid", index = true)
    public String pairingUUID;

    @SerializedName("workstation_name")
    @DatabaseField(columnName = "workstation_name")
    public String workstationName;

    @SerializedName("signature")
    @DatabaseField(columnName = "signature")
    @Nullable
    public String signature;

    public GitCommitSignatureLog(Pairing pairing, CommitInfo commit, @Nullable String signature) {
        this.allowed = signature != null;

        Long committerTime = commit.committerTime();
        if (committerTime != null) {
            this.unixSeconds = committerTime;
        } else {
            this.unixSeconds = System.currentTimeMillis() / 1000;
        }

        this.tree = commit.tree;
        this.parent = commit.parent;
        this.author = commit.author;
        this.committer = commit.committer;
        this.messageString = commit.validatedMessageStringOrError();
        this.message = commit.message;

        this.pairingUUID = pairing.getUUIDString();
        this.workstationName = pairing.workstationName;

        this.signature = signature;
    }

    protected GitCommitSignatureLog() { }

    public boolean isAllowed() {
        return allowed == null || allowed;
    }

    public static List<GitCommitSignatureLog> sortByTimeDescending(Set<GitCommitSignatureLog> logs) {
        List<GitCommitSignatureLog> sortedLogs = new ArrayList<>(logs);
        java.util.Collections.sort(sortedLogs, new Comparator<GitCommitSignatureLog>() {
            @Override
            public int compare(GitCommitSignatureLog lhs, GitCommitSignatureLog rhs) {
                return Long.compare(rhs.unixSeconds, lhs.unixSeconds);
            }
        });
        return sortedLogs;
    }

    @Override
    public long unixSeconds() {
        return unixSeconds;
    }

    public CommitInfo commitInfo() {
        return new CommitInfo(
                tree,
                parent,
                author,
                committer,
                message
        );
    }

    private String header() {
        return (allowed ? "" : "rejected ") + "commit" +
                (signature != null ?
                        " " + commitInfo().shortHash(signature) :
                        ""
                );
    }

    @Override
    public String shortDisplay() {
        return header() + ": " + new String(message).replace("\n", " ").trim();
    }

    @Override
    public String longDisplay() {
        return header() + "\n" + commitInfo().display();
    }

    @Nullable
    @Override
    public View fillShortView(ConstraintLayout container) {
        container.removeAllViews();
        return commitInfo().fillShortView(container, signature != null, signature);
    }

    @Nullable
    @Override
    public View fillLongView(ConstraintLayout container) {
        container.removeAllViews();
        return commitInfo().fillView(container, signature != null, signature);
    }

    @Nullable
    @Override
    public String getSignature() {
        return signature;
    }
}
