package co.krypt.kryptonite.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kevin King on 1/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class Settings {
    public static final String ENABLE_APPROVED_NOTIFICATIONS_KEY = "ENABLE_APPROVED_NOTIFICATIONS";
    public static final String SILENCE_NOTIFICATIONS_KEY = "SILENCE_NOTIFICATIONS";

    private static Object lock = new Object();
    private SharedPreferences preferences;

    public Settings(Context context) {
        preferences = context.getSharedPreferences("SETTINGS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public boolean approvedNotificationsEnabled() {
        synchronized (lock) {
            return preferences.getBoolean(ENABLE_APPROVED_NOTIFICATIONS_KEY, true);
        }
    }

    public void setApprovedNotificationsEnabled(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(ENABLE_APPROVED_NOTIFICATIONS_KEY, b).commit();
        }
    }

    public boolean silenceNotifications() {
        synchronized (lock) {
            return preferences.getBoolean(SILENCE_NOTIFICATIONS_KEY, false);
        }
    }

    public void setSilenceNotifications(boolean b) {
        synchronized (lock) {
            preferences.edit().putBoolean(SILENCE_NOTIFICATIONS_KEY, b).commit();
        }
    }

}
