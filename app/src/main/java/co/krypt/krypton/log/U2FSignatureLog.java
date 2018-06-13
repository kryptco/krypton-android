package co.krypt.krypton.log;

import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import co.krypt.krypton.R;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.protocol.U2FAuthenticateRequest;
import co.krypt.krypton.protocol.U2FRegisterRequest;
import co.krypt.krypton.u2f.KnownAppIds;

/**
 * Created by Kevin King on 7/11/2018.
 * Copyright 2018. KryptCo, Inc.
 */
@DatabaseTable(tableName = "u2f_signature_log")
public class U2FSignatureLog implements Log {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "app_id")
    public String appId;

    @SerializedName("is_register")
    @DatabaseField(columnName = "is_register")
    public boolean isRegister;

    @SerializedName("pairing_uuid")
    @DatabaseField(columnName = "pairing_uuid", index = true)
    public String pairingUUID;

    public U2FSignatureLog(U2FRegisterRequest reg, Pairing pairing) {
        this.appId = reg.appId;
        this.isRegister = true;
        this.pairingUUID = pairing.getUUIDString();
        this.workstationName = pairing.workstationName;
        this.unixSeconds = System.currentTimeMillis() / 1000;
    }

    public U2FSignatureLog(U2FAuthenticateRequest reg, Pairing pairing) {
        this.appId = reg.appId;
        this.isRegister = false;
        this.pairingUUID = pairing.getUUIDString();
        this.workstationName = pairing.workstationName;
        this.unixSeconds = System.currentTimeMillis() / 1000;
    }


    @SerializedName("workstation_name")
    @DatabaseField(columnName = "workstation_name")
    public String workstationName;

    @SerializedName("unix_seconds")
    @DatabaseField(columnName = "unix_seconds")
    public long unixSeconds;

    //  default true
    @SerializedName("allowed")
    @DatabaseField(columnName = "allowed")
    public Boolean allowed;

    protected U2FSignatureLog() {}


    public static List<U2FSignatureLog> sortByTimeDescending(Set<U2FSignatureLog> logs) {
        List<U2FSignatureLog> sortedLogs = new ArrayList<>(logs);
        java.util.Collections.sort(sortedLogs, new Comparator<U2FSignatureLog>() {
            @Override
            public int compare(U2FSignatureLog lhs, U2FSignatureLog rhs) {
                return Long.compare(rhs.unixSeconds, lhs.unixSeconds);
            }
        });
        return sortedLogs;
    }

    @Override
    public long unixSeconds() {
        return unixSeconds;
    }

    @Override
    public String shortDisplay() {
        return (isRegister ? "registered with " : "signed in to ") + KnownAppIds.displayAppId(appId);
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
        View view = View.inflate(container.getContext(), R.layout.ssh_short, container);

        TextView messageText = view.findViewById(R.id.message);
        messageText.setText(shortDisplay());

        TextView label = view.findViewById(R.id.label);
        label.setText("U2F");
        if (approved != null && !approved) {
            label.setBackgroundResource(R.drawable.hash_red_bg);
        }

        TextView timeText = view.findViewById(R.id.time);
        timeText.setText(
                DateUtils.getRelativeTimeSpanString(unixSeconds() * 1000, System.currentTimeMillis(), 1000)
        );

        return view;
    }
}
