package co.krypt.kryptonite.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class OnboardingProgress {
    private static final String CURRENT_STAGE_KEY = "CURRENT_STAGE";

    private static Object lock = new Object();
    private SharedPreferences preferences;

    public OnboardingProgress(Context context) {
        preferences = context.getSharedPreferences("ONBOARDING_PROGRESS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public void reset() {
        synchronized (lock) {
            setStage(OnboardingStage.NONE);
        }
    }

    public boolean inProgress() {
        synchronized (lock) {
            return !currentStage().equals(OnboardingStage.NONE);
        }
    }

    public OnboardingStage currentStage() {
        synchronized (lock) {
            try {
                String stage = preferences.getString(CURRENT_STAGE_KEY, null);
                if (stage == null) {
                    return OnboardingStage.NONE;
                }
                return OnboardingStage.valueOf(stage);
            } catch(Exception e) {
                return OnboardingStage.NONE;
            }
        }
    }

    public void setStage(OnboardingStage stage) {
        synchronized (lock) {
            preferences.edit().putString(CURRENT_STAGE_KEY, stage.toString()).commit();
        }
    }

}
