package co.krypt.krypton.team.onboarding.create;

import android.support.v4.app.Fragment;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2017. KryptCo, Inc.
 */

public enum CreateStage {
    NONE, AUDIT_LOGS, POLICY, PIN_HOSTS, VERIFY_EMAIL, EMAIL_CHALLENGE_SENT, LOAD_TEAM, TEAM_CREATING, COMPLETE;

    public Fragment getFragment() {
        switch (this) {
            case NONE:
                return new OnboardingCreateFragment();
            case AUDIT_LOGS:
                return new OnboardingAuditLogsFragment();
            case POLICY:
                return new OnboardingPolicyFragment();
            case PIN_HOSTS:
                return new OnboardingPinHostsFragment();
            case VERIFY_EMAIL:
                return new OnboardingVerifyEmailFragment();
            case EMAIL_CHALLENGE_SENT:
                return new OnboardingVerifyEmailFragment();
            case LOAD_TEAM:
                return new OnboardingLoadTeamFragment();
            case TEAM_CREATING:
                return new OnboardingLoadTeamFragment();
            case COMPLETE:
                return new OnboardingCompleteFragment();
            default:
                return null;
        }
    }
}