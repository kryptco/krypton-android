package co.krypt.kryptonite.git;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pgp.packet.BinarySignable;
import co.krypt.kryptonite.protocol.JSON;

import static android.view.View.GONE;

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

    public View fillView(ConstraintLayout container, @Nullable Boolean approved, @Nullable String signature) {
        View tagView = View.inflate(container.getContext(), R.layout.tag, container);

        String taggerNameAndEmail = taggerNameAndEmail();
        TextView taggerText = (TextView) tagView.findViewById(R.id.tagger);
        if (taggerNameAndEmail == null) {
            taggerText.setText(tagger);
        } else  {
            taggerText.setText(taggerNameAndEmail);
        }

        TextView typeText = (TextView) tagView.findViewById(R.id.type);
        if (!type.equals("commit")) {
            typeText.setText(type);
        } else {
            typeText.setText("");
            typeText.setVisibility(GONE);
            tagView.findViewById(R.id.typeLabel).setVisibility(GONE);
        }

        TextView objectText = (TextView) tagView.findViewById(R.id.object);
        objectText.setText(object);
        TextView tagText = (TextView) tagView.findViewById(R.id.tag);
        tagText.setText(tag);
        if (approved != null && !approved) {
            tagText.setBackgroundResource(R.drawable.hash_red_bg);
            ImageView tagImage = (ImageView) tagView.findViewById(R.id.tagImage);
            tagImage.setImageResource(R.drawable.tag_hires_red);
        }
        TextView messageText = (TextView) tagView.findViewById(R.id.message);
        messageText.setText(validatedMessageStringOrError());

        TextView timeText = (TextView) tagView.findViewById(R.id.time);
        String taggerTime = GitUtils.getTimeAfterEmail(tagger);
        if (taggerTime != null) {
            timeText.setText(taggerTime);
        } else {
            timeText.setText("invalid time");
        }

        return tagView;
    }

    public View fillShortView(ConstraintLayout container, @Nullable Boolean approved, @Nullable String signature) {
        View tagView = View.inflate(container.getContext(), R.layout.tag_short, container);

        TextView tagText = (TextView) tagView.findViewById(R.id.tag);
        tagText.setText(tag);
        if (approved != null && !approved) {
            tagText.setBackgroundResource(R.drawable.hash_red_bg);
            ImageView tagImage = (ImageView) tagView.findViewById(R.id.tagImage);
            tagImage.setImageResource(R.drawable.tag_hires_red);
        }
        TextView messageText = (TextView) tagView.findViewById(R.id.message);
        messageText.setText(validatedMessageStringOrError().replaceAll("\n", " ").trim());

        TextView timeText = (TextView) tagView.findViewById(R.id.time);
        String taggerTime = GitUtils.getTimeAfterEmail(tagger);
        if (taggerTime != null) {
            timeText.setText(taggerTime);
        } else {
            timeText.setText("invalid time");
        }
        return tagView;
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        RemoteViews remoteViews = new RemoteViews(remoteViewsContainer.getPackage(), R.layout.tag_remote);
        remoteViewsContainer.addView(R.id.content, remoteViews);

        String taggerNameAndEmail = taggerNameAndEmail();
        if (taggerNameAndEmail == null) {
            remoteViews.setTextViewText(R.id.tagger, tagger);
        } else  {
            remoteViews.setTextViewText(R.id.tagger, taggerNameAndEmail);
        }

        if (!type.equals("commit")) {
            remoteViews.setTextViewText(R.id.type, type);
        } else {
            remoteViews.setTextViewText(R.id.type, "");
            remoteViews.setViewVisibility(R.id.type, GONE);
            remoteViews.setViewVisibility(R.id.typeLabel, GONE);
        }

        remoteViews.setTextViewText(R.id.object, object);
        remoteViews.setTextViewText(R.id.tag, tag);
        remoteViews.setTextViewText(R.id.message, validatedMessageStringOrError().replaceAll("\n", " ").trim());

        String taggerTime = GitUtils.getTimeAfterEmail(tagger);
        if (taggerTime != null) {
            remoteViews.setTextViewText(R.id.time, taggerTime);
        } else {
            remoteViews.setTextViewText(R.id.time, "invalid time");
        }

        if (approved != null && !approved) {
            remoteViews.setImageViewResource(R.id.tagImage, R.drawable.tag_hires_red);
        }
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        RemoteViews remoteViews = new RemoteViews(remoteViewsContainer.getPackage(), R.layout.tag_short_remote);
        remoteViewsContainer.addView(R.id.content, remoteViews);


        remoteViews.setTextViewText(R.id.object, object);
        remoteViews.setTextViewText(R.id.tag, tag);
        remoteViews.setTextViewText(R.id.message, validatedMessageStringOrError().replaceAll("\n", " ").trim());

        String taggerTime = GitUtils.getTimeAfterEmail(tagger);
        if (taggerTime != null) {
            remoteViews.setTextViewText(R.id.time, taggerTime);
        } else {
            remoteViews.setTextViewText(R.id.time, "invalid time");
        }
    }
}
