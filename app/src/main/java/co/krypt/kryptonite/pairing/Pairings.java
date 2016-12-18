package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.support.v4.util.Pair;
import android.util.ArraySet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.protocol.JSON;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Pairings {
    public static final String PAIRINGS_KEY = "PAIRINGS";
    private static Object lock = new Object();
    private SharedPreferences preferences;

    public Pairings(Context context) {
        preferences = context.getSharedPreferences("PAIRING_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public static String pairingLogsKey(String pairingUUIDString) {
        return pairingUUIDString + ".SIGNATURE_LOGS";
    }

    public void registerOnSharedPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (lock) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }
    public void unregisterOnSharedPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (lock) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private HashSet<Pairing> loadAllLocked() {
        HashSet<Pairing> pairings = new HashSet<>();
        Set<String> jsonPairings = new HashSet<>(preferences.getStringSet(PAIRINGS_KEY, new ArraySet<String>()));
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
        preferences.edit().putStringSet(PAIRINGS_KEY, jsonPairings).commit();
        return pairings;
    }

    public HashSet<Pairing> loadAll() {
        synchronized (lock) {
            return loadAllLocked();
        }
    }

    public HashSet<Pair<Pairing, SignatureLog>> loadAllWithLastCommand() {
        synchronized (lock) {
            HashSet<Pairing> pairings = loadAllLocked();
            HashSet<Pair<Pairing, SignatureLog>> pairingsWithLastCommand = new HashSet<>();
            for (Pairing pairing: pairings) {
                Pair<Pairing, SignatureLog> pair = new Pair<>(pairing, null);
                List<SignatureLog> sortedLogs = SignatureLog.sortByTimeDescending(getLogs(pairing));
                if (sortedLogs.size() > 0) {
                    pairingsWithLastCommand.add(new Pair<Pairing, SignatureLog>(pairing, sortedLogs.get(0)));
                } else {
                    pairingsWithLastCommand.add(new Pair<Pairing, SignatureLog>(pairing, null));
                }
            }
            return pairingsWithLastCommand;
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
                preferences.getStringSet(pairingLogsKey(pairingUUID), new ArraySet<String>()));
    }

    public void appendToLog(String pairingUUID, SignatureLog log) {
        synchronized (lock) {
            Set<String> logsSetJSON = getLogsJSONLocked(pairingUUID);
            logsSetJSON.add(JSON.toJson(log));
            preferences.edit().putStringSet(pairingLogsKey(pairingUUID), logsSetJSON).commit();
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
