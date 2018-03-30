package co.krypt.krypton.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 2/19/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class LogDecryptionRequest extends RequestBody {
    public static final String FIELD_NAME = "log_decryption_request";

    @SerializedName("wrapped_key")
    @JSON.JsonRequired
    public JsonObject wrappedKey;

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public View fillView(ConstraintLayout container) {
        View v = View.inflate(container.getContext(), R.layout.generic_request_short, container);

        TextView messageText = v.findViewById(R.id.message);
        messageText.setText("Decrypt team logs");
        messageText.setMaxLines(Integer.MAX_VALUE);

        return v;
    }
}
