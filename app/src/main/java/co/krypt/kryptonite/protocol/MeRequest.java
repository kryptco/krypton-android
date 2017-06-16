package co.krypt.kryptonite.protocol;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

import co.krypt.kryptonite.pgp.UserID;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class MeRequest {
    @SerializedName("pgp_user_id")
    @Nullable
    public String pgpUserID;

    @Nullable
    public UserID userID() {
        if (pgpUserID == null) {
            return null;
        }
        return UserID.parse(pgpUserID);
    }

}
