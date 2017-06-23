package co.krypt.kryptonite.log;

import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import co.krypt.kryptonite.R;

/**
 * Created by Kevin King on 12/15/16.
 * Copyright 2016. KryptCo, Inc.
 */
@DatabaseTable(tableName = "signature_log")
public class SSHSignatureLog implements Log {
    @DatabaseField(generatedId = true)
    private Long id;

    @SerializedName("data")
    public byte[] data;

    //  default true
    @SerializedName("allowed")
    @DatabaseField(columnName = "allowed")
    public Boolean allowed;

    @SerializedName("command")
    @Nullable
    public String command;

    @SerializedName("user")
    @Nullable
    @DatabaseField(columnName = "user")
    public String user;

    @SerializedName("host_name")
    @Nullable
    @DatabaseField(columnName = "host_name")
    public String hostName;

    @SerializedName("unix_seconds")
    @DatabaseField(columnName = "unix_seconds")
    public long unixSeconds;

    @SerializedName("host_name_verified")
    @DatabaseField(columnName = "host_name_verified")
    public boolean hostNameVerified;

    @SerializedName("host_auth")
    @DatabaseField(columnName = "host_auth")
    public String hostAuthJSON;

    @SerializedName("pairing_uuid")
    @DatabaseField(columnName = "pairing_uuid", index = true)
    public String pairingUUID;

    @SerializedName("workstation_name")
    @DatabaseField(columnName = "workstation_name")
    public String workstationName;

    protected SSHSignatureLog() {}

    public SSHSignatureLog(byte[] data, Boolean allowed, String command, String user, String hostName, long unixSeconds, boolean hostNameVerified, String hostAuthJSON, String pairingUUID, String workstationName) {
        this.data = data;
        this.allowed = allowed;
        this.command = command;
        this.user = user;
        this.hostName = hostName;
        this.unixSeconds = unixSeconds;
        this.hostNameVerified = hostNameVerified;
        this.hostAuthJSON = hostAuthJSON;
        this.pairingUUID = pairingUUID;
        this.workstationName = workstationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SSHSignatureLog that = (SSHSignatureLog) o;

        if (unixSeconds != that.unixSeconds) return false;
        if (hostNameVerified != that.hostNameVerified) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (!Arrays.equals(data, that.data)) return false;
        if (allowed != null ? !allowed.equals(that.allowed) : that.allowed != null) return false;
        if (command != null ? !command.equals(that.command) : that.command != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null)
            return false;
        if (hostAuthJSON != null ? !hostAuthJSON.equals(that.hostAuthJSON) : that.hostAuthJSON != null)
            return false;
        if (pairingUUID != null ? !pairingUUID.equals(that.pairingUUID) : that.pairingUUID != null)
            return false;
        return workstationName != null ? workstationName.equals(that.workstationName) : that.workstationName == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + (allowed != null ? allowed.hashCode() : 0);
        result = 31 * result + (command != null ? command.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + (int) (unixSeconds ^ (unixSeconds >>> 32));
        result = 31 * result + (hostNameVerified ? 1 : 0);
        result = 31 * result + (hostAuthJSON != null ? hostAuthJSON.hashCode() : 0);
        result = 31 * result + (pairingUUID != null ? pairingUUID.hashCode() : 0);
        result = 31 * result + (workstationName != null ? workstationName.hashCode() : 0);
        return result;
    }

    public boolean isAllowed() {
        return allowed == null || allowed;
    }

    public String userHostTextWithReject() {
        return (isAllowed() ? "" : "rejected: ") + userHostText();
    }

    public String userHostText() {
        return (user != null ? user + "@" : "") +
                (hostNameVerified ? hostName : "unknown host");
    }

    public static List<SSHSignatureLog> sortByTimeDescending(Set<SSHSignatureLog> logs) {
        List<SSHSignatureLog> sortedLogs = new ArrayList<>(logs);
        java.util.Collections.sort(sortedLogs, new Comparator<SSHSignatureLog>() {
            @Override
            public int compare(SSHSignatureLog lhs, SSHSignatureLog rhs) {
                return Long.compare(rhs.unixSeconds, lhs.unixSeconds);
            }
        });
        return sortedLogs;
    }

    public SSHSignatureLog withDataRedacted() {
        return new SSHSignatureLog(null, this.allowed, this.command, this.user, this.hostName, this.unixSeconds, this.hostNameVerified, this.hostAuthJSON, this.pairingUUID, this.workstationName);
    }

    @Override
    public long unixSeconds() {
        return unixSeconds;
    }

    @Override
    public String shortDisplay() {
        return userHostTextWithReject();
    }

    @Override
    public String longDisplay() {
        return shortDisplay();
    }

    @javax.annotation.Nullable
    @Override
    public View fillShortView(ConstraintLayout container) {
        container.removeAllViews();
        return fillView(container, allowed, null);
    }

    @javax.annotation.Nullable
    @Override
    public View fillLongView(ConstraintLayout container) {
        container.removeAllViews();
        View v = fillShortView(container);
        ((TextView) v.findViewById(R.id.message)).setMaxLines(Integer.MAX_VALUE);
        return v;
    }

    @javax.annotation.Nullable
    @Override
    public String getSignature() {
        return null;
    }

    public View fillView(ConstraintLayout container, @Nullable Boolean approved, @Nullable String signature) {
        View sshView = View.inflate(container.getContext(), R.layout.ssh_short, container);

        TextView messageText = (TextView) sshView.findViewById(R.id.message);
        messageText.setText(userHostText());

        if (approved != null && !approved) {
            TextView sshText = (TextView) sshView.findViewById(R.id.ssh);
            sshText.setBackgroundResource(R.drawable.hash_red_bg);
        }

        TextView timeText = (TextView) sshView.findViewById(R.id.time);
        timeText.setText(
                DateUtils.getRelativeTimeSpanString(unixSeconds() * 1000, System.currentTimeMillis(), 1000)
        );

        return sshView;
    }
}
