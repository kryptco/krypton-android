package co.krypt.krypton.approval;

import android.app.Activity;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import co.krypt.krypton.R;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.policy.Policy;
import co.krypt.krypton.protocol.GitSignRequest;
import co.krypt.krypton.protocol.HostsRequest;
import co.krypt.krypton.protocol.LogDecryptionRequest;
import co.krypt.krypton.protocol.MeRequest;
import co.krypt.krypton.protocol.ReadTeamRequest;
import co.krypt.krypton.protocol.Request;
import co.krypt.krypton.protocol.RequestBody;
import co.krypt.krypton.protocol.SignRequest;
import co.krypt.krypton.protocol.TeamOperationRequest;
import co.krypt.krypton.protocol.UnpairRequest;

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

        // setPositiveButton: right button
        // setNeutralButton: left button
        // setNegativeButton: middle button

        long temporaryApprovalSeconds = Policy.temporaryApprovalSeconds(activity, request);
        boolean temporaryApprovalEnabled = temporaryApprovalSeconds > 0;
        String temporaryApprovalDuration = Policy.temporaryApprovalDuration(activity, request);

        request.body.visit(new RequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(MeRequest meRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(SignRequest signRequest) throws RuntimeException {
                builder.setPositiveButton("Once",
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ONCE));

                if (temporaryApprovalEnabled) {
                    builder.setNeutralButton("All for " + temporaryApprovalDuration,
                            (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ALL_TEMPORARILY));

                    if (signRequest.hostNameVerified) {
                        builder.setNegativeButton("This host for " + temporaryApprovalDuration,
                                (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_THIS_TEMPORARILY));
                    }
                }

                return null;
            }

            @Override
            public Void visit(GitSignRequest gitSignRequest) throws RuntimeException {
                builder.setPositiveButton("Once",
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ONCE));

                if (temporaryApprovalEnabled) {
                    builder.setNeutralButton("All for " + temporaryApprovalDuration,
                            (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ALL_TEMPORARILY));
                }
                return null;
            }

            @Override
            public Void visit(UnpairRequest unpairRequest) throws RuntimeException {
                return null;
            }

            @Override
            public Void visit(HostsRequest hostsRequest) throws RuntimeException {
                builder.setPositiveButton("Allow",
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ONCE));

                if (temporaryApprovalEnabled) {
                    builder.setNeutralButton("All for " + temporaryApprovalDuration,
                            (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ALL_TEMPORARILY));
                }
                return null;
            }

            @Override
            public Void visit(ReadTeamRequest readTeamRequest) throws RuntimeException {
                builder.setPositiveButton("Allow for " + temporaryApprovalDuration,
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ALL_TEMPORARILY));
                return null;
            }

            @Override
            public Void visit(LogDecryptionRequest logDecryptionRequest) throws RuntimeException {
                builder.setPositiveButton("Allow for " + temporaryApprovalDuration,
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ALL_TEMPORARILY));
                return null;
            }

            @Override
            public Void visit(TeamOperationRequest teamOperationRequest) throws RuntimeException {
                builder.setPositiveButton("Allow",
                        (dialog, id) -> Policy.onAction(activity.getApplicationContext(), requestID, Policy.APPROVE_ONCE));
                return null;
            }
        });

        builder.setOnDismissListener(dialogInterface -> {
            Policy.onAction(activity.getApplicationContext(), requestID, Policy.REJECT);
        });
        View requestView = activity.getLayoutInflater().inflate(R.layout.request, null);
        TextView workstationNameText = (TextView) requestView.findViewById(R.id.workstationName);
        workstationNameText.setText(pairing.workstationName);
        ConstraintLayout content = (ConstraintLayout) requestView.findViewById(R.id.content);
        request.fillView(content);
        builder.setView(requestView);
        builder.create().show();
    }
}
