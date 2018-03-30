package co.krypt.krypton.team.onboarding.join;

import android.support.v4.app.Fragment;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2017. KryptCo, Inc.
 */

public enum JoinStage {
    NONE,
    DECRYPT_INVITE,
    VERIFY_EMAIL,
    INPERSON_CHALLENGE_EMAIL_SENT, CHALLENGE_EMAIL_SENT,
    INPERSON_ACCEPT_INVITE, ACCEPT_INVITE,
    COMPLETE;

    public Fragment getFragment() {
        switch (this) {
            case NONE:
                return new OnboardingLoadInviteLinkFragment();
            case DECRYPT_INVITE:
                return new OnboardingLoadInviteLinkFragment();
            case VERIFY_EMAIL:
                return new VerifyEmailFragment();
            case INPERSON_CHALLENGE_EMAIL_SENT:
                return new AcceptInviteFragment();
            case CHALLENGE_EMAIL_SENT:
                return new AcceptInviteFragment();
            case INPERSON_ACCEPT_INVITE:
                return new AcceptInviteFragment();
            case ACCEPT_INVITE:
                return new AcceptInviteFragment();
            case COMPLETE:
                return new JoinCompleteFragment();
        }
        throw new IllegalStateException("unhandled case");
    }
}