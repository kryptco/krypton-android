package co.krypt.krypton.team;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class LoaderOverlay {
    private static final String TAG = "LoaderOverlay";

    public static void start(Activity a, String text) {
        if (a == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            ConstraintLayout globalContainer = a.findViewById(R.id.team_loader_overlay_container);
            if (globalContainer == null) {
                return;
            }
            View container = globalContainer.findViewById(R.id.teams_loader);
            AppCompatTextView loaderText = container.findViewById(R.id.text);
            ProgressBar progressBar = container.findViewById(R.id.progressBar);
            progressBar.setAlpha(1f);
            AppCompatImageView check = container.findViewById(R.id.check);
            check.setAlpha(0f);
            AppCompatImageView problem = container.findViewById(R.id.problem);
            problem.setAlpha(0f);
            loaderText.setTextColor(a.getColor(R.color.appBlack));
            loaderText.setText(text);
            container.setAlpha(0f);
            container.animate().setDuration(500).alpha(1).start();
        });
    }

    public static void error(Activity a, String text) {
        if (a == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            ConstraintLayout globalContainer = a.findViewById(R.id.team_loader_overlay_container);
            if (globalContainer == null) {
                return;
            }
            ConstraintLayout container = globalContainer.findViewById(R.id.teams_loader);
            container.setAlpha(1f);
            AppCompatTextView loaderText = container.findViewById(R.id.text);
            ProgressBar progressBar = container.findViewById(R.id.progressBar);
            AppCompatImageView check = container.findViewById(R.id.check);
            AppCompatImageView problem = container.findViewById(R.id.problem);
            progressBar.animate().setDuration(500).alpha(0f).start();
            check.setAlpha(0f);
            problem.animate().setDuration(500).alpha(1f).start();
            loaderText.setTextColor(a.getColor(R.color.appReject));
            loaderText.setText(text);
            container.animate().setStartDelay(2000).setDuration(500).alpha(0).start();
        });
    }

    public static void success(Activity a, String text) {
        if (a == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            ConstraintLayout globalContainer = a.findViewById(R.id.team_loader_overlay_container);
            if (globalContainer == null) {
                return;
            }
            ConstraintLayout container = globalContainer.findViewById(R.id.teams_loader);
            container.setAlpha(1f);
            AppCompatTextView loaderText = container.findViewById(R.id.text);
            ProgressBar progressBar = container.findViewById(R.id.progressBar);
            AppCompatImageView check = container.findViewById(R.id.check);
            AppCompatImageView problem = container.findViewById(R.id.problem);
            progressBar.animate().setDuration(500).alpha(0f).start();
            check.animate().setDuration(500).alpha(1f).start();
            problem.setAlpha(0f);
            loaderText.setTextColor(a.getColor(R.color.appBlack));
            loaderText.setText(text);
            progressBar.setAlpha(1);
            progressBar.animate().setDuration(500).alpha(0);
            container.animate().setStartDelay(1000).setDuration(500).alpha(0);
        });
    }
}
