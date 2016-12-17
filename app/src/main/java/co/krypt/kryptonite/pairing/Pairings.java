package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import java.util.HashSet;
import java.util.Set;

import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.protocol.JSON;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Pairings {
    private static Object lock = new Object();
    private SharedPreferences preferences;

    public Pairings(Context context) {
        preferences = context.getSharedPreferences("PAIRING_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    private HashSet<Pairing> loadAllLocked() {
        HashSet<Pairing> pairings = new HashSet<>();
        Set<String> jsonPairings = new HashSet<>(preferences.getStringSet("PAIRINGS", new ArraySet<String>()));
        for (String jsonPairing : jsonPairings) {
            pairings.add(JSON.fromJson(jsonPairing, Pairing.class));
        }
        return pairings;
    }

    private HashSet<Pairing> setAllLocked(HashSet<Pairing> pairings) {
        Set<String> jsonPairings = new ArraySet<>();
        for (Pairing pairing : pairings) {
            jsonPairings.add(JSON.toJson(pairing));
        }
        preferences.edit().putStringSet("PAIRINGS", jsonPairings).commit();
        return pairings;
    }

    public HashSet<Pairing> loadAll() {
        synchronized (lock) {
            return loadAllLocked();
        }
    }

    public Pairing getPairing(String pairingUUID) {
        synchronized (lock) {
            HashSet<Pairing> pairings =  loadAllLocked();
            for (Pairing pairing: pairings) {
                if (pairing.getUUIDString().equals(pairingUUID)) {
                    return pairing;
                }
            }
            return null;
        }
    }

    public void unpair(Pairing pairing) {
        synchronized (lock) {
            HashSet<Pairing> currentPairings = loadAllLocked();
            currentPairings.remove(pairing);
            setAllLocked(currentPairings);
        }
    }

    public void pair(Pairing pairing) {
        synchronized (lock) {
            HashSet<Pairing> currentPairings = loadAllLocked();
            currentPairings.add(pairing);
            setAllLocked(currentPairings);
        }
    }

    public void unpairAll() {
        synchronized (lock) {
            setAllLocked(new HashSet<Pairing>());
        }
    }

    private Set<String> getLogsJSONLocked(String pairingUUID) {
        return new HashSet<>(
                preferences.getStringSet(pairingUUID + ".SIGNATURE_LOGS", new ArraySet<String>()));
    }

    public void appendToLog(String pairingUUID, SignatureLog log) {
        synchronized (lock) {
            Set<String> logsSetJSON = getLogsJSONLocked(pairingUUID);
            logsSetJSON.add(JSON.toJson(log));
            preferences.edit().putStringSet(pairingUUID + ".SIGNATURE_LOGS", logsSetJSON).commit();
        }
    }

    public void appendToLog(Pairing pairing, SignatureLog log) {
        appendToLog(pairing.getUUIDString(), log);
    }

    public HashSet<SignatureLog> getLogs(String pairingUUID) {
        synchronized (lock) {
            HashSet<SignatureLog> logs = new HashSet<>();
            Set<String> logsSetJSON = getLogsJSONLocked(pairingUUID);
            for (String jsonLog : logsSetJSON) {
                logs.add(JSON.fromJson(jsonLog, SignatureLog.class));
            }
            return logs;
        }
    }

    public HashSet<SignatureLog> getLogs(Pairing pairing) {
        return getLogs(pairing.getUUIDString());
    }
}
