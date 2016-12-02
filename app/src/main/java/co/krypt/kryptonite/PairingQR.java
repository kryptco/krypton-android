package co.krypt.kryptonite;

import com.google.android.gms.common.api.Api;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Kevin King on 12/2/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class PairingQR {
    @SerializedName("pk")
    public byte[] workstationPublicKey;
    @SerializedName("n")
    public String workstationName;

    private PairingQR() {
    }

    public static PairingQR parseJson(String json) {
        PairingQR pairingQR = JSON.fromJson(json, PairingQR.class);
        if (
                pairingQR.workstationPublicKey == null ||
                pairingQR.workstationName == null) {
            throw new JsonParseException(json + " INVALID: missing one or more fields");
        }
        return pairingQR;
    }
}
