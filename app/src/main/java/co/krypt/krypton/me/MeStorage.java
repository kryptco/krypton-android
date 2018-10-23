package co.krypt.krypton.me;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.pgp.PGPException;
import co.krypt.krypton.pgp.PGPManager;
import co.krypt.krypton.pgp.PGPPublicKey;
import co.krypt.krypton.pgp.UserID;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.Notifications;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.uiutils.Email;

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

    private Profile cachedProfile;

    public MeStorage(Context context) {
        preferences = context.getSharedPreferences("ME_MANAGER_PREFERENCES", Context.MODE_PRIVATE);
        this.context = context;
    }

    private static final AtomicReference<SSHKeyPairI> keyPair = new AtomicReference<>(null);

    @Nullable
    public static SSHKeyPairI getOrLoadKeyPair(Context context) throws CryptoException {
        if (keyPair.get() == null) {
            keyPair.compareAndSet(null, KeyManager.loadMeRSAOrEdKeyPair(context));
        }
        return keyPair.get();
    }

    @Nullable
    public Profile load() {
        synchronized (lock) {
            if (cachedProfile == null) {
                cachedProfile = loadWithUserID(null);
            }
            if (cachedProfile == null) {
                //There's nothing to load, so let's not NPE here
                return null;
            }
            return new Profile(cachedProfile);
        }
    }

    public Profile loadWithUserID(@Nullable UserID userID) {
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
                SSHKeyPairI kp = getOrLoadKeyPair(context);
                if (kp != null) {
                    me.sshWirePublicKey = kp.publicKeySSHWireFormat();

                    if (userID != null) {
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
                                if (userIDs.size() == USER_ID_LIMIT) {
                                    //  detect abuse of exporting PGP userIDs
                                    Notifications.notifyPGPKeyExport(context, pgpPublicKey);
                                }
                            }
                            set(me, userIDs);
                        } catch (PGPException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InvalidKeyException | IOException | CryptoException e) {
                e.printStackTrace();
            }
            try {
                me.teamCheckpoint = TeamDataProvider.getTeamCheckpoint(context).success;
            } catch (Native.NotLinked notLinked) {
                notLinked.printStackTrace();
            }
            return me;
        }
    }

    public void delete() {
        synchronized (lock) {
            keyPair.set(null);
            preferences.edit()
                    .remove("ME")
                    .remove("ME.USER_IDS").apply();
            cachedProfile = null;
        }
    }

    public void set(Profile profile) {
        synchronized (lock) {
            preferences.edit().putString("ME", JSON.toJson(profile)).apply();
            cachedProfile = profile;
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

    public void setEmail(String email) throws CryptoException {
        synchronized (lock) {
            Profile me;
            if (Email.verifyEmailPattern.matcher(email).matches()) {
                me = loadWithUserID(UserID.parse(" <" + email + ">"));
            } else {
                me = loadWithUserID(null);
            }
            if (me == null) {
                me = new Profile();
            }
            me.email = email;
            set(me);
        }
    }

    public static String getDeviceName() {
        String deviceName = null;
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null) {
             deviceName = bt.getName();
        }
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = Build.MODEL;
        }
        return deviceName;
    }
}
