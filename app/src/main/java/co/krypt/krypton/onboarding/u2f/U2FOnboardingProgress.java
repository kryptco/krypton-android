package co.krypt.krypton.onboarding.u2f;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class U2FOnboardingProgress {
    private static final String CURRENT_STAGE_KEY = "CURRENT_STAGE";

    private static Object lock = new Object();
    private SharedPreferences preferences;

    public U2FOnboardingProgress(Context context) {
        preferences = context.getSharedPreferences("U2F_ONBOARDING_PROGRESS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public void reset() {
        synchronized (lock) {
            setStage(U2FOnboardingStage.NONE);
        }
    }

    public boolean inProgress() {
        synchronized (lock) {
            return !currentStage().equals(U2FOnboardingStage.DONE);
        }
    }

    public U2FOnboardingStage currentStage() {
        synchronized (lock) {
            try {
                String stage = preferences.getString(CURRENT_STAGE_KEY, null);
                if (stage == null) {
                    return U2FOnboardingStage.NONE;
                }
                return U2FOnboardingStage.valueOf(stage);
            } catch(Exception e) {
                return U2FOnboardingStage.NONE;
            }
        }
    }

    public void setStage(U2FOnboardingStage stage) {
        synchronized (lock) {
            preferences.edit().putString(CURRENT_STAGE_KEY, stage.toString()).apply();
        }
    }

}
