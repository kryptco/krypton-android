package co.krypt.krypton.onboarding.devops;

import android.content.Context;
import android.content.SharedPreferences;

import co.krypt.krypton.onboarding.u2f.U2FOnboardingStage;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class DevopsOnboardingProgress {
    private static final String CURRENT_STAGE_KEY = "CURRENT_STAGE";

    private static Object lock = new Object();
    private SharedPreferences preferences;

    public DevopsOnboardingProgress(Context context) {
        preferences = context.getSharedPreferences("DEVOPS_ONBOARDING_PROGRESS_PREFERENCES", Context.MODE_PRIVATE);
}

    public void reset() {
        synchronized (lock) {
            setStage(DevopsOnboardingStage.NONE);
        }
    }

    public boolean inProgress() {
        synchronized (lock) {
            return !currentStage().equals(U2FOnboardingStage.DONE);
        }
    }

    public DevopsOnboardingStage currentStage() {
        synchronized (lock) {
            try {
                String stage = preferences.getString(CURRENT_STAGE_KEY, null);
                if (stage == null) {
                    return DevopsOnboardingStage.NONE;
                }
                return DevopsOnboardingStage.valueOf(stage);
            } catch(Exception e) {
                return DevopsOnboardingStage.NONE;
            }
        }
    }

    public void setStage(DevopsOnboardingStage stage) {
        synchronized (lock) {
            preferences.edit().putString(CURRENT_STAGE_KEY, stage.toString()).apply();
        }
    }

}
