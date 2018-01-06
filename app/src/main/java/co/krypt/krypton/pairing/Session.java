package co.krypt.krypton.pairing;

import co.krypt.krypton.log.Log;

/**
 * Created by Kevin King on 12/18/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Session {
    public final Pairing pairing;
    public final Log lastApproval;
    public final boolean approved;

    public Session(Pairing pairing, Log lastApproval, boolean approved) {
        this.pairing = pairing;
        this.lastApproval = lastApproval;
        this.approved = approved;
    }
}
