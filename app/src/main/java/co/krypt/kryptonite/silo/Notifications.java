package co.krypt.kryptonite.silo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import javax.annotation.Nullable;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.log.Log;
import co.krypt.kryptonite.onboarding.OnboardingActivity;
import co.krypt.kryptonite.onboarding.OnboardingProgress;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.pgp.PGPPublicKey;
import co.krypt.kryptonite.pgp.publickey.SignedPublicKeySelfCertification;
import co.krypt.kryptonite.policy.NoAuthReceiver;
import co.krypt.kryptonite.policy.Policy;
import co.krypt.kryptonite.policy.UnlockScreenDummyActivity;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.settings.Settings;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Kevin King on 12/5/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Notifications {
    public static void notifySuccess(Context context, Pairing pairing, Request request, @Nullable Log log) {
        if (!new Settings(context).approvedNotificationsEnabled()) {
            return;
        }
        Intent resultIntent = new Intent(context, MainActivity.class);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (!new Settings(context).silenceNotifications()) {
            mBuilder.setSound(notificationSound)
                    .setVibrate(new long[]{0, 100});
        }
        if (request.signRequest != null) {
            mBuilder
                    .setContentTitle("SSH Login Approved")
                    .setContentText(pairing.workstationName + ": " + request.signRequest.display());
            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.result_remote);
            remoteViewsSmall.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.fillShortRemoteViews(remoteViewsSmall, true, log != null ? log.getSignature() : null);
            mBuilder.setContent(remoteViewsSmall);
        }
        if (request.gitSignRequest != null) {
            mBuilder
                    .setContentTitle(request.gitSignRequest.title() + " Approved")
                    .setContentText(pairing.workstationName + ": " + request.gitSignRequest.display());

            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.result_remote);
            remoteViewsSmall.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.gitSignRequest.fillShortRemoteViews(remoteViewsSmall, true, log != null ? log.getSignature() : null);
            mBuilder.setContent(remoteViewsSmall);

            RemoteViews remoteViewsBig = new RemoteViews(context.getPackageName(), R.layout.result_remote);
            remoteViewsBig.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.gitSignRequest.fillRemoteViews(remoteViewsBig, true, log != null ? log.getSignature() : null);
            mBuilder.setCustomBigContentView(remoteViewsBig);
        }

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

    public static void notifyReject(Context context, Pairing pairing, Request request, String title) {
        if (!new Settings(context).approvedNotificationsEnabled()) {
            return;
        }
        Intent resultIntent = new Intent(context, MainActivity.class);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(Color.RED)
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (!new Settings(context).silenceNotifications()) {
            mBuilder.setSound(notificationSound)
                    .setVibrate(new long[]{0, 100, 100, 100});
        }
        if (request.signRequest != null) {
            mBuilder
                    .setContentText(pairing.workstationName + ": " + request.signRequest.display());
        }
        if (request.gitSignRequest != null) {
            mBuilder
                    .setContentTitle(request.gitSignRequest.title() + " Rejected")
                    .setContentText(pairing.workstationName + ": " + request.gitSignRequest.display());

            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.result_remote);
            remoteViewsSmall.setTextViewText(R.id.workstationName, pairing.workstationName);
            remoteViewsSmall.setTextViewText(R.id.header, "Rejected Request From");
            request.gitSignRequest.fillShortRemoteViews(remoteViewsSmall, false, null);
            mBuilder.setCustomContentView(remoteViewsSmall);

            RemoteViews remoteViewsBig = new RemoteViews(context.getPackageName(), R.layout.result_remote);
            remoteViewsBig.setTextViewText(R.id.workstationName, pairing.workstationName);
            remoteViewsBig.setTextViewText(R.id.header, "Rejected Request From");
            request.gitSignRequest.fillRemoteViews(remoteViewsBig, false, null);
            mBuilder.setCustomBigContentView(remoteViewsBig);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
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



        Intent clickIntent = new Intent(context, MainActivity.class);
        if (new OnboardingProgress(context).inProgress()) {
            clickIntent.setClass(context, OnboardingActivity.class);
        }
        clickIntent.setAction("CLICK-" + request.requestID);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        clickIntent.putExtra("requestID", request.requestID);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, ("CLICK-" + request.requestID).hashCode(), clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .addAction(approveOnceBuilder.build())
                        .addAction(approveTemporarilyBuilder.build())
                        .setDeleteIntent(rejectPendingIntent)
                        .setContentIntent(clickPendingIntent)
                        .setAutoCancel(true)
                ;
        if (request.signRequest != null) {
            mBuilder
                    .setContentTitle("Allow SSH Login?")
                    .setContentText(pairing.workstationName + ": " + request.signRequest.display());
            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.request_no_action_remote);
            remoteViewsSmall.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.fillShortRemoteViews(remoteViewsSmall, null, null);
            mBuilder.setContent(remoteViewsSmall);

            RemoteViews remoteViewsBig = new RemoteViews(context.getPackageName(), R.layout.request_remote);
            remoteViewsBig.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.fillRemoteViews(remoteViewsBig, null, null);
            remoteViewsBig.setOnClickPendingIntent(R.id.reject, rejectPendingIntent);
            remoteViewsBig.setOnClickPendingIntent(R.id.allowOnce, approveOncePendingIntent);
            remoteViewsBig.setOnClickPendingIntent(R.id.allowTemporarily, approveTemporarilyPendingIntent);
            mBuilder.setCustomBigContentView(remoteViewsBig);
        }
        if (request.gitSignRequest != null) {
            mBuilder
                    .setContentTitle("Allow " + request.gitSignRequest.title() + "?")
                    .setContentText(pairing.workstationName + ": " + request.gitSignRequest.display());

            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.request_no_action_remote);
            remoteViewsSmall.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.gitSignRequest.fillShortRemoteViews(remoteViewsSmall, null, null);
            mBuilder.setContent(remoteViewsSmall);

            RemoteViews remoteViewsBig = new RemoteViews(context.getPackageName(), R.layout.request_remote);
            remoteViewsBig.setTextViewText(R.id.workstationName, pairing.workstationName);
            request.gitSignRequest.fillRemoteViews(remoteViewsBig, null, null);
            remoteViewsBig.setOnClickPendingIntent(R.id.reject, rejectPendingIntent);
            remoteViewsBig.setOnClickPendingIntent(R.id.allowOnce, approveOncePendingIntent);
            remoteViewsBig.setOnClickPendingIntent(R.id.allowTemporarily, approveTemporarilyPendingIntent);
            mBuilder.setCustomBigContentView(remoteViewsBig);
        }
        if (!new Settings(context).silenceNotifications()) {
            mBuilder.setSound(notificationSound)
                    .setVibrate(new long[]{0, 100, 100, 100});
        }

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(request.requestID, 0, mBuilder.build());
    }

    public static void notifyPGPKeyExport(Context context, PGPPublicKey pubkey) {
        Intent clickIntent = new Intent(context, MainActivity.class);
        if (new OnboardingProgress(context).inProgress()) {
            clickIntent.setClass(context, OnboardingActivity.class);
        }
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, ("CLICK-PGPPUBKEY").hashCode(), clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String userIDs = "";
        for (SignedPublicKeySelfCertification signedIdentity : pubkey.signedIdentities) {
            userIDs += signedIdentity.certification.userIDPacket.userID.toString() + "\n";
        }

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Exported PGP Public Key");
        for (SignedPublicKeySelfCertification signedIdentity : pubkey.signedIdentities) {
            inboxStyle.addLine(signedIdentity.certification.userIDPacket.userID.toString());
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setContentTitle("Exported PGP Public Key")
                        .setContentText(userIDs.trim())
                        .setSmallIcon(R.drawable.ic_notification_white)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(clickPendingIntent)
                        .setAutoCancel(true)
                        .setStyle(inboxStyle)
                ;
        if (!new Settings(context).silenceNotifications()) {
            mBuilder.setSound(notificationSound)
                    .setVibrate(new long[]{0, 100, 100, 100});
        }

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(1, mBuilder.build());
    }

    public static void clearRequest(Context context, Request request) {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(request.requestID, 0);
    }
}
