package co.krypt.kryptonite.pairing;

import co.krypt.kryptonite.log.Log;

/**
 * Created by Kevin King on 12/18/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Session {
    public final Pairing pairing;
    public final Log lastApproval;
    public final boolean approved;
    public final Long approvedUntilUnixSeconds;

    public Session(Pairing pairing, Log lastApproval, boolean approved, Long approvedUntilUnixSeconds) {
        this.pairing = pairing;
        this.lastApproval = lastApproval;
        this.approved = approved;
        this.approvedUntilUnixSeconds = approvedUntilUnixSeconds;
    }
}
