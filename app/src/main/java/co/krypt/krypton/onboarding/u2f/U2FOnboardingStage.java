package co.krypt.krypton.onboarding.u2f;

/**
 * Created by Kevin King on 1/11/17.
 * Copyright 2016. KryptCo, Inc.
 */

public enum U2FOnboardingStage {
    NONE, GENERATE, GENERATING, FIRST_PAIR_EXT, FIRST_PAIR_CLI, TEST_SSH, DONE
}
