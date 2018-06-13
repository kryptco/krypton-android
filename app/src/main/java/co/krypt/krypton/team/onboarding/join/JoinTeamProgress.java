package co.krypt.krypton.team.onboarding.join;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.TeamDataProvider;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class JoinTeamProgress {
    private static final String CURRENT_STAGE_KEY = "CURRENT_STAGE";
    private static final String TEAM_DATA_KEY = "TEAM_DATA";
    private static final String TAG = "JoinTeamProgress";

    public interface TeamDataStateChange {
        JoinStage update(JoinStage stage, JoinTeamData data);
    }

    private final static Object lock = new Object();
    private SharedPreferences preferences;

    public JoinTeamProgress(Context context) {
        preferences = context.getSharedPreferences("TEAM_ONBOARDING_JOIN_PROGRESS_PREFERENCES", Context.MODE_PRIVATE);
    }

    public void reset() {
        synchronized (lock) {
            setTeamOnboardingData(new JoinTeamData());
            setStage(JoinStage.NONE);
        }
    }

    public void resetAndDeleteTeam(Context context) {
        synchronized (lock) {
            setTeamOnboardingData(new JoinTeamData());
            setStage(JoinStage.NONE);
            try {
                TeamDataProvider.deleteDB(context);
            } catch (Native.NotLinked notLinked) {
                notLinked.printStackTrace();
            }
        }
    }

    public boolean inProgress() {
        synchronized (lock) {
            return !currentStage().equals(JoinStage.NONE);
        }
    }

    public JoinStage currentStage() {
        synchronized (lock) {
            try {
                String stage = preferences.getString(CURRENT_STAGE_KEY, null);
                if (stage == null) {
                    return JoinStage.NONE;
                }
                return JoinStage.valueOf(stage);
            } catch(Exception e) {
                return JoinStage.NONE;
            }
        }
    }

    public void setStage(JoinStage stage) {
        synchronized (lock) {
            Log.i(TAG, "JoinStage " + currentStage() + " -> " + stage.toString());
            preferences.edit().putString(CURRENT_STAGE_KEY, stage.toString()).apply();
        }
    }

    public JoinTeamData getTeamOnboardingData() {
        synchronized (lock) {
            String teamDataJson = preferences.getString(TEAM_DATA_KEY, null);
            if (teamDataJson == null) {
                return new JoinTeamData();
            }
            JoinTeamData data = JSON.fromJson(teamDataJson, JoinTeamData.class);
            if (data == null) {
                return new JoinTeamData();
            }
            return data;
        }
    }

    public void setTeamOnboardingData(JoinTeamData data) {
        synchronized (lock) {
            preferences.edit().putString(TEAM_DATA_KEY, JSON.toJson(data)).apply();
        }
    }

    public void updateTeamData(TeamDataStateChange f) {
        synchronized (lock) {
            JoinTeamData data = getTeamOnboardingData();
            JoinStage stage = currentStage();
            setStage(f.update(stage, data));
            setTeamOnboardingData(data);
        }
    }

}
