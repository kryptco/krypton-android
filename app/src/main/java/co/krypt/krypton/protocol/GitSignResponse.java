package co.krypt.krypton.protocol;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2017. KryptCo, Inc.
 */

public class GitSignResponse {
    @Nullable
    @SerializedName("signature")
    public byte[] signature;

    @Nullable
    @SerializedName("error")
    public String error;

    public GitSignResponse(@Nullable byte[] signature, @Nullable String error) {
        this.signature = signature;
        this.error = error;
    }
}
