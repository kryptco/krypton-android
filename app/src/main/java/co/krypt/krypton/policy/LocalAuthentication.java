package co.krypt.krypton.policy;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import co.krypt.krypton.R;
import co.krypt.kryptonite.MainActivity;

/**
 * Created by Kevin King on 1/10/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class LocalAuthentication {
    private static Runnable lastSuccessCallback = null;

    public static synchronized void requestAuthentication(Activity context, String title, String description, Runnable onSuccess) {
        lastSuccessCallback = onSuccess;
        Intent intent = ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE))
                .createConfirmDeviceCredentialIntent(title, description + " Enter your device PIN or pattern to confirm.");
        if (intent != null) {
            context.startActivityForResult(intent, MainActivity.USER_AUTHENTICATION_REQUEST);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setIcon(R.mipmap.ic_launcher)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton("Confirm", (d, w) -> onSuccess())
                    .setNegativeButton("Cancel", (d, w) -> {});
            builder.show();
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
