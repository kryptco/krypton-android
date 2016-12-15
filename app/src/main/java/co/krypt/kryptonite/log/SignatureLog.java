package co.krypt.kryptonite.log;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

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

    @SerializedName("unix_seconds")
    public final long unixSeconds;

    public SignatureLog(byte[] digest, String command, long unixSeconds) {
        this.digest = digest;
        this.command = command;
        this.unixSeconds = unixSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SignatureLog that = (SignatureLog) o;

        if (unixSeconds != that.unixSeconds) return false;
        if (!Arrays.equals(digest, that.digest)) return false;
        return command != null ? command.equals(that.command) : that.command == null;

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(digest);
        result = 31 * result + (command != null ? command.hashCode() : 0);
        result = 31 * result + (int) (unixSeconds ^ (unixSeconds >>> 32));
        return result;
    }
}
