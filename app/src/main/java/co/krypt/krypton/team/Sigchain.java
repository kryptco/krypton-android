package co.krypt.krypton.team;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Arrays;

import javax.annotation.Nullable;

import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.Profile;

/**
 * Created by Kevin King on 1/17/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Sigchain {
    public static class Identity implements Serializable {
        @SerializedName("email")
        @JSON.JsonRequired
        public String email;

        @SerializedName("public_key")
        @JSON.JsonRequired
        public byte[] publicKey;

        @SerializedName("ssh_public_key")
        @JSON.JsonRequired
        public byte[] sshPublicKey;

        @SerializedName("pgp_public_key")
        @JSON.JsonRequired
        public byte[] pgpPublicKey;

        @SerializedName("encryption_public_key")
        @JSON.JsonRequired
        public byte[] encryptionPublicKey;
    }
    public static class PinnedHost implements Serializable {
        public PinnedHost(String host, byte[] publicKey) {
            this.host = host;
            this.publicKey = publicKey;
        }

        @SerializedName("host")
        @JSON.JsonRequired
        public String host;

        @SerializedName("public_key")
        @JSON.JsonRequired
        public byte[] publicKey;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PinnedHost that = (PinnedHost) o;

            if (!host.equals(that.host)) return false;
            return Arrays.equals(publicKey, that.publicKey);
        }

        @Override
        public int hashCode() {
            int result = host.hashCode();
            result = 31 * result + Arrays.hashCode(publicKey);
            return result;
        }
    }
    public static class Policy implements Serializable {
        @SerializedName("temporary_approval_seconds")
        @Nullable
        public Long temporaryApprovalSeconds;

        public Policy(Long temporaryApprovalSeconds) {
            this.temporaryApprovalSeconds = temporaryApprovalSeconds;
        }
    }
    public static class IndirectInvitationSecret implements Serializable {
        @SerializedName("initial_team_public_key")
        @JSON.JsonRequired
        public byte[] initialTeamPublicKey;

        @SerializedName("last_block_hash")
        @JSON.JsonRequired
        public byte[] lastBlockHash;

        @SerializedName("nonce_keypair_seed")
        @JSON.JsonRequired
        byte[] nonceKeypairSeed;

        @SerializedName("restriction")
        @JSON.JsonRequired
        public IndirectInvitationRestriction restriction;
    }
    public static class IndirectInvitationRestriction implements Serializable {
        @SerializedName("domain")
        @Nullable
        public String domain;

        @SerializedName("emails")
        @Nullable
        public String[] emails;

        public IndirectInvitationRestriction(String[] emails) {
            this.emails = emails;
        }

        public IndirectInvitationRestriction(String domain) {
            this.domain = domain;
        }

        public boolean isSatisfiedByEmail(String email) {
            if (domain != null) {
                if (email.endsWith("@" + domain)) {
                    return true;
                }
            }
            if (emails != null) {
                for (String restrictedEmail: emails) {
                    if (restrictedEmail.equals(email)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    public static class Member implements Serializable {
        @SerializedName("identity")
        @JSON.JsonRequired
        public Identity identity;

        @SerializedName("is_admin")
        @JSON.JsonRequired
        public boolean is_admin;

        @SerializedName("is_removed")
        @JSON.JsonRequired
        public boolean is_removed;
    }
    public static class TeamHomeData implements Serializable {
        @SerializedName("team_public_key")
        @JSON.JsonRequired
        public byte[] teamPublicKey;

        @SerializedName("identity_public_key")
        @JSON.JsonRequired
        public byte[] identityPublicKey;

        @SerializedName("last_block_hash")
        @JSON.JsonRequired
        public byte[] lastBlockHash;

        @SerializedName("members")
        @JSON.JsonRequired
        public Member[] members;

        @SerializedName("name")
        @JSON.JsonRequired
        public String name;

        @SerializedName("is_admin")
        @JSON.JsonRequired
        public boolean is_admin;

        @SerializedName("email")
        @JSON.JsonRequired
        public String email;

        @SerializedName("temporary_approval_seconds")
        @Nullable
        public Long temporaryApprovalSeconds;

        @SerializedName("pinned_hosts")
        @JSON.JsonRequired
        public PinnedHost[] pinnedHosts;

        @SerializedName("n_open_invites")
        @JSON.JsonRequired
        public Long nOpenInvites;

        @SerializedName("n_blocks")
        @JSON.JsonRequired
        public Long nBlocks;

        @SerializedName("audit_logs_enabled")
        @JSON.JsonRequired
        public boolean auditLogsEnabled;

        @SerializedName("billing_url")
        @Nullable
        public String billingUrl;
    }
    public static class DirectInvitation {
        @SerializedName("public_key")
        @JSON.JsonRequired
        public byte[] publicKey;

        @SerializedName("email")
        @JSON.JsonRequired
        public String email;

        public DirectInvitation(byte[] publicKey, String email) {
            this.publicKey = publicKey;
            this.email = email;
        }
    }
    public static class TeamInfo {
        public TeamInfo(String name) {
            this.name = name;
        }

        @SerializedName("name")
        @JSON.JsonRequired
        public String name;
    }
    public static class RequestableTeamOperation implements Serializable {
        @SerializedName("set_policy")
        @Nullable
        public Policy setPolicy;

        public RequestableTeamOperation(Policy setPolicy) {
            this.setPolicy = setPolicy;
        }

        @SerializedName("direct_invite")
        @Nullable
        public DirectInvitation directInvite;

        public RequestableTeamOperation(DirectInvitation directInvite) {
            this.directInvite = directInvite;
        }

        @SerializedName("indirect_invite")
        @Nullable
        public IndirectInvitationRestriction indirectInvite;

        public RequestableTeamOperation(IndirectInvitationRestriction indirectInvite) {
            this.indirectInvite = indirectInvite;
        }

        @SerializedName("close_invitations")
        @Nullable
        public Object closeInvitations;

        public static RequestableTeamOperation cancelInvite() {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.closeInvitations = new Object();
            return op;
        }

        @SerializedName("promote")
        @Nullable
        public byte[] promote;

        public static RequestableTeamOperation promote(byte[] promote) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.promote = promote;
            return op;
        }

        @SerializedName("demote")
        @Nullable
        public byte[] demote;

        public static RequestableTeamOperation demote(byte[] demote) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.demote = demote;
            return op;
        }

        @SerializedName("remove")
        @Nullable
        public byte[] remove;

        public static RequestableTeamOperation remove(byte[] remove) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.remove = remove;
            return op;
        }

        @SerializedName("leave")
        @Nullable
        public Object leave;

        public static RequestableTeamOperation leave() {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.leave = new Object();
            return op;
        }

        @SerializedName("unpin_host_key")
        @Nullable
        PinnedHost unpinHostKey;

        public static RequestableTeamOperation unpinHostKey(PinnedHost hostKey) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.unpinHostKey = hostKey;
            return op;
        }

        @SerializedName("pin_host_key")
        @Nullable
        PinnedHost pinHostKey;

        public static RequestableTeamOperation pinHostKey(PinnedHost hostKey) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.pinHostKey = hostKey;
            return op;
        }

        @SerializedName("set_team_info")
        @Nullable
        TeamInfo setTeamInfo;

        public static RequestableTeamOperation setTeamInfo(TeamInfo teamInfo) {
            RequestableTeamOperation op = new RequestableTeamOperation();
            op.setTeamInfo = teamInfo;
            return op;
        }

        public String loadingText() {
            if (setPolicy != null) {
                return "Setting team policy...";
            }
            if (indirectInvite != null || directInvite != null) {
                return "Creating invite...";
            }
            if (closeInvitations != null) {
                return "Closing all invitations...";
            }
            if (promote != null) {
                return "Promoting member...";
            }
            if (demote != null) {
                return "Demoting admin...";
            }
            if (remove != null) {
                return "Removing member...";
            }
            if (leave != null) {
                return "Leaving team...";
            }
            if (setTeamInfo != null) {
                return "Updating team name...";
            }
            if (pinHostKey != null) {
                return "Pinning host key...";
            }
            if (unpinHostKey != null) {
                return "Removing host key...";
            }
            return "Updating team...";
        }

        public String successText() {
            if (setPolicy != null) {
                return "Team policy updated";
            }
            if (indirectInvite != null || directInvite != null) {
                return "Invite created";
            }
            if (closeInvitations != null) {
                return "Invitations closed";
            }
            if (promote != null) {
                return "Member promoted";
            }
            if (demote != null) {
                return "Admin demoted";
            }
            if (remove != null) {
                return "Member removed";
            }
            if (setTeamInfo != null) {
                return "Updated team name";
            }
            if (leave != null) {
                return "Left team";
            }
            if (pinHostKey != null) {
                return "Pinned host key";
            }
            if (unpinHostKey != null) {
                return "Removed host key";
            }
            return "Team updated";
        }

        private RequestableTeamOperation() {}
    }
    public static class TeamOperationResponse {
        @SerializedName("posted_block_hash")
        @JSON.JsonRequired
        public byte[] postedBlockHash;

        @SerializedName("data")
        @Nullable
        public TeamOperationResponseData data;
    }
    public static class TeamOperationResponseData {
        @SerializedName("invite_link")
        @Nullable
        public String inviteLink;
    }
    public static class DecryptInviteOutput implements Serializable {
        @SerializedName("indirect_invitation_secret")
        @JSON.JsonRequired
        public IndirectInvitationSecret indirectInvitationSecret;

        @SerializedName("team_name")
        @JSON.JsonRequired
        public String teamName;
    }
    public static class AcceptInviteArgs implements Serializable {
        @SerializedName("invite_secret")
        @JSON.JsonRequired
        public IndirectInvitationSecret inviteSecret;

        @SerializedName("email_challenge_nonce")
        @JSON.JsonRequired
        public byte[] emailChallengeNonce;

        @SerializedName("profile")
        @JSON.JsonRequired
        public Profile profile;

        public AcceptInviteArgs(IndirectInvitationSecret inviteSecret, byte[] emailChallengeNonce, Profile profile) {
            this.inviteSecret = inviteSecret;
            this.emailChallengeNonce = emailChallengeNonce;
            this.profile = profile;
        }
    }
    public static class NativeResult<T> {
        @SerializedName("success")
        @Nullable
        public T success;

        public NativeResult(String error) {
            this.error = error;
            this.success = null;
        }

        @SerializedName("error")
        @Nullable
        public String error;
    }

    public static class FormattedBlock {
        @SerializedName("header")
        @JSON.JsonRequired
        public String header;

        @SerializedName("author")
        @JSON.JsonRequired
        public String author;

        @SerializedName("time")
        @JSON.JsonRequired
        public String time;

        @SerializedName("body")
        @Nullable
        public String body;

        @SerializedName("hash")
        @JSON.JsonRequired
        public byte[] hash;

        @SerializedName("first")
        @JSON.JsonRequired
        public boolean first;

        @SerializedName("last")
        @JSON.JsonRequired
        public boolean last;
    }

    public static class FormattedRequestableOp {
        @SerializedName("header")
        @JSON.JsonRequired
        public String header;

        @SerializedName("body")
        @JSON.JsonRequired
        public String body;
    }

    public static class ServerEndpoints {
        @SerializedName("api_host")
        @JSON.JsonRequired
        public String apiHost;

        @SerializedName("billing_host")
        @JSON.JsonRequired
        public String billingHost;

        public ServerEndpoints(ServerEndpoints serverEndpoints) {
            this.apiHost = serverEndpoints.apiHost;
            this.billingHost = serverEndpoints.billingHost;
        }
    }

    public static class TeamCheckpoint {
        @SerializedName("team_public_key")
        @JSON.JsonRequired
        public byte[] teamPublicKey;

        @SerializedName("last_block_hash")
        @JSON.JsonRequired
        public byte[] lastBlockHash;

        @SerializedName("public_key")
        @JSON.JsonRequired
        public byte[] publicKey;

        @SerializedName("server_endpoints")
        @JSON.JsonRequired
        public ServerEndpoints serverEndpoints;

        public TeamCheckpoint(TeamCheckpoint teamCheckpoint) {
            this.teamPublicKey = teamCheckpoint.teamPublicKey.clone();
            this.lastBlockHash = teamCheckpoint.lastBlockHash.clone();
            this.publicKey = teamCheckpoint.publicKey.clone();
            this.serverEndpoints = teamCheckpoint.serverEndpoints == null ? null : new ServerEndpoints(teamCheckpoint.serverEndpoints);
        }
    }

    public static class UpdateTeamOutput {
        @SerializedName("more")
        @JSON.JsonRequired
        public boolean more;

        @SerializedName("last_formatted_block")
        @Nullable
        public FormattedBlock lastFormattedBlock;
    }

    public static class QRPayload {
        @Nullable
        @SerializedName("aqp")
        public AdminQRPayload adminQRPayload;

        @Nullable
        @SerializedName("mqp")
        public MemberQRPayload memberQRPayload;

        public QRPayload(AdminQRPayload adminQRPayload) {
            this.adminQRPayload = adminQRPayload;
        }

        public QRPayload(MemberQRPayload memberQRPayload) {
            this.memberQRPayload = memberQRPayload;
        }
    }

    public static class AdminQRPayload {
        @SerializedName("tpk")
        @JSON.JsonRequired
        public byte[] teamPublicKey;

        @SerializedName("lbh")
        @JSON.JsonRequired
        public byte[] lastBlockHash;

        @SerializedName("n")
        @JSON.JsonRequired
        public String teamName;

        public AdminQRPayload(byte[] teamPublicKey, byte[] lastBlockHash, String teamName) {
            this.teamPublicKey = teamPublicKey;
            this.lastBlockHash = lastBlockHash;
            this.teamName = teamName;
        }

    }

    public static class MemberQRPayload {
        @SerializedName("e")
        @JSON.JsonRequired
        public String email;

        @SerializedName("pk")
        @JSON.JsonRequired
        public byte[] publicKey;

        public MemberQRPayload(String email, byte[] publicKey) {
            this.email = email;
            this.publicKey = publicKey;
        }
    }

    public static class GenerateClientInput {
        @SerializedName("profile")
        @JSON.JsonRequired
        public Profile profile;

        @SerializedName("team_public_key")
        @JSON.JsonRequired
        public byte[] teamPublicKey;

        @SerializedName("last_block_hash")
        @JSON.JsonRequired
        public byte[] last_block_hash;

        public GenerateClientInput(Profile profile, byte[] teamPublicKey, byte[] last_block_hash) {
            this.profile = profile;
            this.teamPublicKey = teamPublicKey;
            this.last_block_hash = last_block_hash;
        }
    }

    public static class AcceptDirectInviteArgs {
        @SerializedName("identity")
        @JSON.JsonRequired
        public Identity profile;

        @SerializedName("email_challenge_nonce")
        @JSON.JsonRequired
        public byte[] emailChallengeNonce;

        public AcceptDirectInviteArgs(Identity profile, byte[] emailChallengeNonce) {
            this.profile = profile;
            this.emailChallengeNonce = emailChallengeNonce;
        }
    }

    public static class UnwrapKeyOutput {
        @SerializedName("key")
        @JSON.JsonRequired
        public byte[] key;
    }
}
