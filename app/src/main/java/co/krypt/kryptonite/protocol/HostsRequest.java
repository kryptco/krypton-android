package co.krypt.kryptonite.protocol;

import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import co.krypt.kryptonite.R;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class HostsRequest {
    public View fillView(ConstraintLayout container) {
        View hostsView = View.inflate(container.getContext(), R.layout.hosts_short, container);

        TextView messageText = (TextView) hostsView.findViewById(R.id.message);
        messageText.setText("Send user@host SSH logs?");
        messageText.setMaxLines(Integer.MAX_VALUE);

        return hostsView;
    }
}
