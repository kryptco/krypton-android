package co.krypt.krypton.log;

import android.support.constraint.ConstraintLayout;
import android.view.View;

import javax.annotation.Nullable;

/**
 * Created by Kevin King on 6/19/17.
 * Copyright 2017. KryptCo, Inc.
 */

public interface Log {
    long unixSeconds();

    String shortDisplay();

    String longDisplay();

    @Nullable
    View fillShortView(ConstraintLayout container);

    @Nullable
    View fillLongView(ConstraintLayout container);

    @Nullable String getSignature();
}
