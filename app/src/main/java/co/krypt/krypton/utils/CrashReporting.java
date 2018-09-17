package co.krypt.krypton.utils;

import com.crashlytics.android.Crashlytics;
import com.github.anrwatchdog.ANRWatchDog;

import co.krypt.krypton.BuildConfig;

/**
 * Created by Kevin King on 3/21/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class CrashReporting {
    private static ANRWatchDog anrWatchDog = null;
    public static synchronized void startANRReporting() {
        if (anrWatchDog == null) {
            anrWatchDog = new ANRWatchDog()
                    .setIgnoreDebugger(true)
                    .setReportMainThreadOnly();
            if (!BuildConfig.DEBUG) {
                anrWatchDog.setANRListener(Crashlytics::logException);
            }
            anrWatchDog.start();
        }

    }
}
