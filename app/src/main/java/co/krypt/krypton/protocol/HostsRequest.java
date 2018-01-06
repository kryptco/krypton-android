package co.krypt.krypton.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostsRequest extends RequestBody {
    public static final String FIELD_NAME = "hosts_request";

    public View fillView(ConstraintLayout container) {
        View hostsView = View.inflate(container.getContext(), R.layout.hosts_short, container);

        TextView messageText = (TextView) hostsView.findViewById(R.id.message);
        messageText.setText("Send user@host SSH logs?");
        messageText.setMaxLines(Integer.MAX_VALUE);

        return hostsView;
    }

    @Override
    public <T, E extends Throwable> T visit(Visitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }
}
