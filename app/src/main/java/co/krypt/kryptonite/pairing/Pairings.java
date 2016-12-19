package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.content.SharedPreferences;
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

    public static String pairingApprovedKey(String pairingUUIDString) {
        return pairingUUIDString + ".APPROVED";
    }

    public static String pairingApprovedUntilKey(String pairingUUIDString) {
        return pairingUUIDString + ".APPROVED_UNTIL";
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

    public Boolean getApproved(String pairingUUID) {
        synchronized (lock) {
            return preferences.getBoolean(pairingApprovedKey(pairingUUID), false);
        }
    }

    public Boolean getApproved(Pairing pairing) {
        return getApproved(pairing.getUUIDString());
    }

    public void setApproved(String pairingUUID, boolean approved) {
        synchronized (lock) {
            SharedPreferences.Editor editor = preferences.edit().putBoolean(pairingApprovedKey(pairingUUID), approved);
            if (!approved) {
                editor.putLong(pairingApprovedUntilKey(pairingUUID), -1);
            }
            editor.commit();
        }
    }

    public Long getApprovedUntil(String pairingUUID) {
        synchronized (lock) {
            long approvedUntil = preferences.getLong(pairingApprovedUntilKey(pairingUUID), -1);
            if (approvedUntil == -1) {
                return null;
            }
            return approvedUntil;
        }
    }

    public Long getApprovedUntil(Pairing pairing) {
        return getApprovedUntil(pairing.getUUIDString());
    }

    public void setApprovedUntil(String pairingUUID, Long time) {
        synchronized (lock) {
            preferences.edit()
                    .putLong(pairingApprovedUntilKey(pairingUUID), time)
                    .putBoolean(pairingApprovedKey(pairingUUID), false)
                    .commit();
        }
    }

    public void setApprovedUntil(Pairing pairing, Long time) {
        setApprovedUntil(pairing.getUUIDString(), time);
    }

    public boolean isApprovedNow(String pairingUUID) {
        synchronized (lock) {
            if (getApproved(pairingUUID)) {
                return true;
            }
            Long approvedUntil = getApprovedUntil(pairingUUID);
            return approvedUntil != null && System.currentTimeMillis() < approvedUntil * 1000;
        }
    }

    public boolean isApprovedNow(Pairing pairing) {
        return isApprovedNow(pairing.getUUIDString());
    }

   public HashSet<Session> loadAllSessions() {
        synchronized (lock) {
            HashSet<Pairing> pairings = loadAllLocked();
            HashSet<Session> sessions = new HashSet<>();
            for (Pairing pairing: pairings) {
                Pair<Pairing, SignatureLog> pair = new Pair<>(pairing, null);
                List<SignatureLog> sortedLogs = SignatureLog.sortByTimeDescending(getLogs(pairing));
                if (sortedLogs.size() > 0) {
                    sessions.add(new Session(pairing, sortedLogs.get(0), getApproved(pairing), getApprovedUntil(pairing)));
                } else {
                    sessions.add(new Session(pairing, null, getApproved(pairing), getApprovedUntil(pairing)));
                }
            }
            return sessions;
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
