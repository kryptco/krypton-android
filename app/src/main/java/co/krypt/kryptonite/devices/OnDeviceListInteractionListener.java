package co.krypt.kryptonite.devices;

import co.krypt.kryptonite.pairing.Pairing;

/**
 * Created by Kevin King on 12/14/16.
 * Copyright 2016. KryptCo, Inc.
 */
public interface OnDeviceListInteractionListener {
        void onListFragmentInteraction(Pairing item);
}
