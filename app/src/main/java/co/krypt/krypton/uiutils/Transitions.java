package co.krypt.krypton.uiutils;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 2/7/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Transitions {
    public static FragmentTransaction beginFade(Fragment f) {
        return f.getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
    }
    public static FragmentTransaction beginSlide(Fragment f) {
        return f.getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
    }
    public static FragmentTransaction beginInstant(Fragment f) {
        return f.getFragmentManager().beginTransaction();
    }
}
