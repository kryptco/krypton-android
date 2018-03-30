package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.content.SharedPreferences;

import co.krypt.krypton.protocol.JSON;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class CreateTeamProgress {
    private static final String CURRENT_STAGE_KEY = "CURRENT_STAGE";
    private static final String TEAM_DATA_KEY = "TEAM_DATA";

    public interface TeamDataStateChange {
        CreateStage update(CreateStage stage, CreateTeamData data);
    }

    private final static Object lock = new Object();
    private SharedPreferences preferences;

    public CreateTeamProgress(Context context) {
        preferences = context.getSharedPreferences("TEAM_ONBOARDING_CREATE_PROGRESS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public void reset() {
        synchronized (lock) {
            setTeamOnboardingData(new CreateTeamData());
            setStage(CreateStage.NONE);
        }
    }

    public boolean inProgress() {
        synchronized (lock) {
            return !currentStage().equals(CreateStage.NONE);
        }
    }

    public CreateStage currentStage() {
        synchronized (lock) {
            try {
                String stage = preferences.getString(CURRENT_STAGE_KEY, null);
                if (stage == null) {
                    return CreateStage.NONE;
                }
                return CreateStage.valueOf(stage);
            } catch(Exception e) {
                return CreateStage.NONE;
            }
        }
    }

    public void setStage(CreateStage stage) {
        synchronized (lock) {
            preferences.edit().putString(CURRENT_STAGE_KEY, stage.toString()).apply();
        }
    }

    public CreateTeamData getTeamOnboardingData() {
        synchronized (lock) {
            String teamDataJson = preferences.getString(TEAM_DATA_KEY, null);
            if (teamDataJson == null) {
                return new CreateTeamData();
            }
            CreateTeamData data = JSON.fromJson(teamDataJson, CreateTeamData.class);
            if (data == null) {
                return new CreateTeamData();
            }
            return data;
        }
    }

    public void setTeamOnboardingData(CreateTeamData data) {
        synchronized (lock) {
            preferences.edit().putString(TEAM_DATA_KEY, JSON.toJson(data)).apply();
        }
    }

    public void updateTeamData(TeamDataStateChange f) {
        synchronized (lock) {
            CreateTeamData data = getTeamOnboardingData();
            CreateStage stage = currentStage();
            setStage(f.update(stage, data));
            setTeamOnboardingData(data);
        }
    }

}
