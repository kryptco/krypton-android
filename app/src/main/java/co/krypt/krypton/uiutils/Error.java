package co.krypt.krypton.uiutils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by Kevin King on 2/7/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Error {
    public static void shortToast(Context context, String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        });
    }
    public static void longToast(Context context, String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        });
    }
}
