package co.krypt.kryptonite.me;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nullable;

import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPairI;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pgp.PGPException;
import co.krypt.kryptonite.pgp.PGPManager;
import co.krypt.kryptonite.pgp.UserID;
import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.Profile;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class MeStorage {
    private static final String TAG = "MeStorage";
    private static final Object lock = new Object();
    private SharedPreferences preferences;
    private final Context context;

    public MeStorage(Context context) {
        preferences = context.getSharedPreferences("ME_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
        this.context = context;
    }

    public Profile load(@Nullable SSHKeyPairI kp, @Nullable UserID userID) {
        //TODO: store signed userIDs
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
                    me.pgpPublicKey = PGPManager.publicKeyWithIdentities(kp, Collections.singletonList(userID));
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

    public void setEmail(String email) {
        synchronized (lock) {
            Profile me = load(null, null);
            if (me == null) {
                me = new Profile();
            }
            me.email = email;
            set(me);
        }
    }
}
