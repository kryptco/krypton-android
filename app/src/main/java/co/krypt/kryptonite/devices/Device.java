package co.krypt.kryptonite.devices;

import java.util.ArrayList;
import java.util.List;

import co.krypt.kryptonite.pairing.Pairing;

/**
 * Created by Kevin King on 12/14/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Device {
    public final String workstationName;

    public Device(Pairing pairing) {
        workstationName = pairing.workstationName;
    }

    public static List<Device> devicesFromPairings(List<Pairing> pairings) {
        List<Device> devices = new ArrayList<>();
        for (Pairing pairing : pairings) {
            devices.add(new Device(pairing));
        }
        return devices;
    }
}
