package co.krypt.kryptonite.log;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by Kevin King on 12/15/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class SignatureLog {
    @SerializedName("digest")
    public final byte[] digest;

    @SerializedName("command")
    @Nullable
    public final String command;

    @SerializedName("user")
    @Nullable
    public final String user;

    @SerializedName("host_name")
    @Nullable
    public final String hostName;

    @SerializedName("unix_seconds")
    public final long unixSeconds;

    @SerializedName("host_name_verified")
    public final boolean hostNameVerified;

    public SignatureLog(byte[] digest, String command, String user, String hostName, long unixSeconds, boolean hostNameVerified) {
        this.digest = digest;
        this.command = command;
        this.user = user;
        this.hostName = hostName;
        this.unixSeconds = unixSeconds;
        this.hostNameVerified = hostNameVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SignatureLog that = (SignatureLog) o;

        if (unixSeconds != that.unixSeconds) return false;
        if (hostNameVerified != that.hostNameVerified) return false;
        if (!Arrays.equals(digest, that.digest)) return false;
        if (command != null ? !command.equals(that.command) : that.command != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        return hostName != null ? hostName.equals(that.hostName) : that.hostName == null;

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(digest);
        result = 31 * result + (command != null ? command.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + (int) (unixSeconds ^ (unixSeconds >>> 32));
        result = 31 * result + (hostNameVerified ? 1 : 0);
        return result;
    }

    public String userHostText() {
        return (user != null ? user + "@" : "") +
                (hostNameVerified ? hostName : "unknown host");
    }

    public static List<SignatureLog> sortByTimeDescending(Set<SignatureLog> logs) {
        List<SignatureLog> sortedLogs = new ArrayList<>(logs);
        java.util.Collections.sort(sortedLogs, new Comparator<SignatureLog>() {
            @Override
            public int compare(SignatureLog lhs, SignatureLog rhs) {
                return Long.compare(rhs.unixSeconds, lhs.unixSeconds);
            }
        });
        return sortedLogs;
    }
}
