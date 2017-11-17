package co.krypt.kryptonite.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.RemoteViews;

import com.amazonaws.util.Base32;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.pairing.Pairing;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Request {
    @SerializedName("request_id")
    @JSON.JsonRequired
    public String requestID;

    public String requestIDCacheKey(Pairing pairing) throws CryptoException {
        return Base32.encodeAsString(SHA256.digest((pairing.getUUIDString().toLowerCase() + requestID).getBytes())).toLowerCase().replace("=", "-");
    }

    @SerializedName("v")
    @JSON.JsonRequired
    public String version;

    @SerializedName("unix_seconds")
    @JSON.JsonRequired
    public Long unixSeconds;

    @SerializedName("me_request")
    @Nullable
    public MeRequest meRequest;

    @SerializedName("sign_request")
    @Nullable
    public SignRequest signRequest;

    @SerializedName("git_sign_request")
    @Nullable
    public GitSignRequest gitSignRequest;

    @SerializedName("unpair_request")
    @Nullable
    public UnpairRequest unpairRequest;

    @SerializedName("hosts_request")
    @Nullable
    public HostsRequest hostsRequest;

    @SerializedName("a")
    @Nullable
    public Boolean sendACK;

    public Version semVer() {
        try {
            return Version.valueOf(version);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return Version.valueOf("0.0.0");
    }

    public void fillRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        if (gitSignRequest != null) {
            gitSignRequest.fillRemoteViews(remoteViewsContainer, approved, signature);
        }
        if (signRequest != null) {
            signRequest.fillRemoteViews(remoteViewsContainer, approved, signature);
        }
    }

    public void fillShortRemoteViews(RemoteViews remoteViewsContainer, @Nullable Boolean approved, @Nullable String signature) {
        remoteViewsContainer.removeAllViews(R.id.content);
        if (gitSignRequest != null) {
            gitSignRequest.fillShortRemoteViews(remoteViewsContainer, approved, signature);
        }
        if (signRequest != null) {
            signRequest.fillShortRemoteViews(remoteViewsContainer, approved, signature);
        }
    }

    public String analyticsCategory() {
        if (signRequest != null) {
            return "signature";
        }
        if (gitSignRequest != null) {
            return gitSignRequest.analyticsCategory();
        }
        if (hostsRequest != null) {
            return "hosts";
        }
        return "no analytics category";
    }

    public boolean containsExactlyOneRequestType() {
        List<Object> requests = new ArrayList<>(Arrays.asList(meRequest, signRequest, gitSignRequest, unpairRequest, hostsRequest));
        Iterator<Object> iter = requests.iterator();
        while (iter.hasNext()) {
            if (iter.next() == null) {
                iter.remove();
            }
        }
        return requests.size() == 1;
    }

    public View fillView(ConstraintLayout container) {
        if (signRequest != null) {
            return signRequest.fillView(container);
        }
        if (gitSignRequest != null) {
            return gitSignRequest.fillView(container);
        }
        if (hostsRequest != null) {
            return hostsRequest.fillView(container);
        }
        return null;
    }
}
