package co.krypt.krypton.team.log;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.git.CommitInfo;
import co.krypt.krypton.git.TagInfo;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.protocol.GitSignRequest;
import co.krypt.krypton.protocol.GitSignRequestBody;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.Request;
import co.krypt.krypton.protocol.SignRequest;

/**
 * Created by Kevin King on 2/12/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Log {
    public static class Session {
        @SerializedName("device_name")
        @JSON.JsonRequired
        public String deviceName;

        @SerializedName("workstation_public_key_double_hash")
        @JSON.JsonRequired
        public byte[] workstationPublicKeyDoubleHash;
    }

    @SerializedName("session")
    @JSON.JsonRequired
    public Session session;

    @SerializedName("unix_seconds")
    @JSON.JsonRequired
    public Long unixSeconds;

    public static class Body {
        public static class SSH {
            public static class HostAuthorization {
                @SerializedName("host")
                @JSON.JsonRequired
                public String host;

                @SerializedName("public_key")
                @JSON.JsonRequired
                byte[] publicKey;

                @SerializedName("signature")
                public byte[] signature;
            }

            public static class Result {
                @SerializedName("user_rejected")
                @Nullable
                public Object userRejected;

                public static Result userRejected() {
                    Result r = new Result();
                    r.userRejected = new Object();
                    return r;
                }

                /* Base64 encoded pinned hosts */
                @SerializedName("host_mismatch")
                @Nullable
                public String[] hostMismatch;

                public static Result hostMismatch(String[] pinnedPublicKeys) {
                    Result r = new Result();
                    r.hostMismatch = pinnedPublicKeys;
                    return r;
                }

                @SerializedName("signature")
                @Nullable
                public byte[] signature;

                public static Result signature(byte[] signature) {
                    Result r = new Result();
                    r.signature = signature;
                    return r;
                }

                @SerializedName("error")
                @Nullable
                public String error;

                public static Result error(String error) {
                    Result r = new Result();
                    r.error = error;
                    return r;
                }
            }

            @SerializedName("user")
            @JSON.JsonRequired
            public String user;

            @SerializedName("host_authorization")
            public HostAuthorization hostAuthorization;

            @SerializedName("session_data")
            @JSON.JsonRequired
            public byte[] sessionData;

            @SerializedName("result")
            @JSON.JsonRequired
            Result result;
        }

        @SerializedName("ssh")
        @Nullable
        public SSH ssh;

        public static class GitSignatureResult {
            @SerializedName("user_rejected")
            @Nullable
            public Object userRejected;

            public static GitSignatureResult userRejected() {
                GitSignatureResult gitSignatureResult = new GitSignatureResult();
                gitSignatureResult.userRejected = new Object();
                return gitSignatureResult;
            }

            @SerializedName("signature")
            @Nullable
            public byte[] signature;

            @SerializedName("error")
            @Nullable
            public String error;
        }
        public static class GitCommitSignature {
            @SerializedName("tree")
            @JSON.JsonRequired
            public String tree;

            @SerializedName("parents")
            @JSON.JsonRequired
            public String[] parents;

            @SerializedName("author")
            @JSON.JsonRequired
            public String author;

            @SerializedName("committer")
            @JSON.JsonRequired
            public String committer;

            @SerializedName("message")
            @JSON.JsonRequired
            public byte[] message;

            @SerializedName("message_string")
            @Nullable
            public String messageString;

            @SerializedName("result")
            @JSON.JsonRequired
            GitSignatureResult result;
        }

        @SerializedName("git_commit")
        @Nullable
        public GitCommitSignature gitCommit;

        public static class GitTagSignature {
            @SerializedName("object")
            @JSON.JsonRequired
            public String object;

            @SerializedName("type")
            @JSON.JsonRequired
            public String type;

            @SerializedName("tag")
            @JSON.JsonRequired
            public String tag;

            @SerializedName("tagger")
            @JSON.JsonRequired
            public String tagger;

            @SerializedName("message")
            @JSON.JsonRequired
            public byte[] message;

            @SerializedName("message_string")
            @Nullable
            public String messageString;

            @SerializedName("result")
            @JSON.JsonRequired
            GitSignatureResult result;
        }

        @SerializedName("git_tag")
        @Nullable
        GitTagSignature gitTag;
    }

    @SerializedName("body")
    @JSON.JsonRequired
    Body body;

    public static Log fromSSHRequest(Pairing pairing, Request req, SignRequest signRequest, Body.SSH.Result res) {
        Log log = new Log();

        log.session = new Session();
        log.session.deviceName = pairing.workstationName;
        try {
            log.session.workstationPublicKeyDoubleHash = SHA256.digest(SHA256.digest(pairing.workstationPublicKey));
        } catch (CryptoException e) {
            log.session.workstationPublicKeyDoubleHash = new byte[]{};
            e.printStackTrace();
        }

        log.unixSeconds = req.unixSeconds;

        Body body = new Body();
        body.ssh = new Body.SSH();
        if (signRequest.hostNameVerified && signRequest.hostAuth.hostNames.length > 0) {
            Body.SSH.HostAuthorization hostAuthorization = new Body.SSH.HostAuthorization();
            hostAuthorization.host = signRequest.hostAuth.hostNames[0];
            hostAuthorization.publicKey = signRequest.hostAuth.hostKey;
            hostAuthorization.signature = signRequest.hostAuth.signature;

            body.ssh.hostAuthorization = hostAuthorization;
        }
        body.ssh.sessionData = signRequest.data;
        body.ssh.user = signRequest.user();
        body.ssh.result = res;

        log.body = body;

        return log;
    }

    public static Log fromGitRequest(Pairing pairing, Request req, GitSignRequest gitSignRequest, Body.GitSignatureResult res) {
        Log log = new Log();

        log.session = new Session();
        log.session.deviceName = pairing.workstationName;
        try {
            log.session.workstationPublicKeyDoubleHash = SHA256.digest(SHA256.digest(pairing.workstationPublicKey));
        } catch (CryptoException e) {
            log.session.workstationPublicKeyDoubleHash = new byte[]{};
            e.printStackTrace();
        }

        log.unixSeconds = req.unixSeconds;

        Body body = new Body();
        gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Void, RuntimeException>() {
            @Override
            public Void visit(CommitInfo commit) throws RuntimeException {
                Body.GitCommitSignature gitCommitSignature = new Body.GitCommitSignature();
                gitCommitSignature.tree = commit.tree;
                gitCommitSignature.author = commit.author;
                gitCommitSignature.committer = commit.committer;
                gitCommitSignature.message = commit.message;
                gitCommitSignature.messageString = commit.validatedMessageStringOrError();
                if (commit.mergeParents != null) {
                    gitCommitSignature.parents = commit.mergeParents.toArray(new String[]{});
                } else {
                    gitCommitSignature.parents = new String[]{};
                }
                gitCommitSignature.result = res;

                body.gitCommit = gitCommitSignature;
                return null;
            }

            @Override
            public Void visit(TagInfo tag) throws RuntimeException {
                Body.GitTagSignature gitTagSignature = new Body.GitTagSignature();
                gitTagSignature.message = tag.message;
                gitTagSignature.messageString = tag.validatedMessageStringOrError();
                gitTagSignature.tag = tag.tag;
                gitTagSignature.tagger = tag.tagger;
                gitTagSignature.object = tag.object;
                gitTagSignature.type = tag.type;

                body.gitTag = gitTagSignature;
                return null;
            }
        });

        log.body = body;

        return log;
    }
}
