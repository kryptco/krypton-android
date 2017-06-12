package co.krypt.kryptonite.pgp.packet;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Kevin King on 6/15/17.
 * Copyright 2017. KryptCo, Inc.
 */

public interface Signable {
    void writeSignableData(DataOutputStream out) throws IOException;
}
