package co.krypt.krypton.utils;

import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.team.TeamService;

/**
 * Created by Kevin King on 4/3/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class Services {
    // Initialization of app-wide event handlers and crash reporting
    public Services() {
        CrashReporting.startANRReporting();
        // Handles async processing and callbacks for silo related requests
        IdentityService.instance();
        // Handles async processing and callbacks for team related requests
        TeamService.instance();
    }
}
