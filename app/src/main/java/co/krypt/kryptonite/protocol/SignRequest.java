package co.krypt.kryptonite.protocol;

import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.crypto.SSHWireDataParser;
import co.krypt.kryptonite.exception.ProtocolException;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignRequest extends RequestBody {
    public static final String FIELD_NAME = "sign_request";
    @SerializedName("data")
    @JSON.JsonRequired
    public byte[] data;

    @SerializedName("public_key_fingerprint")
    @JSON.JsonRequired
    public byte[] publicKeyFingerprint;

    @SerializedName("command")
    public String command;

    @SerializedName("host_auth")
    public HostAuth hostAuth;

    public Boolean hostNameVerified = null;

    public String getCommandOrDefault(String defaultString) {
        if (command != null) {
            return command;
        }
        return defaultString;
    }

    public void validate() throws ProtocolException {
        if (sessionID() == null) {
            throw new ProtocolException("no sessionID");
        }
        if (user() == null) {
            throw new ProtocolException("no user name");
        }
        if (algo() == null) {
            throw new ProtocolException("no algo");
        }
    }

    private boolean parsed;
    private byte[] parsedSessionID;
    private String parsedUser;
    private String parsedAlgo;

    private synchronized void parseClientAuth() throws IOException {
        if (parsed) {
            return;
        }
        parsed = true;

        //  https://tools.ietf.org/html/rfc4252 section 7
        //
        //    string    session identifier
        //    byte      SSH_MSG_USERAUTH_REQUEST
        //    string    user name
        //    string    service name
        //    string    "publickey"
        //    boolean   TRUE
        //    string    public key algorithm name
        //
        //    OMITTED since it is redundant:
        //    string    public key to be used for authentication
        //
        SSHWireDataParser parser = new SSHWireDataParser(data);
        parsedSessionID = parser.popByteArray();
        parser.popByte();
        parsedUser = parser.popString();
        parser.popString();
        parser.popString();
        parser.popBoolean();
        parsedAlgo = parser.popString();
    }

    public byte[] sessionID() {
        try {
            parseClientAuth();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedSessionID;
    }

    public String user() {
        try {
            parseClientAuth();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedUser;
    }

    public String algo() {
        try {
            parseClientAuth();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedAlgo;
    }


    @Nullable
    public String firstHostnameIfExists() {
        if (hostAuth != null &&
                hostAuth.hostNames != null &&
                hostAuth.hostNames.length > 0) {
            return hostAuth.hostNames[0];
        }
        return null;
    }

    public String hostNameVerifiedDisplay() {
        String hostName = firstHostnameIfExists() != null ? firstHostnameIfExists() : "unknown host";
        String user = user();
        if (user != null) {
            return user + " @ " + hostName;
        }
        return hostName;
    }

    public String hostNameNotVerifiedDisplay() {
        String hostName = "unknown host";
        String user = user();
        if (user != null) {
            return user + " @ " + hostName;
        }
        return hostName;
    }

    public String display() {
        return verifyHostName() ? hostNameVerifiedDisplay() : hostNameNotVerifiedDisplay();
    }

    public boolean verifyHostName() {
        if (hostNameVerified != null) {
            return hostNameVerified;
        }
        if (sessionID() == null || hostAuth == null || firstHostnameIfExists() == null) {
            hostNameVerified = false;
            return hostNameVerified;
        }
        hostNameVerified = hostAuth.verifySessionID(sessionID());
        return hostNameVerified;
    }

    public String verifiedHostNameOrDefault(String defaultHostName) {
        String firstHostName = firstHostnameIfExists();
        if (firstHostName == null) {
            return defaultHostName;
        }
        return verifyHostName() ? firstHostName : defaultHostName;
    }

    public String userOrInvalid() {
        if (user() == null) {
            return "invalid user";
        } else {
            return user();
        }
    }

    public View fillView(ConstraintLayout container) {
        View sshView = View.inflate(container.getContext(), R.layout.ssh_short, container);

        TextView messageText = (TextView) sshView.findViewById(R.id.message);
        messageText.setText(userOrInvalid() + " @ " + verifiedHostNameOrDefault("unknown host"));
        messageText.setMaxLines(Integer.MAX_VALUE);

        TextView timeText = (TextView) sshView.findViewById(R.id.time);
        timeText.setText("");

        return sshView;
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @javax.annotation.Nullable Boolean approved, @javax.annotation.Nullable String signature) {
        fillShortRemoteViews(remoteViewsContainer, approved, signature);
        remoteViewsContainer.setInt(R.id.message, "setMaxLines", Integer.MAX_VALUE);
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @javax.annotation.Nullable Boolean approved, @javax.annotation.Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        RemoteViews remoteViews = new RemoteViews(remoteViewsContainer.getPackage(), R.layout.ssh_short_remote);

        remoteViewsContainer.addView(R.id.content, remoteViews);

        remoteViews.setTextViewText(R.id.message, userOrInvalid() + " @ " + verifiedHostNameOrDefault("unknown host"));

        if (approved != null && !approved) {
            remoteViews.setInt(R.id.label, "setBackgroundResource", R.drawable.hash_red_bg);
        }
    }

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }
}
