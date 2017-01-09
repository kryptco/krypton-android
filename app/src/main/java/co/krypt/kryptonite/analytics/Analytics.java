package co.krypt.kryptonite.analytics;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kevin King on 1/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class Analytics {
    public static final String ANALYTICS_DISABLED_KEY = "ANALYTICS_DISABLED";
    private static Object lock = new Object();
    private SharedPreferences preferences;

    public Analytics(Context context) {
        preferences = context.getSharedPreferences("ANALYTICS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public boolean isDisabled() {
        synchronized (lock) {
            return preferences.getBoolean(ANALYTICS_DISABLED_KEY, false);
        }
    }

    public void setAnalyticsDisabled(boolean disabled) {
        synchronized (lock) {
            preferences.edit().putBoolean(ANALYTICS_DISABLED_KEY, disabled).commit();
        }
    }
}
