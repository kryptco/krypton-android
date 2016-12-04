package co.krypt.kryptonite;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    private ArrayList<Pairing> loadAllLocked() {
        ArrayList<Pairing> pairings = new ArrayList<>();
        Set<String> jsonPairings = preferences.getStringSet("PAIRINGS", new ArraySet<String>());
        for (String jsonPairing : jsonPairings) {
            pairings.add(JSON.fromJson(jsonPairing, Pairing.class));
        }
        return pairings;
    }

    private ArrayList<Pairing> setAllLocked(ArrayList<Pairing> pairings) {
        Set<String> jsonPairings = new ArraySet<>();
        for (Pairing pairing : pairings) {
            jsonPairings.add(JSON.toJson(pairing));
        }
        preferences.edit().putStringSet("PAIRINGS", jsonPairings).commit();
        return pairings;
    }

    public ArrayList<Pairing> loadAll() {
        synchronized (lock) {
            return loadAllLocked();
        }
    }

    public void unpair(Pairing pairing) {
        synchronized (lock) {
            ArrayList<Pairing> currentPairings = loadAllLocked();
            currentPairings.remove(pairing);
            setAllLocked(currentPairings);
        }
    }

    public void pair(Pairing pairing) {
        synchronized (lock) {
            ArrayList<Pairing> currentPairings = loadAllLocked();
            currentPairings.add(pairing);
            setAllLocked(currentPairings);
        }
    }

    public void unpairAll() {
        synchronized (lock) {
            setAllLocked(new ArrayList<Pairing>());
        }
    }
}
