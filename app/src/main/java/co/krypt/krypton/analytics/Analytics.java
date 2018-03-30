package co.krypt.krypton.analytics;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.krypt.krypton.BuildConfig;

/**
 * Created by Kevin King on 1/9/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class Analytics {
    public static final String ANALYTICS_DISABLED_KEY = "ANALYTICS_DISABLED";
    public static final String CLIENT_ID_KEY = "CLIENT_ID";
    public static final String PUBLISHED_EMAIL_KEY = "PUBLISHED_EMAIL";
    private static final Object lock = new Object();
    private SharedPreferences preferences;

    private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private static final String TRACKING_ID() {
        if (BuildConfig.DEBUG) {
            return "UA-86173430-1";
        } else {
            return "UA-86173430-2";
        }
    }

    public String getClientID() {
        synchronized (lock) {
            if (!preferences.contains(CLIENT_ID_KEY)) {
                preferences.edit().putString(CLIENT_ID_KEY, Base64.encodeAsString(SecureRandom.getSeed(16))).apply();
            }
            return preferences.getString(CLIENT_ID_KEY, "");
        }
    }

    private String getPublishedEmail() {
        synchronized (lock) {
            return preferences.getString(PUBLISHED_EMAIL_KEY, "");
        }
    }

    public void publishEmailToTeamsIfNeeded(final String email) {
        synchronized (lock) {
            if (isDisabled()) {
                return;
            }
            if (!email.equals(getPublishedEmail())) {
                threadPool.submit(() -> {
                    Uri.Builder uri = new Uri.Builder().scheme("https").authority("teams.krypt.co")
                            .appendQueryParameter("id", getClientID())
                            .appendQueryParameter("email", email);
                    try {
                        URL url = new URL(uri.toString());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("PUT");
                        try {
                            InputStream in = new BufferedInputStream(connection.getInputStream());
                            preferences.edit().putString(PUBLISHED_EMAIL_KEY, email).apply();
                        } finally {
                            connection.disconnect();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void post(String clientID, HashMap<String, String> params, boolean force) {
        if (isDisabled() && !force) {
            return;
        }
        HashMap<String, String> defaultParams = new HashMap<String, String>();
        defaultParams.put("v", "1");
        defaultParams.put("tid", TRACKING_ID());
        defaultParams.put("cid", clientID);
        String userAgentString = System.getProperty("http.agent") + " Version/" + BuildConfig.VERSION_NAME + " kr/" + BuildConfig.VERSION_NAME;
        defaultParams.put("ua", userAgentString);
        defaultParams.put("cd4", "android");
        defaultParams.put("cd5", "android " + String.valueOf(android.os.Build.VERSION.SDK_INT));
        defaultParams.put("cd6", DeviceName.getDeviceName());
        defaultParams.put("cd7", clientID);
        defaultParams.put("cd9", BuildConfig.VERSION_NAME);

        defaultParams.putAll(params);

        final Uri.Builder uri = new Uri.Builder();
        uri.scheme("https").path("/collect").authority("www.google-analytics.com");
        for (Map.Entry<String, String> param: defaultParams.entrySet()) {
            uri.appendQueryParameter(param.getKey(), param.getValue());
        }
        threadPool.submit(() -> {
            try {
                URL url = new URL(uri.build().toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void postPageView(String page) {
        HashMap<String, String> params = new HashMap<>();
        params.put("t", "pageview");
        params.put("dt", page);
        params.put("dp", "/" + page);
        params.put("dh", "co.krypt.kryptonite");

        post(getClientID(), params, false);
    }

    public void postEvent(String category, String action, @Nullable String label, @Nullable Integer value, boolean force) {
        HashMap<String, String> params = new HashMap<>();
        params.put("t", "event");
        params.put("ec", category);
        params.put("ea", action);
        if (label != null) {
            params.put("el", label);
        }
        if (value != null) {
            params.put("ev", String.valueOf(value));
        }

        post(getClientID(), params, force);
    }

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
            postEvent("analytics", disabled ? "disabled" : "enabled", null, null, true);
            preferences.edit().putBoolean(ANALYTICS_DISABLED_KEY, disabled).apply();
        }
    }
}
