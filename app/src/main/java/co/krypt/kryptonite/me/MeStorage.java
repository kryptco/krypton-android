package co.krypt.kryptonite.me;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.HashSet;
import java.util.Set;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.log.SignatureLog;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.Profile;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class MeStorage {
    private static Object lock = new Object();
    private SharedPreferences preferences;

    public MeStorage(Context context) {
        preferences = context.getSharedPreferences("ME_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
    }

    public Profile load() {
        synchronized (lock) {
            String meJSON = preferences.getString("ME", "");
            Profile me = JSON.fromJson(meJSON, Profile.class);
            try {
                me.sshWirePublicKey = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG).publicKeySSHWireFormat();
            } catch (InvalidKeyException | IOException | CryptoException e) {
                e.printStackTrace();
            }
            return me;
        }
    }

    public void set(Profile profile) {
        synchronized (lock) {
            preferences.edit().putString("ME", JSON.toJson(profile)).commit();
        }
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
