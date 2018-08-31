package co.krypt.krypton.protocol;

import android.net.Uri;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.exception.CryptoException;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class PairingQR {
    @JSON.JsonRequired
    @SerializedName("pk")
    public byte[] workstationPublicKey;
    @JSON.JsonRequired
    @SerializedName("n")
    public String workstationName;

    // TODO: use deviceId to invalidate existing pairings
    @SerializedName("d")
    @Nullable
    public byte[] deviceId;
    @SerializedName("b")
    @Nullable
    public String browser;

    //  version null in kr version < 2.0.0
    @SerializedName("v")
    public String version;

    private PairingQR() {
    }

    public static PairingQR parseJson(String jsonOrLink) throws JsonParseException {
        if (jsonOrLink.startsWith("https://get.krypt.co")) {
            try {
                return JSON.fromJson(Base64.decodeURLSafe(Uri.parse(jsonOrLink).getFragment()), PairingQR.class);
            } catch (CryptoException e) {
                throw new JsonParseException(e);
            }
        }
        return JSON.fromJson(jsonOrLink, PairingQR.class);
    }
}
