package co.krypt.kryptonite.approval;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.policy.Policy;
import co.krypt.kryptonite.protocol.Request;

/**
 * Created by Kevin King on 5/5/17.
 * Copyright 2016. KryptCo, Inc.
 */

public class ApprovalDialog {
    private static final String TAG = "ApprovalDialog";

    public static final String NOTIFICATION_CLICK_ACTION = "co.krypt.action.NOTIFICATION_CLICK";

    public static void showApprovalDialog(final Activity activity, final String requestID) {
        Pair<Pairing, Request> pendingRequestAndPairing = Policy.getPendingRequestAndPairing(requestID);
        if (pendingRequestAndPairing == null) {
            Log.e(TAG, "user clicked notification for unknown request");
            return;
        }
        Pairing pairing = pendingRequestAndPairing.first;
        Request request = pendingRequestAndPairing.second;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setPositiveButton("Once",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id) {
                        Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ONCE);
                    }
                });

        builder.setNeutralButton("Reject",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id) {
                        Policy.onAction(activity.getApplicationContext(), requestID, Policy.REJECT);
                    }
                });

        builder.setNegativeButton("For 1 Hour",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id) {
                        Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_TEMPORARILY);
                    }
                });
        builder.setCancelable(false);
        if (request.signRequest != null) {
            builder.setTitle("Allow SSH Login?");
            builder.setMessage(pairing.workstationName + ": " + request.signRequest.display());
        }
        if (request.gitSignRequest != null) {
//            builder.setTitle("Allow " + request.gitSignRequest.title() + "?");
//            builder.setMessage(pairing.workstationName + "\n" + request.gitSignRequest.display());

            View requestView = activity.getLayoutInflater().inflate(R.layout.request, null);
            TextView workstationNameText = (TextView) requestView.findViewById(R.id.workstationName);
            workstationNameText.setText(pairing.workstationName);
            ConstraintLayout content = (ConstraintLayout) requestView.findViewById(R.id.content);
            request.gitSignRequest.fillView(content);
            builder.setView(requestView);
        }
        builder.create().show();
    }
}
