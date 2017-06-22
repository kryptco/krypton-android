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

import co.krypt.kryptonite.git.TagInfo;
import co.krypt.kryptonite.pairing.Pairing;

/**
 * Created by Kevin King on 12/15/16.
 * Copyright 2017. KryptCo, Inc.
 */
@DatabaseTable(tableName = "git_tag_signature_log")
public class GitTagSignatureLog implements Log {
    @DatabaseField(generatedId = true)
    private Long id;

    //  default true
    @SerializedName("allowed")
    @DatabaseField(columnName = "allowed")
    public Boolean allowed;


    @SerializedName("unix_seconds")
    @DatabaseField(columnName = "unix_seconds")
    public long unixSeconds;

    @SerializedName("object")
    @DatabaseField(columnName = "object")
    public String object;

    @SerializedName("type")
    @DatabaseField(columnName = "type")
    public String type;

    @SerializedName("tag")
    @DatabaseField(columnName = "tag")
    public String tag;

    @SerializedName("tagger")
    @DatabaseField(columnName = "tagger")
    public String tagger;

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

    public GitTagSignatureLog(Pairing pairing, TagInfo tag, @Nullable String signature) {
        this.allowed = signature != null;

        Long taggerTime = tag.taggerTime();
        if (taggerTime != null) {
            this.unixSeconds = taggerTime;
        } else {
            this.unixSeconds = System.currentTimeMillis() / 1000;
        }

        this.object = tag.object;
        this.type = tag.type;
        this.tag = tag.tag;
        this.tagger = tag.tagger;
        this.message = tag.message;
        this.messageString = tag.validatedMessageStringOrError();

        this.pairingUUID = pairing.getUUIDString();
        this.workstationName = pairing.workstationName;

        this.signature = signature;
    }

    protected GitTagSignatureLog() { }

    public boolean isAllowed() {
        return allowed == null || allowed;
    }

    public static List<GitTagSignatureLog> sortByTimeDescending(Set<GitTagSignatureLog> logs) {
        List<GitTagSignatureLog> sortedLogs = new ArrayList<>(logs);
        java.util.Collections.sort(sortedLogs, new Comparator<GitTagSignatureLog>() {
            @Override
            public int compare(GitTagSignatureLog lhs, GitTagSignatureLog rhs) {
                return Long.compare(rhs.unixSeconds, lhs.unixSeconds);
            }
        });
        return sortedLogs;
    }

    @Override
    public long unixSeconds() {
        return unixSeconds;
    }

    public TagInfo tagInfo() {
        return new TagInfo(
                object,
                type,
                tag,
                tagger,
                message
        );
    }

    private String header() {
        return (allowed ? "" : "rejected ") + "tag";
    }

    @Override
    public String shortDisplay() {
        return header() + ": " + tag.trim();
    }

    @Override
    public String longDisplay() {
        return header() + "\n" + tagInfo().display();
    }

    @Nullable
    @Override
    public View fillShortView(ConstraintLayout container) {
        container.removeAllViews();
        return tagInfo().fillShortView(container, signature != null, signature);
    }

    @Nullable
    @Override
    public View fillLongView(ConstraintLayout container) {
        container.removeAllViews();
        return tagInfo().fillView(container, signature != null, signature);
    }

    @Nullable
    @Override
    public String getSignature() {
        return signature;
    }

}
