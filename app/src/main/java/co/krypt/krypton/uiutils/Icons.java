package co.krypt.krypton.uiutils;

import java.util.HashMap;
import java.util.Map;

import co.krypt.krypton.R;
import co.krypt.krypton.u2f.KnownAppIds;

public class Icons {

    public static Integer getIconFromAppId(String appId) {
        KnownAppIds.KnownAppId knownAppId = KnownAppIds.getKnownAppId(appId);
        if (knownAppId != null) {
            return knownAppId.logoSrc;
        }
        return R.drawable.login;
    }

    private static final Map<String, Integer> knownTOTPIssuerToIcon;
    static
    {
        knownTOTPIssuerToIcon = new HashMap<>();
        knownTOTPIssuerToIcon.put("BitBucket", R.drawable.bitbucket);
        knownTOTPIssuerToIcon.put("Dropbox", R.drawable.dropbox);
        knownTOTPIssuerToIcon.put("Facebook", R.drawable.facebook);
        knownTOTPIssuerToIcon.put("Fedora", R.drawable.fedora);
        knownTOTPIssuerToIcon.put("GitHub", R.drawable.github);
        knownTOTPIssuerToIcon.put("GitLab", R.drawable.gitlab);
        knownTOTPIssuerToIcon.put("Google", R.drawable.google);
        knownTOTPIssuerToIcon.put("Keeper", R.drawable.keeper);
        knownTOTPIssuerToIcon.put("Sentry", R.drawable.sentry);
        knownTOTPIssuerToIcon.put("Stripe", R.drawable.stripe);
        knownTOTPIssuerToIcon.put("Twitter", R.drawable.twitter);
    }

    public static Integer getIconFromTOTPIssuer(String issuer) {
        if (knownTOTPIssuerToIcon.containsKey(issuer)) {
            return knownTOTPIssuerToIcon.get(issuer);
        }
        return R.drawable.login;
    }

}
