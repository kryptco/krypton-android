package co.krypt.kryptonite.policy;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import co.krypt.kryptonite.MainActivity;

/**
 * Created by Kevin King on 1/10/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class LocalAuthentication {
    private static Runnable lastSuccessCallback = null;

    public static synchronized void requestAuthentication(Activity context, String title, String description, Runnable onSuccess) {
        lastSuccessCallback = onSuccess;
        Intent intent = ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE))
                .createConfirmDeviceCredentialIntent(title, description);
        if (intent != null) {
            context.startActivityForResult(intent, MainActivity.USER_AUTHENTICATION_REQUEST);
        } else {
            onSuccess();
        }
    }

    public static synchronized void onSuccess() {
        final Runnable successCallback = lastSuccessCallback;
        if (successCallback != null) {
            successCallback.run();
        }
        lastSuccessCallback = null;
    }

}
