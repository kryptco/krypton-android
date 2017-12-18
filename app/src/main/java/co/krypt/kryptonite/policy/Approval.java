package co.krypt.kryptonite.policy;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import co.krypt.kryptonite.crypto.Base64;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.db.OpenDatabaseHelper;
import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 12/15/17.
 * Copyright 2017. KryptCo, Inc.
 */

@DatabaseTable(tableName = "approvals")
public class Approval {
    enum ApprovalType {
        SSH_USER_HOST,
        SSH_ANY_HOST,
        GIT_COMMIT_SIGNATURES,
        GIT_TAG_SIGNATURES,
    }

    @DatabaseField(generatedId = true)
    private Long id;

    private Approval() {}

    private Approval(@Nonnull ApprovalType type, @Nonnull UUID pairingUUID, @Nonnull Date approvedAt) {
        this.pairingUUID = pairingUUID;
        this.approvedAt = approvedAt;
        this.type = type;
    }

    public static synchronized void approveSSHUserHost(OpenDatabaseHelper db, UUID pairingUUID, String user, String host) throws CryptoException, IOException, SQLException {
        Approval approval = new Approval(ApprovalType.SSH_USER_HOST, pairingUUID, new Date());
        approval.sshUserHashHostHash = hashSSHUserHost(user, host);

        DeleteBuilder deleteExisting = db.getApprovalDao().deleteBuilder();
        deleteExisting.where().eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.SSH_USER_HOST)
                .and()
                .eq("ssh_user_hash_host_hash", approval.sshUserHashHostHash);
        deleteExisting.delete();

        db.getApprovalDao().create(approval);
    }

    public static synchronized void approveSSHAnyHost(OpenDatabaseHelper db, UUID pairingUUID) throws CryptoException, IOException, SQLException {
        Approval approval = new Approval(ApprovalType.SSH_ANY_HOST, pairingUUID, new Date());

        DeleteBuilder deleteExisting = db.getApprovalDao().deleteBuilder();
        deleteExisting.where().eq("pairing_uuid", pairingUUID)
                .eq("type", ApprovalType.SSH_ANY_HOST);
        deleteExisting.delete();

        db.getApprovalDao().create(approval);
    }

    public static synchronized void approveGitCommitSignatures(OpenDatabaseHelper db, UUID pairingUUID) throws CryptoException, IOException, SQLException {
        Approval approval = new Approval(ApprovalType.GIT_COMMIT_SIGNATURES, pairingUUID, new Date());

        DeleteBuilder deleteExisting = db.getApprovalDao().deleteBuilder();
        deleteExisting.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.GIT_COMMIT_SIGNATURES);
        deleteExisting.delete();

        db.getApprovalDao().create(approval);
    }

    public static synchronized void approveGitTagSignatures(OpenDatabaseHelper db, UUID pairingUUID) throws CryptoException, IOException, SQLException {
        Approval approval = new Approval(ApprovalType.GIT_TAG_SIGNATURES, pairingUUID, new Date());
        DeleteBuilder deleteExisting = db.getApprovalDao().deleteBuilder();
        deleteExisting.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.GIT_TAG_SIGNATURES);
        deleteExisting.delete();

        db.getApprovalDao().create(approval);
    }

    @DatabaseField(columnName = "pairing_uuid", canBeNull = false)
    @Nonnull
    private UUID pairingUUID;

    @Nonnull
    @DatabaseField(columnName = "approved_at", canBeNull = false)
    private Date approvedAt;

    @Nonnull
    @DatabaseField(columnName = "type", canBeNull = false)
    private ApprovalType type;

    /*
    b64(SHA256(SHA256(user)|| SHA256(host))) to prevent collisions between user@hostname strings
     */
    @DatabaseField(columnName = "ssh_user_hash_host_hash")
    @Nullable
    private String sshUserHashHostHash;


    public static synchronized void deleteExpiredApprovals(OpenDatabaseHelper db, Long temporaryApprovalSeconds) throws SQLException {
        DeleteBuilder deleteBuilder = db.getApprovalDao().deleteBuilder();
        deleteBuilder.where().lt("approved_at", new Date(System.currentTimeMillis() - temporaryApprovalSeconds * 1000));
        deleteBuilder.delete();
    }

    public static synchronized void deleteApprovalsForPairing(OpenDatabaseHelper db, UUID pairingUUID) throws SQLException {
        DeleteBuilder deleteBuilder = db.getApprovalDao().deleteBuilder();
        deleteBuilder.where().eq("pairing_uuid", pairingUUID);
        deleteBuilder.delete();
    }

    public static synchronized boolean isSSHUserHostApprovedNow(OpenDatabaseHelper db, UUID pairingUUID, Long temporaryApprovalSeconds, String user, String host) throws SQLException, CryptoException, IOException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        Approval query = new Approval();
        query.pairingUUID = pairingUUID;
        query.sshUserHashHostHash = hashSSHUserHost(user, host);
        query.type = ApprovalType.SSH_USER_HOST;
        return db.getApprovalDao().queryForMatchingArgs(query).size() > 0;
    }

    public static synchronized boolean isSSHAnyHostApprovedNow(OpenDatabaseHelper db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.getApprovalDao().queryBuilder();
        return query.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.SSH_ANY_HOST).countOf() > 0;
    }

    public static synchronized boolean isGitCommitApprovedNow(OpenDatabaseHelper db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.getApprovalDao().queryBuilder();
        return query.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.GIT_COMMIT_SIGNATURES).countOf() > 0;
    }

    public static synchronized boolean isGitTagApprovedNow(OpenDatabaseHelper db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.getApprovalDao().queryBuilder();
        return query.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.GIT_TAG_SIGNATURES).countOf() > 0;
    }

    private static String hashSSHUserHost(String user, String host) throws CryptoException, IOException {
        ByteArrayOutputStream userHashConcatHostHashStream = new ByteArrayOutputStream();
        userHashConcatHostHashStream.write(SHA256.digest(user.getBytes()));
        userHashConcatHostHashStream.write(SHA256.digest(host.getBytes()));
        return Base64.encode(SHA256.digest(userHashConcatHostHashStream.toByteArray()));
    }
}
