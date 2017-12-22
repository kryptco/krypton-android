package co.krypt.kryptonite.policy;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import co.krypt.kryptonite.crypto.Base64;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.db.OpenDatabaseHelper;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.silo.Silo;
import co.krypt.kryptonite.uiutils.TimeUtils;

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

    @DatabaseField(columnName = "pairing_uuid", canBeNull = false)
    @Nonnull
    private UUID pairingUUID;

    @Nonnull
    @DatabaseField(columnName = "approved_at", canBeNull = false)
    private Date approvedAt;

    @Nonnull
    @DatabaseField(columnName = "type", canBeNull = false)
    public ApprovalType type;

    /*
    b64(SHA256(SHA256(user)|| SHA256(host))) to prevent collisions between user@hostname strings
     */
    @DatabaseField(columnName = "ssh_user_hash_host_hash")
    @Nullable
    private String sshUserHashHostHash;
    @Nullable
    @DatabaseField
    private String user;
    @Nullable
    @DatabaseField
    private String host;

    private Approval(@Nonnull ApprovalType type, @Nonnull UUID pairingUUID, @Nonnull Date approvedAt) {
        this.pairingUUID = pairingUUID;
        this.approvedAt = approvedAt;
        this.type = type;
    }

    public static synchronized void approveSSHUserHost(OpenDatabaseHelper db, UUID pairingUUID, String user, String host) throws CryptoException, IOException, SQLException {
        Approval approval = new Approval(ApprovalType.SSH_USER_HOST, pairingUUID, new Date());
        approval.sshUserHashHostHash = hashSSHUserHost(user, host);
        approval.user = user;
        approval.host = host;

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

    public static synchronized void deleteExpiredApprovals(Dao<Approval, Long> db, Long temporaryApprovalSeconds) throws SQLException {
        DeleteBuilder deleteBuilder = db.deleteBuilder();
        deleteBuilder.where().lt("approved_at", new Date(System.currentTimeMillis() - temporaryApprovalSeconds * 1000));
        deleteBuilder.delete();
    }

    public static synchronized void deleteApprovalsForPairing(Dao<Approval, Long> db, UUID pairingUUID) throws SQLException {
        DeleteBuilder deleteBuilder = db.deleteBuilder();
        deleteBuilder.where().eq("pairing_uuid", pairingUUID);
        deleteBuilder.delete();
    }

    public static synchronized boolean isSSHUserHostApprovedNow(Dao<Approval, Long> db, UUID pairingUUID, Long temporaryApprovalSeconds, String user, String host) throws SQLException, CryptoException, IOException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        Approval query = new Approval();
        query.pairingUUID = pairingUUID;
        query.sshUserHashHostHash = hashSSHUserHost(user, host);
        query.type = ApprovalType.SSH_USER_HOST;
        return db.queryForMatchingArgs(query).size() > 0;
    }

    public static synchronized boolean isSSHAnyHostApprovedNow(Dao<Approval, Long> db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.queryBuilder();
        return query.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.SSH_ANY_HOST).countOf() > 0;
    }

    public static synchronized boolean isGitCommitApprovedNow(Dao<Approval, Long> db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.queryBuilder();
        return query.where()
                .eq("pairing_uuid", pairingUUID)
                .and()
                .eq("type", ApprovalType.GIT_COMMIT_SIGNATURES).countOf() > 0;
    }

    public static synchronized boolean isGitTagApprovedNow(Dao<Approval, Long> db, UUID pairingUUID, Long temporaryApprovalSeconds) throws SQLException {
        deleteExpiredApprovals(db, temporaryApprovalSeconds);
        QueryBuilder query = db.queryBuilder();
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

    public void delete(Context context) throws SQLException {
        Silo.shared(context).pairings().dbHelper.getApprovalDao().delete(this);
    }

    public String display() {
        switch (type) {
            case SSH_USER_HOST:
                return user + "@" + host;
            case SSH_ANY_HOST:
                return "SSH Logins To Any Host";
            case GIT_COMMIT_SIGNATURES:
                return "Git Commit Signatures";
            case GIT_TAG_SIGNATURES:
                return "Git Tag Signatures";
        }
        return "";
    }

    public String timeRemaining(Long temporaryApprovalSeconds) {
        return TimeUtils.formatDurationSeconds(approvedAt.getTime() / 1000 +  temporaryApprovalSeconds - System.currentTimeMillis() / 1000);
    }

    private static Where<Approval, Long> notExpired(Where<Approval, Long> w, Long temporaryApprovalSeconds) throws SQLException {
        return w.ge("approved_at", new Date(System.currentTimeMillis() - temporaryApprovalSeconds * 1000));
    }

    public static List<Approval> getSSHHostApprovals(Dao<Approval, Long> dao, Long temporaryApprovalSeconds, UUID pairingUUID) throws SQLException {
        QueryBuilder<Approval, Long> q = dao.queryBuilder();
        return notExpired(q.where(), temporaryApprovalSeconds)
                .and()
                .eq("pairing_uuid", pairingUUID)
                .and().eq("type", ApprovalType.SSH_USER_HOST)
                .query();
    }

    public static List<Approval> getRequestTypeApprovals(Dao<Approval, Long> dao, Long temporaryApprovalSeconds, UUID pairingUUID) throws SQLException {
        QueryBuilder<Approval, Long> q = dao.queryBuilder();
        return notExpired(q.where(), temporaryApprovalSeconds)
                .and()
                .eq("pairing_uuid", pairingUUID)
                .and().in("type", ApprovalType.SSH_ANY_HOST, ApprovalType.GIT_COMMIT_SIGNATURES, ApprovalType.GIT_TAG_SIGNATURES)
                .query();
    }

    public static boolean hasTemporaryApprovals(Dao<Approval, Long> dao, Long temporaryApprovalSeconds, UUID pairingUUID) throws SQLException {
        return notExpired(dao.queryBuilder().where(), temporaryApprovalSeconds)
                .and()
                .eq("pairing_uuid", pairingUUID)
                .countOf() > 0;
    }
}
