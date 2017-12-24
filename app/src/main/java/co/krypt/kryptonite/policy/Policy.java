package co.krypt.kryptonite.policy;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import java.util.HashMap;

import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.db.OpenDatabaseHelper;
import co.krypt.kryptonite.git.CommitInfo;
import co.krypt.kryptonite.git.TagInfo;
import co.krypt.kryptonite.pairing.Pairing;
import co.krypt.kryptonite.protocol.GitSignRequest;
import co.krypt.kryptonite.protocol.GitSignRequestBody;
import co.krypt.kryptonite.protocol.HostsRequest;
import co.krypt.kryptonite.protocol.MeRequest;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.RequestBody;
import co.krypt.kryptonite.protocol.SignRequest;
import co.krypt.kryptonite.protocol.UnpairRequest;
import co.krypt.kryptonite.silo.Notifications;
import co.krypt.kryptonite.silo.Silo;

/**
 * Created by Kevin King on 12/18/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Policy {
    private static final String TAG = "Policy";
    public static final String APPROVE_ONCE = "approve-once";
    //  approve a subset of a request type, such as a single SSH user@host
    public static final String APPROVE_THIS_TEMPORARILY = "approve-this-temporarily";
    //  approve all requests of a specific type, such as all SSH requests
    public static final String APPROVE_ALL_TEMPORARILY = "approve-all-temporarily";
    public static final String REJECT = "reject";

    public static final long TEMPORARY_APPROVAL_SECONDS = 3600*3;

    public static String temporaryApprovalDuration() {
        return co.krypt.kryptonite.uiutils.TimeUtils.formatDurationMillis(TEMPORARY_APPROVAL_SECONDS * 1000);
    }

    private static final HashMap<String, Pair<Pairing, Request>> pendingRequestCache = new HashMap<>();

    public static synchronized boolean requestApproval(Context context, Pairing pairing, Request request) {
        if (pendingRequestCache.get(request.requestID) != null) {
            return false;
        }
        pendingRequestCache.put(request.requestID, new Pair<>(pairing, request));
        Notifications.requestApproval(context, pairing, request);
        return true;
    }

    public static synchronized boolean isApprovedNow(Context context, Pairing pairing, Request request) {
        Silo silo = Silo.shared(context);
        try {
            Dao<Approval, Long> db = silo.pairings().dbHelper.getApprovalDao();
            boolean neverAsk = Silo.shared(context).pairings().getApproved(pairing);
            return request.body.visit(new RequestBody.Visitor<Boolean, Exception>() {
                @Override
                public Boolean visit(MeRequest meRequest) throws Exception {
                    return true;
                }

                @Override
                public Boolean visit(SignRequest signRequest) throws Exception {
                    String hostname = signRequest.verifiedHostNameOrDefault(null);
                    if (hostname == null) {
                        if (!Silo.shared(context).pairings().requireUnknownHostManualApproval(pairing)) {
                            return Approval.isSSHAnyHostApprovedNow(db, pairing.uuid, TEMPORARY_APPROVAL_SECONDS) ||
                                    neverAsk;
                        }
                        return false;
                    }
                    if (!Silo.shared(context).hasKnownHost(hostname)) {
                        return false;
                    }
                    return Approval.isSSHUserHostApprovedNow(db, pairing.uuid, TEMPORARY_APPROVAL_SECONDS, signRequest.user(), hostname) ||
                            Approval.isSSHAnyHostApprovedNow(db, pairing.uuid, TEMPORARY_APPROVAL_SECONDS) ||
                            neverAsk;
                }

                @Override
                public Boolean visit(GitSignRequest gitSignRequest) throws Exception {
                    try {
                        if (neverAsk) {
                            return true;
                        }
                        return gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Boolean, Exception>() {
                            @Override
                            public Boolean visit(CommitInfo commit) throws Exception {
                                return Approval.isGitCommitApprovedNow(db, pairing.uuid, TEMPORARY_APPROVAL_SECONDS);
                            }

                            @Override
                            public Boolean visit(TagInfo tag) throws Exception {
                                return Approval.isGitTagApprovedNow(db, pairing.uuid, TEMPORARY_APPROVAL_SECONDS);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                @Override
                public Boolean visit(UnpairRequest unpairRequest) throws Exception {
                    return true;
                }

                @Override
                public Boolean visit(HostsRequest hostsRequest) throws Exception {
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized Pair<Pairing, Request> getPendingRequestAndPairing(String requestID) {
        return pendingRequestCache.get(requestID);
    }

    public static synchronized void onAction(final Context context, final String requestID, final String action) {
        Log.i(TAG, action + " requestID " + requestID);
        final Pair<Pairing, Request> pairingAndRequest = pendingRequestCache.remove(requestID);
        if (pairingAndRequest == null) {
            Log.e(TAG, "requestID " + requestID + " not pending");
            return;
        }

        Silo silo = Silo.shared(context);
        OpenDatabaseHelper db = silo.pairings().dbHelper;

        Notifications.clearRequest(context, pairingAndRequest.second);
        switch (action) {
            case APPROVE_ONCE:
                try {
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background approve", "once", null, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case APPROVE_ALL_TEMPORARILY:
                try {
                    pairingAndRequest.second.body.visit(new RequestBody.Visitor<Void, Exception>() {
                        @Override
                        public Void visit(MeRequest meRequest) throws Exception {
                            return null;
                        }

                        @Override
                        public Void visit(SignRequest signRequest) throws Exception {
                            Approval.approveSSHAnyHost(db, pairingAndRequest.first.uuid);
                            return null;
                        }

                        @Override
                        public Void visit(GitSignRequest gitSignRequest) throws Exception {
                            gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Void, Exception>() {
                                @Override
                                public Void visit(CommitInfo commit) throws Exception {
                                    Approval.approveGitCommitSignatures(db, pairingAndRequest.first.uuid);
                                    return null;
                                }

                                @Override
                                public Void visit(TagInfo tag) throws Exception {
                                    Approval.approveGitTagSignatures(db, pairingAndRequest.first.uuid);
                                    return null;
                                }
                            });
                            return null;
                        }

                        @Override
                        public Void visit(UnpairRequest unpairRequest) throws Exception {
                            return null;
                        }

                        @Override
                        public Void visit(HostsRequest hostsRequest) throws Exception {
                            return null;
                        }
                    });
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background approve", "time", (int) TEMPORARY_APPROVAL_SECONDS, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case APPROVE_THIS_TEMPORARILY:
                try {
                    pairingAndRequest.second.body.visit(new RequestBody.Visitor<Void, Exception>() {
                        @Override
                        public Void visit(MeRequest meRequest) throws Exception {
                            return null;
                        }

                        @Override
                        public Void visit(SignRequest signRequest) throws Exception {
                            String user = signRequest.user();
                            if (signRequest.hostNameVerified && signRequest.hostAuth.hostNames.length > 0) {
                                Approval.approveSSHUserHost(db, pairingAndRequest.first.uuid, user, signRequest.hostAuth.hostNames[0]);
                            }
                            return null;
                        }

                        @Override
                        public Void visit(GitSignRequest gitSignRequest) throws Exception {
                            return null;
                        }

                        @Override
                        public Void visit(UnpairRequest unpairRequest) throws Exception {
                            return null;
                        }

                        @Override
                        public Void visit(HostsRequest hostsRequest) throws Exception {
                            return null;
                        }
                    });
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background approve this", "time", (int) TEMPORARY_APPROVAL_SECONDS, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            case REJECT:
                try {
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, false);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background reject", null, null, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

}
