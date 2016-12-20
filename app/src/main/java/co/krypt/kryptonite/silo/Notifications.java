package co.krypt.kryptonite.silo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.policy.NoAuthReceiver;
import co.krypt.kryptonite.policy.Policy;
import co.krypt.kryptonite.policy.UnlockScreenDummyActivity;
import co.krypt.kryptonite.protocol.Request;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Kevin King on 12/5/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Notifications {
    public static void notifySuccess(Context context, Pairing pairing, Request request) {
        Intent resultIntent = new Intent(context, MainActivity.class);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(context.getColor(R.color.colorPrimary))
                        .setContentTitle("Request Approved")
                        .setContentText(pairing.workstationName + ": " + request.signRequest.getCommandOrDefault("SSH Login"))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSound(notificationSound)
                        .setVibrate(new long[]{0, 100});

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(0, mBuilder.build());
    }

    public static void requestApproval(Context context, Pairing pairing, Request request) {
        Intent approveOnceIntent = new Intent(context, UnlockScreenDummyActivity.class);
        approveOnceIntent.setAction(Policy.APPROVE_ONCE + "-" + request.requestID);
        approveOnceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        approveOnceIntent.putExtra("action", Policy.APPROVE_ONCE);
        approveOnceIntent.putExtra("requestID", request.requestID);
        PendingIntent approveOncePendingIntent = PendingIntent.getActivity(context, (Policy.APPROVE_ONCE + "-" + request.requestID).hashCode(), approveOnceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action.Builder approveOnceBuilder = new NotificationCompat.Action.Builder(
                R.drawable.ic_notification_checkmark, "Once", approveOncePendingIntent);

        Intent approveTemporarilyIntent = new Intent(context, UnlockScreenDummyActivity.class);
        approveTemporarilyIntent.setAction(Policy.APPROVE_TEMPORARILY + "-" + request.requestID);
        approveTemporarilyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        approveTemporarilyIntent.putExtra("action", Policy.APPROVE_TEMPORARILY);
        approveTemporarilyIntent.putExtra("requestID", request.requestID);
        PendingIntent approveTemporarilyPendingIntent = PendingIntent.getActivity(context, (Policy.APPROVE_TEMPORARILY + "-" + request.requestID).hashCode(), approveTemporarilyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action.Builder approveTemporarilyBuilder = new NotificationCompat.Action.Builder(
                R.drawable.ic_notification_stopwatch, "For 1 Hour", approveTemporarilyPendingIntent);

        Intent rejectIntent = new Intent(context, NoAuthReceiver.class);
        rejectIntent.setAction(Policy.REJECT + "-" + request.requestID);
        rejectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        rejectIntent.putExtra("action", Policy.REJECT);
        rejectIntent.putExtra("requestID", request.requestID);
        PendingIntent rejectPendingIntent = PendingIntent.getBroadcast(context, (Policy.REJECT + "-" + request.requestID).hashCode(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent resultIntent = new Intent(context, MainActivity.class);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(context.getColor(R.color.colorPrimary))
                        .setContentTitle("Allow Request?")
                        .setContentText(pairing.workstationName + ": " + request.signRequest.getCommandOrDefault("SSH Login"))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSound(notificationSound)
                        .setVibrate(new long[]{0, 100, 100, 100})
                        .addAction(approveOnceBuilder.build())
                        .addAction(approveTemporarilyBuilder.build())
                        .setDeleteIntent(rejectPendingIntent)
                ;

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        request.requestID.hashCode(),
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(request.requestID, 0, mBuilder.build());
    }

    public static void clearRequest(Context context, Request request) {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(request.requestID, 0);
    }
}
