package co.krypt.krypton.u2f;

import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Kevin King on 8/21/18.
 * Copyright 2018. KryptCo, Inc.
 */

@DatabaseTable(tableName = "u2f_accounts")
public class RegisteredAccount {
    @DatabaseField(columnName = "key_handle_hash", id = true)
    public String keyHandleHash;

    @DatabaseField(columnName = "key_handle")
    public String keyHandle;

    @DatabaseField(columnName = "app_id")
    public String appId;
    @DatabaseField(columnName = "added")
    public Long added;
    @DatabaseField(columnName = "last_used")
    @Nullable
    public Long lastUsed;

    @SuppressWarnings("unused")
    public RegisteredAccount() {}

    public RegisteredAccount(String keyHandleHash, String appId, Long added) {
        this.keyHandleHash = keyHandleHash;
        this.appId = appId;
        this.added = added;
    }

    public RegisteredAccount(String keyHandleHash, String keyHandle, String appId, Long added) {
        this.keyHandleHash = keyHandleHash;
        this.keyHandle = keyHandle;
        this.appId = appId;
        this.added = added;
    }
}
