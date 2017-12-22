package co.krypt.kryptonite.uiutils;

import android.text.format.DateUtils;

/**
 * Created by Kevin King on 12/17/17.
 * Copyright 2017. KryptCo, Inc.
 */

public class TimeUtils {
    public static String formatDurationMillis(long millis) {
        String str = "";
        if (millis >= DateUtils.DAY_IN_MILLIS) {
            str += millis / DateUtils.DAY_IN_MILLIS + "d ";
            millis %= DateUtils.DAY_IN_MILLIS;
        }
        if (millis >= DateUtils.HOUR_IN_MILLIS) {
            str += millis / DateUtils.HOUR_IN_MILLIS + "h ";
            millis %= DateUtils.HOUR_IN_MILLIS;
        }
        if (millis >= DateUtils.MINUTE_IN_MILLIS) {
            str += millis / DateUtils.MINUTE_IN_MILLIS + "m ";
            millis %= DateUtils.MINUTE_IN_MILLIS;
        }
        if (millis >= DateUtils.SECOND_IN_MILLIS) {
            str += millis / DateUtils.SECOND_IN_MILLIS + "s ";
            millis %= DateUtils.SECOND_IN_MILLIS;
        }
        if (millis >= 1) {
            str += millis + "ms ";
        }
        return str.trim();
    }
    public static String formatDurationSeconds(long seconds) {
        return formatDurationMillis(seconds * 1000);
    }
}
