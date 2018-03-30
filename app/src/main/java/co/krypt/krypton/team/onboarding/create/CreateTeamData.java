package co.krypt.krypton.team.onboarding.create;

import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.team.Sigchain;

/**
 * Created by Kevin King on 1/15/18.
 * Copyright 2016. KryptCo, Inc.
 */

public class CreateTeamData {
    @SerializedName("name")
    String name;

    @SerializedName("enable_audit_logs")
    boolean enableAuditLogs;

    @SerializedName("temporary_approval_seconds")
    long temporaryApprovalSeconds;

    @SerializedName("pinned_hosts")
    Sigchain.PinnedHost[] pinnedHosts;

    @SerializedName("creator_profile")
    Profile creatorProfile;

    @SerializedName("email_challenge_nonce")
    public String emailChallengeNonce;
}
