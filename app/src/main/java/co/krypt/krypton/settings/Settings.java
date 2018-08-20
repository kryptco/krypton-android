package co.krypt.krypton.settings;

import android.content.Context;
import android.content.SharedPreferences;

import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;

/**
 * Created by Kevin King on 1/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class Settings {
    public static final String ENABLE_APPROVED_NOTIFICATIONS_KEY = "ENABLE_APPROVED_NOTIFICATIONS";
    public static final String SILENCE_NOTIFICATIONS_KEY = "SILENCE_NOTIFICATIONS";
    public static final String ONE_TOUCH_LOGIN_KEY = "ONE_TOUCH_LOGIN";
    public static final String DEVELOPER_MODE_KEY = "DEVELOPER_MODE";

    private static Object lock = new Object();
    private final SharedPreferences preferences;
    private final Context context;

    public Settings(Context context) {
        preferences = context.getSharedPreferences("SETTINGS_PREFERENCES", Context.MODE_PRIVATE);
        this.context = context;
    }

    public boolean approvedNotificationsEnabled() {
        synchronized (lock) {
            return preferences.getBoolean(ENABLE_APPROVED_NOTIFICATIONS_KEY, true);
        }
    }

    public void setApprovedNotificationsEnabled(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(ENABLE_APPROVED_NOTIFICATIONS_KEY, b).apply();
        }
    }

    public boolean silenceNotifications() {
        synchronized (lock) {
            return preferences.getBoolean(SILENCE_NOTIFICATIONS_KEY, false);
        }
    }

    public void setSilenceNotifications(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(SILENCE_NOTIFICATIONS_KEY, b).apply();
        }
    }

    public boolean oneTouchLogin() {
        synchronized (lock) {
            return preferences.getBoolean(ONE_TOUCH_LOGIN_KEY, false);
        }
    }

    public void setOneTouchLogin(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(ONE_TOUCH_LOGIN_KEY, b).apply();
        }
    }

    public boolean developerMode() {
        Profile profile = new MeStorage(context).load();
        synchronized (lock) {
            return preferences.getBoolean(DEVELOPER_MODE_KEY, profile != null && profile.sshWirePublicKey != null);
        }
    }

    public void setDeveloperMode(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(DEVELOPER_MODE_KEY, b).apply();
        }
    }

}
