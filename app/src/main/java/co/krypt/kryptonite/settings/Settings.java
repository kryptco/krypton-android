package co.krypt.kryptonite.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.amazonaws.util.Base64;
import com.jaredrummler.android.device.DeviceName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import co.krypt.kryptonite.BuildConfig;

/**
 * Created by Kevin King on 1/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class Settings {
    public static final String ENABLE_APPROVED_NOTIFICATIONS_KEY = "ENABLE_APPROVED_NOTFICATIONS";
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
}
