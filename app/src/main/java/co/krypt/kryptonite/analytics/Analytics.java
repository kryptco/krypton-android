package co.krypt.kryptonite.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.amazonaws.util.Base64;

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

public class Analytics {
    public static final String ANALYTICS_DISABLED_KEY = "ANALYTICS_DISABLED";
    public static final String CLIENT_ID_KEY = "CLIENT_ID";
    private static Object lock = new Object();
    private SharedPreferences preferences;

    //TODO: bump to prod
    private static final String TRACKING_ID = "UA-86173430-1";

    public String getClientID() {
        synchronized (lock) {
            if (!preferences.contains(CLIENT_ID_KEY)) {
                preferences.edit().putString(CLIENT_ID_KEY, Base64.encodeAsString(SecureRandom.getSeed(16))).commit();
            }
            return preferences.getString(CLIENT_ID_KEY, "");
        }
    }

    private void post(String clientID, HashMap<String, String> params, boolean force) {
        if (isDisabled() && !force) {
            return;
        }
        HashMap<String, String> defaultParams = new HashMap<String, String>();
        defaultParams.put("v", "1");
        defaultParams.put("tid", TRACKING_ID);
        defaultParams.put("cid", clientID);
        String userAgentString = System.getProperty("http.agent") + " Version/" + BuildConfig.VERSION_NAME + " kr/" + BuildConfig.VERSION_NAME;
        defaultParams.put("ua", userAgentString);

        defaultParams.putAll(params);

        final Uri.Builder uri = new Uri.Builder();
        uri.scheme("https").path("/collect").authority("www.google-analytics.com");
        for (Map.Entry<String, String> param: defaultParams.entrySet()) {
            uri.appendQueryParameter(param.getKey(), param.getValue());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
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
            }
        }).start();

//        defaultParams.put("cd1", BuildConfig.VERSION_NAME);
//        defaultParams.put("cd2", Build.VERSION.RELEASE);
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
            preferences.edit().putBoolean(ANALYTICS_DISABLED_KEY, disabled).commit();
        }
    }
}
