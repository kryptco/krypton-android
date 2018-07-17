package co.krypt.krypton.u2f;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kevin King on 7/11/18.
 * Copyright 2018. KryptCo, Inc.
 */
public class KnownAppIds {

    private static final Map<String, String> knownAppIdToSite;
    static
    {
        knownAppIdToSite = new HashMap<>();
        knownAppIdToSite.put("https://www.gstatic.com/securitykey/origins.json", "Google");
        knownAppIdToSite.put("https://dashboard.stripe.com/u2f-facets", "Stripe");
        knownAppIdToSite.put("https://www.dropbox.com/u2f-app-id.json", "Dropbox");
        knownAppIdToSite.put("www.dropbox.com", "Dropbox");
        knownAppIdToSite.put("https://github.com/u2f/trusted_facets", "GitHub");
        knownAppIdToSite.put("https://gitlab.com", "GitLab");
        knownAppIdToSite.put("https://demo.yubico.com", "Yubico Demo");
        knownAppIdToSite.put("https://api-9dcf9b83.duosecurity.com", "Duo Demo");
        knownAppIdToSite.put("https://keepersecurity.com", "Keeper");
        knownAppIdToSite.put("https://id.fedoraproject.org/u2f-origins.json", "Fedora");
        knownAppIdToSite.put("https://bitbucket.org", "BitBucket");
        knownAppIdToSite.put("https://sentry.io/auth/2fa/u2fappid.json", "Sentry");
        knownAppIdToSite.put("https://twitter.com/account/login_verification/u2f_trusted_facets.json", "Twitter");
    }

    public static String displayAppId(String appId) {
        if (knownAppIdToSite.containsKey(appId)) {
            return knownAppIdToSite.get(appId);
        }
        if (appId.startsWith("https://www.facebook.com/u2f/app_id/")) {
            return "Facebook";
        }
        return appId.replaceFirst("^https://", "");
    }

}
