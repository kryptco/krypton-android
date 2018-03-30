package co.krypt.krypton.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;

/**
 * Created by Kevin King on 2/19/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class TeamOperationRequest extends RequestBody {
    public static final String FIELD_NAME = "team_operation_request";

    @SerializedName("operation")
    @JSON.JsonRequired
    public Sigchain.RequestableTeamOperation operation;

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public View fillView(ConstraintLayout container) {

        View v = View.inflate(container.getContext(), R.layout.generic_request_short, container);

        try {
            Sigchain.NativeResult<Sigchain.FormattedRequestableOp> format = TeamDataProvider.formatRequestableOp(container.getContext(), operation);

            TextView messageText = v.findViewById(R.id.message);
            messageText.setMaxLines(Integer.MAX_VALUE);

            if (format.success != null) {
                messageText.setText(format.success.body);
            } else {
                messageText.setText("Invalid team operation request");
            }
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }

        return v;
    }
}
