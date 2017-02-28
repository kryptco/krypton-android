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
    @SerializedName("digest")
    public byte[] digest;

    @SerializedName("public_key_fingerprint")
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
        if (digest.length < 36) {
            return null;
        }
        return Arrays.copyOfRange(digest, 4, 36);
    }

    public String user() {
        if (digest.length < 38) {
            return null;
        }
        byte[] bigEndianUserLen = Arrays.copyOfRange(digest, 37, 41);
        DataInputStream readLen = new DataInputStream(new ByteArrayInputStream(bigEndianUserLen));
        try {
            int userLen = readLen.readInt();
            if (userLen == 0) {
                return "";
            }
            if (digest.length < 41 + userLen) {
                return null;
            }
            byte[] userBytes = Arrays.copyOfRange(digest, 41, 41 + userLen);
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
