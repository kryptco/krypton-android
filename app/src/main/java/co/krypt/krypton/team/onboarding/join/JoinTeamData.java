package co.krypt.krypton.team.onboarding.join;

import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.team.Sigchain;

/**
 * Created by Kevin King on 1/15/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class JoinTeamData {
    /// IndirectInvite fields

    @SerializedName("invite_link")
    public String inviteLink;

    @SerializedName("decrypt_invite_output")
    Sigchain.DecryptInviteOutput decryptInviteOutput;

    @SerializedName("profile")
    public Profile profile;

    /// Shared Fields

    @SerializedName("email_challenge_nonce")
    public String emailChallengeNonce;

    /// DirectInvite fields

    @SerializedName("team_name")
    public String teamName;

    @SerializedName("identity")
    public Sigchain.Identity identity;

}
