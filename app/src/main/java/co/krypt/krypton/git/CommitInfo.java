package co.krypt.krypton.git;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.amazonaws.util.Base16;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.SHA1;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.packet.BinarySignable;
import co.krypt.krypton.protocol.GitSignRequestBody;
import co.krypt.krypton.protocol.JSON;

import static android.view.View.GONE;

/**
 * Created by Kevin King on 6/17/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class CommitInfo extends GitSignRequestBody implements BinarySignable {
    public static final String FIELD_NAME = "commit";

    @SerializedName("tree")
    @JSON.JsonRequired
    public String tree;

    @Nullable
    @SerializedName("parent")
    public String parent;

    @Nullable
    @SerializedName("merge_parents")
    public List<String> mergeParents;

    @SerializedName("author")
    @JSON.JsonRequired
    public String author;

    @SerializedName("committer")
    @JSON.JsonRequired
    public String committer;

    @SerializedName("message")
    @JSON.JsonRequired
    public byte[] message;

    public CommitInfo(String tree, @Nullable String parent, @Nullable List<String> mergeParents, String author, String committer, byte[] message) {
        this.tree = tree;
        this.parent = parent;
        this.author = author;
        this.mergeParents = mergeParents;
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

        if (mergeParents != null) {
            for (String mergeParent: mergeParents) {
                out.write("parent ".getBytes("UTF-8"));
                out.write(mergeParent.getBytes("UTF-8"));
                out.write("\n".getBytes("UTF-8"));
            }
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

        if (mergeParents != null) {
            for (String mergeParent: mergeParents) {
                s.append("PARENT ").append(mergeParent).append("\n");
            }
        }

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

        if (mergeParents != null) {
            for (String mergeParent: mergeParents) {
                commitDataBuf.write("parent ".getBytes("UTF-8"));
                commitDataBuf.write(mergeParent.getBytes("UTF-8"));
                commitDataBuf.write("\n".getBytes("UTF-8"));
            }
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

    public View fillShortView(ConstraintLayout container, @Nullable Boolean approved, @Nullable String signature) {
        View commitView = View.inflate(container.getContext(), R.layout.commit_short, container);

        TextView messageText = (TextView) commitView.findViewById(R.id.message);
        messageText.setText(validatedMessageStringOrError().replaceAll("\n", " ").trim());

        TextView timeText = (TextView) commitView.findViewById(R.id.time);
        String committerTime = GitUtils.getTimeAfterEmail(this.committer);
        if (committerTime != null) {
            timeText.setText(committerTime);
        } else {
            timeText.setText("invalid time");
        }

        TextView hashText = (TextView) commitView.findViewById(R.id.hash);
        if (signature != null) {
            hashText.setText(shortHash(signature));
        } else {
            if (approved != null && !approved) {
                hashText.setText("REJECTED");
                hashText.setBackgroundResource(R.drawable.hash_red_bg);
            } else {
                commitView.findViewById(R.id.hash).setVisibility(GONE);
            }
        }

        return commitView;
    }

    public View fillView(ConstraintLayout container, @Nullable Boolean approved, @Nullable String signature) {
        View commitView = View.inflate(container.getContext(), R.layout.commit, container);

        TextView authorText = (TextView) commitView.findViewById(R.id.author);
        String authorNameAndEmail = authorNameAndEmail();
        String committerNameAndEmail = committerNameAndEmail();
        if (authorNameAndEmail == null) {
            authorText.setText(author);
        } else  {
            if (authorNameAndEmail.equals(committerNameAndEmail)) {
                authorText.setText("");
                authorText.setVisibility(GONE);
                commitView.findViewById(R.id.authorLabel).setVisibility(GONE);
            } else {
                authorText.setText(authorNameAndEmail);
            }
        }

        TextView committerText = (TextView) commitView.findViewById(R.id.committer);
        if (committerNameAndEmail == null) {
            committerText.setText(committer);
        } else {
            committerText.setText(committerNameAndEmail);
        }

        TextView treeText = (TextView) commitView.findViewById(R.id.tree);
        treeText.setText(tree);

        TextView parentText = (TextView) commitView.findViewById(R.id.parent);
        if (parent != null) {
            parentText.setText(parent);
        }
        if (mergeParents != null) {
            for (String mergeParent: mergeParents) {
                parentText.append("\n" + mergeParent);
            }
        }
        if (parent == null && mergeParents == null) {
            parentText.setText("");
            commitView.findViewById(R.id.parentLabel).setVisibility(GONE);
        }

        TextView messageText = (TextView) commitView.findViewById(R.id.message);
        messageText.setText(validatedMessageStringOrError());

        TextView timeText = (TextView) commitView.findViewById(R.id.time);
        String committerTime = GitUtils.getTimeAfterEmail(this.committer);
        if (committerTime != null) {
            timeText.setText(committerTime);
        } else {
            timeText.setText("invalid time");
        }

        TextView hashText = (TextView) commitView.findViewById(R.id.hash);
        if (signature != null) {
            hashText.setText(shortHash(signature));
        } else {
            if (approved != null && !approved) {
                hashText.setText("REJECTED");
                hashText.setBackgroundResource(R.drawable.hash_red_bg);
            } else {
                commitView.findViewById(R.id.hash).setVisibility(GONE);
            }
        }

        return commitView;
    }

    public void fillRemoteTime(RemoteViews remoteViews, @Nullable Boolean approved, @Nullable String signature) {
        String committerTime = GitUtils.getTimeAfterEmail(this.committer);
        if (committerTime != null) {
            remoteViews.setTextViewText(R.id.time, committerTime);
        } else {
            remoteViews.setTextViewText(R.id.time, "invalid time");
        }
    }
    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        RemoteViews remoteViews = new RemoteViews(remoteViewsContainer.getPackage(), R.layout.commit_short_remote);

        remoteViewsContainer.addView(R.id.content, remoteViews);
        remoteViews.setTextViewText(R.id.message, validatedMessageStringOrError().replaceAll("\n", " ").trim());

        if (signature != null) {
            remoteViews.setTextViewText(R.id.hash, shortHash(signature));
        } else {
            if (approved != null && !approved) {
                remoteViews.setTextViewText(R.id.hash, "REJECTED");
                remoteViews.setInt(R.id.hash, "setBackgroundResource", R.drawable.hash_red_bg);
            } else {
                if (parent != null) {
                    if (parent.length() >= 7) {
                        remoteViews.setTextViewText(R.id.hash, parent.substring(0, 7));
                } else {
                        remoteViews.setTextViewText(R.id.hash, parent);
                    }
                } else {
                    remoteViews.setTextViewText(R.id.hash, "[first]");
                }
            }
        }
        fillRemoteTime(remoteViews, approved, signature);
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        RemoteViews remoteViews = new RemoteViews(remoteViewsContainer.getPackage(), R.layout.commit_remote);
        remoteViewsContainer.addView(R.id.content, remoteViews);

        String authorNameAndEmail = authorNameAndEmail();
        String committerNameAndEmail = committerNameAndEmail();
        if (authorNameAndEmail == null) {
            remoteViews.setTextViewText(R.id.author, author);
        } else  {
            if (authorNameAndEmail.equals(committerNameAndEmail)) {
                remoteViews.setTextViewText(R.id.author, "");
                remoteViews.setViewVisibility(R.id.author, GONE);
                remoteViews.setViewVisibility(R.id.authorLabel, GONE);
            } else {
                remoteViews.setTextViewText(R.id.author, authorNameAndEmail);
            }
        }

        if (committerNameAndEmail == null) {
            remoteViews.setTextViewText(R.id.committer, committer);
        } else {
            remoteViews.setTextViewText(R.id.committer, committerNameAndEmail);
        }


        remoteViews.setTextViewText(R.id.tree, tree);

        if (parent != null || mergeParents != null) {
            String parentText = "";
            if (parent != null) {
                parentText = parent;
            }
            if (mergeParents != null) {
                for (String mergeParent: mergeParents) {
                    parentText += "\n" + mergeParent;
                }
            }
            remoteViews.setTextViewText(R.id.parent, parentText);
        } else {
            remoteViews.setTextViewText(R.id.parent, "");
            remoteViews.setViewVisibility(R.id.parent, GONE);
            remoteViews.setViewVisibility(R.id.parentLabel, GONE);
        }

        remoteViews.setTextViewText(R.id.message, validatedMessageStringOrError().replaceAll("\n", " ").trim());

        if (signature != null) {
            remoteViews.setTextViewText(R.id.hash, shortHash(signature));
        } else {
            if (approved != null && !approved) {
                remoteViews.setTextViewText(R.id.hash, "REJECTED");
                remoteViews.setInt(R.id.hash, "setBackgroundResource", R.drawable.hash_red_bg);
            } else {
                remoteViews.setViewVisibility(R.id.hash, GONE);
            }
        }

        fillRemoteTime(remoteViews, approved, signature);
    }

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }
}
