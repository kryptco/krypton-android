package co.krypt.krypton.u2f;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class U2FAppIdChecker {

    public static void verifyU2FAppId(String origin, String appId) throws SecurityException {
        FacetProvider fetcher = new AppIdFacetFetcher();
        verifyU2FAppId(fetcher, origin, appId);
    }

    public static void verifyU2FAppId(FacetProvider fetcher, String origin, String appId) throws SecurityException {
        if (origin != null) {
            origin = origin.toLowerCase();
        }
        if (appId != null) {
            appId = appId.toLowerCase();
        }
        if (appId == null || appId.isEmpty()) {
            // FIDO AppID & Facet (v1.2) 3.1.2.2
            return;
        }
        if (appId.equals(origin)) {
            // FIDO AppID & Facet (v1.2) 3.1.2.1
            return;
        }

        // FIDO AppID & Facet (v1.2) 3.1.2.3
        if (!checkCanOriginClaimAppId(origin, appId)) {
            throw new SecurityException("origin cannot claim given appId " + appId);
        }

        List<String> trustedFacets = getTrustedFacetsFromAppId(fetcher, appId);

        if (trustedFacets.indexOf(origin) == -1) {
            // FIDO AppId & Facet (v1.2) 3.1.2.16
            throw new SecurityException("Trusted Facets list does not include the request origin " + origin);
        }
    }

    private static boolean checkCanOriginClaimAppId(String origin, String appId) {
        if (appId.equals(origin)) {
            return true;
        }
        String appIdOrigin;
        try {
            appIdOrigin = getOriginFromURL(appId);
        } catch (URISyntaxException e) {
            return false;
        }
        if (appIdOrigin == null) {
            return false;
        }
        String appIdLSPL, originLSPL;
        try {
            appIdLSPL = getLeastSpecificPrivateLabel(appIdOrigin);
            originLSPL = getLeastSpecificPrivateLabel(origin);
        } catch (SecurityException e) {
            return false;
        }
        if (originLSPL.equals(appIdLSPL)) {
            return true;
        }

        // FIDO-AppID-Redirect-Authorized header handling not implemented, so we allow an exception for Google (gstatic.com)
        if (originLSPL.equals("google.com")) {
            return appIdLSPL.equals("gstatic.com");
        }
        // Fail if fall-through
        return false;
    }

    private static String getLeastSpecificPrivateLabel(String origin) throws SecurityException {
        String host;
        if (origin.startsWith("http://")) {
            host = origin.substring(7);
        } else if (origin.startsWith("https://")) {
            host = origin.substring(8);
        } else {
            host = origin;
        }
        if (host.indexOf(':') != -1) {
            host = host.substring(0, host.indexOf(':'));
        }
        if (host.equals("localhost")) {
            return host;
        }
        // Loop over each possible subdomain, from longest to shortest, in order to
        // find the longest matching eTLD first.
        String prev;
        String next = host;
        while (true) {
            int dot = next.indexOf('.');
            if (dot == -1) {
                throw new SecurityException("given origin has no least-specific private label");
            }
            prev = next;
            next = next.substring(dot + 1);
            if (Arrays.asList(EtldProvider.ETLD_NAMES_LIST).contains(next)) {
                return prev;
            }
        }
    }

    public static String getOriginFromURL(String url) throws URISyntaxException {
        URI uri = new URI(url);
        URI originUri = new URI(uri.getScheme(), uri.getHost(), null, null);
        return originUri.toString();
    }

    private static List<String> getTrustedFacetsFromAppId(FacetProvider fetcher, String appId) throws SecurityException {
        String[] facets = fetcher.getFacetsFromAppId(appId);
        List<String> validFacets = new ArrayList<>();
        for (String facet : facets) {
            facet = facet.toLowerCase();
            // Enforce only HTTPS origins for Trusted Facets per FIDO AppId & Facet (v1.2) 3.1.2.12
            // TODO: allow for valid mobile facets as well
            // FIDO AppID & Facet (v1.2) 3.1.2.14
            if (facet.startsWith("https://") && checkCanOriginClaimAppId(facet, appId)) {
                validFacets.add(facet);
            }
        }
        return validFacets;
    }
}
