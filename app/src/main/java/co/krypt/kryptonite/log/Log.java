package co.krypt.kryptonite.log;

/**
 * Created by Kevin King on 6/19/17.
 * Copyright 2017. KryptCo, Inc.
 */

public interface Log {
    long unixSeconds();

    String shortDisplay();

    String longDisplay();
}
