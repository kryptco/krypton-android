package co.krypt.krypton.policy;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.db.OpenDatabaseHelper;
import co.krypt.krypton.exception.Unrecoverable;
import co.krypt.krypton.git.CommitInfo;
import co.krypt.krypton.git.TagInfo;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.protocol.GitSignRequest;
import co.krypt.krypton.protocol.GitSignRequestBody;
import co.krypt.krypton.protocol.HostsRequest;
import co.krypt.krypton.protocol.LogDecryptionRequest;
import co.krypt.krypton.protocol.MeRequest;
import co.krypt.krypton.protocol.ReadTeamRequest;
import co.krypt.krypton.protocol.Request;
import co.krypt.krypton.protocol.RequestBody;
import co.krypt.krypton.protocol.SignRequest;
import co.krypt.krypton.protocol.TeamOperationRequest;
import co.krypt.krypton.protocol.UnpairRequest;
import co.krypt.krypton.silo.Notifications;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;

/**
 * Created by Kevin King on 12/18/16.
 * Copyright 2016. KryptCo, Inc.
 *
 * LOCK ORDERING: Silo -> Policy -> Pairings
 */
public class Policy {
    private static final String TAG = "Policy";
    public static final String APPROVE_ONCE = "approve-once";
    //  approve a subset of a request type, such as a single SSH user@host
    public static final String APPROVE_THIS_TEMPORARILY = "approve-this-temporarily";
    //  approve all requests of a specific type, such as all SSH requests
    public static final String APPROVE_ALL_TEMPORARILY = "approve-all-temporarily";
    public static final String REJECT = "reject";

    private static final long DEFAULT_TEMPORARY_APPROVAL_SECONDS = 3600*3;

    public static final long READ_TEAM_TEMPORARY_APPROVAL_SECONDS = 3600*6;

    public static String temporaryApprovalDuration(Context context, Request r) {
            return co.krypt.krypton.uiutils.TimeUtils.formatDurationMillis(
                    temporaryApprovalSeconds(context, r) * 1000
            );
    }

