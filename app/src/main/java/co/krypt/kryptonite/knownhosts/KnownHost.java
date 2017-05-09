package co.krypt.kryptonite.knownhosts;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Kevin King on 5/8/17.
 * Copyright 2016. KryptCo, Inc.
 */

@DatabaseTable(tableName = "known_host")
public class KnownHost {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "host_name")
    public String hostName;

    @DatabaseField(columnName = "public_key")
    public String publicKey;

    @DatabaseField(columnName = "added_unix_seconds")
    public long addedUnixSeconds;

    public KnownHost(String hostName, String publicKey, long addedUnixSeconds) {
        this.hostName = hostName;
        this.publicKey = publicKey;
        this.addedUnixSeconds = addedUnixSeconds;
    }

    protected KnownHost() {}
}
