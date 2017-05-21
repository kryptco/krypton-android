package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.ArraySet;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.db.OpenDatabaseHelper;
import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.protocol.JSON;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Pairings {
    private static final String OLD_PAIRINGS_KEY = "PAIRINGS";
    private static final String PAIRINGS_KEY = "PAIRINGS_2";
    private static final Object lock = new Object();
    private final SharedPreferences preferences;
    private final Context context;
    private final Analytics analytics;

    public static final String ON_DEVICE_LOG_ACTION = "co.krypt.kryptonite.action.ON_DEVICE_LOG";

    //  per-pairing settings
    private static final String REQUIRE_UNKNOWN_HOST_MANUAL_APPROVAL = ".REQUIRE_UNKNOWN_HOST_MANUAL_APPROVAL";


    public final OpenDatabaseHelper dbHelper;

    public Pairings(Context context) {
        this.context = context;
        this.analytics = new Analytics(context);
        preferences = context.getSharedPreferences("PAIRING_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
        dbHelper = OpenHelperManager.getHelper(context, OpenDatabaseHelper.class);
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

    public HashSet<Pairing> loadAllOldPairings() {
        HashSet<Pairing> pairings = new HashSet<>();
        synchronized (lock) {
            Set<String> jsonPairings = new HashSet<>(preferences.getStringSet(OLD_PAIRINGS_KEY, new ArraySet<String>()));
            for (String jsonPairing : jsonPairings) {
                pairings.add(JSON.fromJson(jsonPairing, Pairing.class));
            }
        }
        return pairings;
    }

    public boolean hasOldPairings() {
        return !loadAllOldPairings().isEmpty();
    }

    public void clearOldPairings() {
        synchronized (lock) {
            preferences.edit().remove(OLD_PAIRINGS_KEY).apply();
        }
    }

    private HashSet<Pairing> setAllLocked(HashSet<Pairing> pairings) {
        Set<String> jsonPairings = new ArraySet<>();
        for (Pairing pairing : pairings) {
            jsonPairings.add(JSON.toJson(pairing));
        }
        preferences.edit().putStringSet(PAIRINGS_KEY, jsonPairings).apply();
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
            editor.apply();
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
                    .apply();
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
            SharedPreferences.Editor prefs = preferences.edit();
            prefs.remove(getSettingsKey(pairing, REQUIRE_UNKNOWN_HOST_MANUAL_APPROVAL))
                    .remove(pairingApprovedKey(pairing.getUUIDString()))
                    .remove(pairingApprovedUntilKey(pairing.getUUIDString()))
                    .apply();
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
            for (Pairing pairing: loadAllLocked()) {
                unpair(pairing);
            }
        }
    }

    public void appendToLog(String pairingUUID, SignatureLog log) {
        synchronized (lock) {
            try {
                dbHelper.getSignatureLogDao().create(log);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        Intent onLog = new Intent(ON_DEVICE_LOG_ACTION);
        context.sendBroadcast(onLog);
    }

    public void appendToLog(Pairing pairing, SignatureLog log) {
        appendToLog(pairing.getUUIDString(), log);
    }

    public HashSet<SignatureLog> getLogs(String pairingUUID) {
        synchronized (lock) {
            try {
                List<SignatureLog> logs = dbHelper.getSignatureLogDao().queryForEq("pairing_uuid", pairingUUID);
                return new HashSet<>(logs);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new HashSet<>();
        }
    }

    public HashSet<SignatureLog> getLogs(Pairing pairing) {
        return getLogs(pairing.getUUIDString());
    }

    public List<SignatureLog> getAllLogsRedacted() {
        synchronized (lock) {
            try {
                List<SignatureLog> logs = dbHelper.getSignatureLogDao().queryForAll();
                return SignatureLog.sortByTimeDescending(new HashSet<SignatureLog>(logs));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
    }

    private static String getSettingsKey(Pairing pairing, String settingsKey) {
        return pairing.getUUIDString() + "." + settingsKey;
    }

    public boolean requireUnknownHostManualApproval(Pairing pairing) {
        synchronized (lock) {
            return preferences.getBoolean(getSettingsKey(pairing, REQUIRE_UNKNOWN_HOST_MANUAL_APPROVAL), true);
        }
    }

    public void setRequireUnknownHostManualApproval(Pairing pairing, boolean requireApproval) {
        synchronized (lock) {
            preferences.edit().putBoolean(getSettingsKey(pairing, REQUIRE_UNKNOWN_HOST_MANUAL_APPROVAL), requireApproval).apply();
        }
    }
}
