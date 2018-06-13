/*
 * Copyright (c) 2018. KryptCo, Inc.
 */

package co.krypt.krypton.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.R;
import co.krypt.krypton.u2f.KnownAppIds;

public class U2FAuthenticateRequest extends RequestBody {
    public static final String FIELD_NAME = "u2f_authenticate_request";

    @SerializedName("challenge")
    public byte[] challenge;

    @SerializedName("app_id")
    public String appId;

    @SerializedName("key_handle")
    public byte[] keyHandle;

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E{
        return visitor.visit(this);
    }

    public View fillView(ConstraintLayout container) {
        View v = View.inflate(container.getContext(), R.layout.generic_request_short, container);

        TextView messageText = v.findViewById(R.id.message);
        messageText.setMaxLines(Integer.MAX_VALUE);

        String baseText = "Sign in to " + KnownAppIds.displayAppId(appId);
        messageText.setText(baseText);

        return v;
    }
}
