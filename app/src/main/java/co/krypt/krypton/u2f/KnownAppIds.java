package co.krypt.krypton.u2f;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import co.krypt.krypton.R;

/**
 * Created by Kevin King on 7/11/18.
 * Copyright 2018. KryptCo, Inc.
 */
public class KnownAppIds {

    static final KnownAppId DROPBOX = new KnownAppId("Dropbox", R.drawable.dropbox);
    static final KnownAppId GOOGLE = new KnownAppId("Google", R.drawable.google);
    static final KnownAppId STRIPE = new KnownAppId("Stripe", R.drawable.stripe);
    static final KnownAppId GITHUB = new KnownAppId("GitHub", R.drawable.github);
    static final KnownAppId GITLAB = new KnownAppId("GitLab", R.drawable.gitlab);
    static final KnownAppId YUBICO = new KnownAppId("Yubico Demo", R.drawable.login);
    static final KnownAppId DUO = new KnownAppId("Duo Demo", R.drawable.duo);
    static final KnownAppId KEEPER = new KnownAppId("Keeper", R.drawable.keeper);
    static final KnownAppId FEDORA = new KnownAppId("Fedora", R.drawable.fedora);
    static final KnownAppId BITBUCKET = new KnownAppId("BitBucket", R.drawable.bitbucket);
    static final KnownAppId SENTRY = new KnownAppId("Sentry", R.drawable.sentry);
    static final KnownAppId TWITTER = new KnownAppId("Twitter", R.drawable.twitter);
    static final KnownAppId FACEBOOK = new KnownAppId("Facebook", R.drawable.facebook);

    public static final List<KnownAppId> COMMON_APP_IDS;
    static {
        COMMON_APP_IDS = Lists.newArrayList(
                GOOGLE,
                FACEBOOK,
                TWITTER,
                DROPBOX,
                STRIPE,
                GITHUB,
                GITLAB,
                BITBUCKET,
                SENTRY
        );
    }

    private static final Map<String, KnownAppId> knownAppIdToSite;
    static
    {
        knownAppIdToSite = new HashMap<>();
        knownAppIdToSite.put("https://www.gstatic.com/securitykey/origins.json", GOOGLE);
        knownAppIdToSite.put("https://dashboard.stripe.com/u2f-facets", STRIPE);
        knownAppIdToSite.put("https://www.dropbox.com/u2f-app-id.json", DROPBOX);
        knownAppIdToSite.put("www.dropbox.com", DROPBOX);
        knownAppIdToSite.put("https://github.com/u2f/trusted_facets", GITHUB);
        knownAppIdToSite.put("https://gitlab.com", GITLAB);
        knownAppIdToSite.put("https://demo.yubico.com", YUBICO);
        knownAppIdToSite.put("https://api-9dcf9b83.duosecurity.com", DUO);
        knownAppIdToSite.put("https://keepersecurity.com", KEEPER);
        knownAppIdToSite.put("https://id.fedoraproject.org/u2f-origins.json", FEDORA);
        knownAppIdToSite.put("https://bitbucket.org", BITBUCKET);
        knownAppIdToSite.put("https://sentry.io/auth/2fa/u2fappid.json", SENTRY);
        knownAppIdToSite.put("https://twitter.com/account/login_verification/u2f_trusted_facets.json", TWITTER);
    }

    public static String displayAppId(String appId) {
        KnownAppId knownAppId = getKnownAppId(appId);
        if (knownAppId != null) {
            return knownAppId.site;
        }

        return appId.replaceFirst("^https://", "");
    }

    public static @javax.annotation.Nullable KnownAppId getKnownAppId(String appId) {
        if (knownAppIdToSite.containsKey(appId)) {
            return knownAppIdToSite.get(appId);
        }
        if (appId.startsWith("https://www.facebook.com/u2f/app_id/")) {
            return FACEBOOK;
        }
        return null;
    }

    public static Integer displayAppLogo(String appId) {
        KnownAppId knownAppId = getKnownAppId(appId);
        if (knownAppId != null) {
            return knownAppId.logoSrc;
        }
        return R.drawable.login;
    }

    public static class KnownAppId {
        public final String site;
        public final int logoSrc;

        public KnownAppId(String site, Integer logoSrc) {
            this.site = site;
            this.logoSrc = logoSrc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KnownAppId that = (KnownAppId) o;
            return Objects.equals(site, that.site) &&
                    Objects.equals(logoSrc, that.logoSrc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(site, logoSrc);
        }
    }

}
