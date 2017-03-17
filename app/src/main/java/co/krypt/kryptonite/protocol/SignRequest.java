package co.krypt.kryptonite.protocol;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignRequest {
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

    public byte[] sessionID() {
        if (data.length < 4) {
            return null;
        }

        byte[] bigEndianSessionIDLen = Arrays.copyOfRange(data, 0, 4);
        DataInputStream readSessionIDLen = new DataInputStream(new ByteArrayInputStream(bigEndianSessionIDLen));
        try {
            int sessionIDLen = readSessionIDLen.readInt();
            if (data.length < 4 + sessionIDLen) {
                return null;
            }
            return Arrays.copyOfRange(data, 4, 4 + sessionIDLen);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String user() {
        byte[] sessionID = this.sessionID();
        if (sessionID == null) {
            return null;
        }
        int sessionIDLen = sessionID.length;
        if (data.length < 4 + sessionIDLen + 1 + 4) {
            return null;
        }
        byte[] bigEndianUserLen = Arrays.copyOfRange(data, 4 + sessionIDLen + 1, 4 + sessionIDLen + 1 + 4);
        DataInputStream readLen = new DataInputStream(new ByteArrayInputStream(bigEndianUserLen));
        try {
            int userLen = readLen.readInt();
            if (userLen == 0) {
                return "";
            }
            if (data.length < 4 + sessionIDLen + 1 + 4 + userLen) {
                return null;
            }
            byte[] userBytes = Arrays.copyOfRange(data, 4 + sessionIDLen + 1 + 4, 4 + sessionIDLen + 1 + 4 + userLen);
            return new String(userBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
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
            return user + "@" + hostName;
        }
        return hostName;
    }

    public String hostNameNotVerifiedDisplay() {
        String hostName = "unknown host";
        String user = user();
        if (user != null) {
            return user + "@" + hostName;
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
}
