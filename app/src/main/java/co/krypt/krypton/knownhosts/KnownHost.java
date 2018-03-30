package co.krypt.krypton.knownhosts;

import com.amazonaws.util.Base64;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 5/8/17.
 * Copyright 2016. KryptCo, Inc.
 */

@DatabaseTable(tableName = "known_host")
public class KnownHost {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "host_name", uniqueIndex = true)
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

    public String fingerprint() {
        try {
            return Base64.encodeAsString(SHA256.digest(Base64.decode(publicKey)));
        } catch (CryptoException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected KnownHost() {}

    public static List<KnownHost> sortByTimeDescending(List<KnownHost> knownHosts) {
        List<KnownHost> sortedHosts = new ArrayList<>(knownHosts);
        java.util.Collections.sort(sortedHosts, new Comparator<KnownHost>() {
            @Override
            public int compare(KnownHost lhs, KnownHost rhs) {
                return Long.compare(rhs.addedUnixSeconds, lhs.addedUnixSeconds);
            }
        });
        return sortedHosts;
    }

    public static List<KnownHost> sortByHostAscending(List<KnownHost> knownHosts) {
        List<KnownHost> sortedHosts = new ArrayList<>(knownHosts);
        java.util.Collections.sort(sortedHosts, (lhs, rhs) -> lhs.hostName.compareTo(rhs.hostName));
        return sortedHosts;
    }
}