    public static long temporaryApprovalSeconds(Context context, Request r) {
        if (r.body instanceof ReadTeamRequest || r.body instanceof LogDecryptionRequest) {
            return READ_TEAM_TEMPORARY_APPROVAL_SECONDS;
        }
        try {
            Sigchain.NativeResult<Sigchain.Policy> result = TeamDataProvider.getPolicy(context);
            if (result.success != null && result.success.temporaryApprovalSeconds != null) {
                return result.success.temporaryApprovalSeconds;
            }
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
        return DEFAULT_TEMPORARY_APPROVAL_SECONDS;
    }

    public static long temporaryApprovalSeconds(Context context, Approval approval) {
        return temporaryApprovalSeconds(context, approval.type);
    }

    public static long temporaryApprovalSeconds(Context context, Approval.ApprovalType type) {
        if (type.equals(Approval.ApprovalType.READ_TEAM_DATA)) {
            return READ_TEAM_TEMPORARY_APPROVAL_SECONDS;
        }
        try {
            Sigchain.NativeResult<Sigchain.Policy> result = TeamDataProvider.getPolicy(context);
            if (result.success != null && result.success.temporaryApprovalSeconds != null) {
                return result.success.temporaryApprovalSeconds;
            }
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
        return DEFAULT_TEMPORARY_APPROVAL_SECONDS;
    }

    public static boolean teamPolicySet(Context context) {
        try {
            Sigchain.NativeResult<Sigchain.Policy> result = TeamDataProvider.getPolicy(context);
            return result.success != null && result.success.temporaryApprovalSeconds != null;
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
            return false;
        }
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

    public static synchronized boolean isApprovedNow(Silo silo, Context context, Pairing pairing, Request request) {
        try {
            Dao<Approval, Long> db = silo.pairings().dbHelper.getApprovalDao();
            boolean neverAskAndTeamPolicyUnset = silo.pairings().getApproved(pairing) && !teamPolicySet(context);
            return request.body.visit(new RequestBody.Visitor<Boolean, Unrecoverable>() {
                @Override
                public Boolean visit(MeRequest meRequest) throws Unrecoverable {
                    return true;
                }

                @Override
                public Boolean visit(SignRequest signRequest) throws Unrecoverable {
                    try {
                        String hostname = signRequest.verifiedHostNameOrDefault(null);
                        if (hostname == null) {
                            if (!silo.pairings().requireUnknownHostManualApproval(pairing)) {
                                return Approval.isSSHAnyHostApprovedNow(db, pairing.uuid, temporaryApprovalSeconds(context, request)) ||
                                        neverAskAndTeamPolicyUnset;
                            }
                            return false;
                        }
                        if (!silo.hasKnownHost(hostname)) {
                            return false;
                        }
                        return Approval.isSSHUserHostApprovedNow(db, pairing.uuid, temporaryApprovalSeconds(context, request), signRequest.user(), hostname) ||
                                Approval.isSSHAnyHostApprovedNow(db, pairing.uuid, temporaryApprovalSeconds(context, request)) ||
                                neverAskAndTeamPolicyUnset;
                    } catch (SQLException | IOException e) {
                        throw new Unrecoverable(e);
                    }
                }

                @Override
                public Boolean visit(GitSignRequest gitSignRequest) throws Unrecoverable {
                    try {
                        if (neverAskAndTeamPolicyUnset) {
                            return true;
                        }
                        return gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Boolean, Exception>() {
                            @Override
                            public Boolean visit(CommitInfo commit) throws Exception {
                                return Approval.isGitCommitApprovedNow(db, pairing.uuid, temporaryApprovalSeconds(context, request));
                            }

                            @Override
                            public Boolean visit(TagInfo tag) throws Exception {
                                return Approval.isGitTagApprovedNow(db, pairing.uuid, temporaryApprovalSeconds(context, request));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }

                @Override
                public Boolean visit(UnpairRequest unpairRequest) throws Unrecoverable {
                    return true;
                }

                @Override
                public Boolean visit(HostsRequest hostsRequest) throws Unrecoverable {
                    return false;
                }

                @Override
                public Boolean visit(ReadTeamRequest readTeamRequest) throws Unrecoverable {
                    try {
                        return Approval.isReadTeamDataApproved(db, pairing.uuid, temporaryApprovalSeconds(context, request));
                    } catch (SQLException e) {
                        throw new Unrecoverable(e);
                    }
                }

                @Override
                public Boolean visit(LogDecryptionRequest logDecryptionRequest) throws Unrecoverable {
                    try {
                        return Approval.isReadTeamDataApproved(db, pairing.uuid, temporaryApprovalSeconds(context, request));
                    } catch (SQLException e) {
                        throw new Unrecoverable(e);
                    }
                }

                @Override
                public Boolean visit(TeamOperationRequest teamOperationRequest) throws Unrecoverable {
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

    public static void onAction(final Context context, final String requestID, final String action) {
        Log.i(TAG, action + " requestID " + requestID);
        final Pair<Pairing, Request> pairingAndRequest;

        // Lock manually to prevent deadlock from Silo
        synchronized (Policy.class) {
            pairingAndRequest = pendingRequestCache.remove(requestID);
        }
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
                    pairingAndRequest.second.body.visit(new RequestBody.Visitor<Void, Unrecoverable>() {
                        @Override
                        public Void visit(MeRequest meRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(SignRequest signRequest) throws Unrecoverable {
                            try {
                                Approval.approveSSHAnyHost(db, pairingAndRequest.first.uuid);
                            } catch (IOException | SQLException e) {
                                throw new Unrecoverable(e);
                            }
                            return null;
                        }

                        @Override
                        public Void visit(GitSignRequest gitSignRequest) throws Unrecoverable {
                            gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Void, Unrecoverable>() {
                                @Override
                                public Void visit(CommitInfo commit) throws Unrecoverable {
                                    try {
                                        Approval.approveGitCommitSignatures(db, pairingAndRequest.first.uuid);
                                    } catch (IOException | SQLException e) {
                                        throw new Unrecoverable(e);
                                    }
                                    return null;
                                }

                                @Override
                                public Void visit(TagInfo tag) throws Unrecoverable {
                                    try {
                                        Approval.approveGitTagSignatures(db, pairingAndRequest.first.uuid);
                                    } catch (IOException | SQLException e) {
                                        throw new Unrecoverable(e);
                                    }
                                    return null;
                                }
                            });
                            return null;
                        }

                        @Override
                        public Void visit(UnpairRequest unpairRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(HostsRequest hostsRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(ReadTeamRequest readTeamRequest) throws Unrecoverable {
                            try {
                                Approval.approveReadTeamData(db, pairingAndRequest.first.uuid);
                            } catch (IOException | SQLException e) {
                                throw new Unrecoverable(e);
                            }
                            return null;
                        }

                        @Override
                        public Void visit(LogDecryptionRequest logDecryptionRequest) throws Unrecoverable {
                            try {
                                Approval.approveReadTeamData(db, pairingAndRequest.first.uuid);
                            } catch (IOException | SQLException e) {
                                throw new Unrecoverable(e);
                            }
                            return null;
                        }

                        @Override
                        public Void visit(TeamOperationRequest teamOperationRequest) throws Unrecoverable {
                            return null;
                        }
                    });
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background approve", "time", (int) temporaryApprovalSeconds(context, pairingAndRequest.second), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case APPROVE_THIS_TEMPORARILY:
                try {
                    pairingAndRequest.second.body.visit(new RequestBody.Visitor<Void, Unrecoverable>() {
                        @Override
                        public Void visit(MeRequest meRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(SignRequest signRequest) throws Unrecoverable {
                            String user = signRequest.user();
                            if (signRequest.hostNameVerified && signRequest.hostAuth.hostNames.length > 0) {
                                try {
                                    Approval.approveSSHUserHost(db, pairingAndRequest.first.uuid, user, signRequest.hostAuth.hostNames[0]);
                                } catch (IOException | SQLException e) {
                                    throw new Unrecoverable(e);
                                }
                            }
                            return null;
                        }

                        @Override
                        public Void visit(GitSignRequest gitSignRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(UnpairRequest unpairRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(HostsRequest hostsRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(ReadTeamRequest readTeamRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(LogDecryptionRequest logDecryptionRequest) throws Unrecoverable {
                            return null;
                        }

                        @Override
                        public Void visit(TeamOperationRequest teamOperationRequest) throws Unrecoverable {
                            return null;
                        }
                    });
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, true);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background approve this", "time", (int) temporaryApprovalSeconds(context, pairingAndRequest.second), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            case REJECT:
                try {
                    silo.respondToRequest(pairingAndRequest.first, pairingAndRequest.second, false);
                    new Analytics(context).postEvent(pairingAndRequest.second.analyticsCategory(), "background reject", null, null, false);
                } catch (Unrecoverable e) {
                    e.printStackTrace();
                }
                break;
        }
    }

}
