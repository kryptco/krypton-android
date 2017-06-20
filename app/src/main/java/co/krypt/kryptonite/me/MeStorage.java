package co.krypt.kryptonite.me;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pgp.PGPException;
import co.krypt.kryptonite.pgp.PGPManager;
import co.krypt.kryptonite.pgp.PGPPublicKey;
import co.krypt.kryptonite.pgp.UserID;
import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.silo.Notifications;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class MeStorage {
    private static final int USER_ID_LIMIT = 3;
    private static final String TAG = "MeStorage";
    private static final Object lock = new Object();
    private SharedPreferences preferences;
    private final Context context;

    public MeStorage(Context context) {
        preferences = context.getSharedPreferences("ME_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
        this.context = context;
    }


    public Profile load() {
        return loadWithUserID(null, null, null);
    }

    public Profile loadWithUserID(@Nullable SSHKeyPairI kp, @Nullable UserID userID, @Nullable Pairing pairing) {
        synchronized (lock) {
            String meJSON = preferences.getString("ME", null);
            if (meJSON == null) {
                Log.i(TAG, "no profile found");
                return null;
            }
            Profile me = JSON.fromJson(meJSON, Profile.class);
            if (me == null) {
                Log.i(TAG, "no profile found");
                return null;
            }
            try {
                me.sshWirePublicKey = KeyManager.loadMeRSAOrEdKeyPair(context).publicKeySSHWireFormat();
            } catch (InvalidKeyException | IOException | CryptoException e) {
                e.printStackTrace();
            }
            if (kp != null && userID != null) {
                try {
                    List<UserID> userIDs = getUserIDs();
                    //  keep USER_ID_LIMIT most recent UserIDs
                    if (userIDs.remove(userID)) {
                        userIDs.add(userID);
                    } else {
                        if (userIDs.size() >= USER_ID_LIMIT) {
                            userIDs.remove(0);
                        }
                        userIDs.add(userID);
                        PGPPublicKey pgpPublicKey = PGPManager.publicKeyWithIdentities(kp, userIDs);
                        me.pgpPublicKey = pgpPublicKey.serializedBytes();
                        Notifications.notifyPGPKeyExport(context, pgpPublicKey);
                    }
                    set(me, userIDs);
                } catch (PGPException | IOException e) {
                    e.printStackTrace();
                }
            }
            return me;
        }
    }

    public void delete() {
        synchronized (lock) {
            preferences.edit().putString("ME", "").apply();
        }
    }

    public void set(Profile profile) {
        synchronized (lock) {
            preferences.edit().putString("ME", JSON.toJson(profile)).apply();
        }
    }

    public void set(Profile profile, List<UserID> userIDs) {
        ArrayList<String> userIDStrings = new ArrayList<>();
        for (UserID userID : userIDs) {
            userIDStrings.add(new String(userID.utf8()));
        }
        synchronized (lock) {
            set(profile);
            preferences.edit().putString("ME.USER_IDS", JSON.toJson(userIDStrings.toArray(new String[]{}))).apply();
        }
    }

    public List<UserID> getUserIDs() {
        List<UserID> userIDs = new LinkedList<>();

        synchronized (lock) {
            String[] userIDStrings = JSON.fromJson(preferences.getString("ME.USER_IDS", "[]"), String[].class);
            for (String userIDString: userIDStrings) {
                userIDs.add(UserID.parse(userIDString));
            }
        }

        return userIDs;
    }

    public void setEmail(String email) {
        synchronized (lock) {
            Profile me = load();
            if (me == null) {
                me = new Profile();
            }
            me.email = email;
            set(me);
        }
    }
}
