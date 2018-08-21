package co.krypt.krypton.u2f;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import co.krypt.krypton.protocol.JSON;

/**
 * Resolves an AppId URL, parses the JSON present, and returns an array of facets (including untrusted ones)
 */
public class AppIdFacetFetcher implements FacetProvider {
    @Override
    public String[] getFacetsFromAppId(String appId) {
        return getFacetsFromAppId(appId, 5);
    }

    public static String[] getFacetsFromAppId(String appId, int remainingRetryAttempts) throws SecurityException {
        // Sanity/safety checks
        {
            if (remainingRetryAttempts <= 0) {
                return new String[0];
            }
            if (appId == null || appId.isEmpty()) {
                return new String[0];
            }

            if (appId.startsWith("http://")) {
                throw new SecurityException("http app ids not allowed");
            }

            String origin;
            try {
                origin = U2FAppIdChecker.getOriginFromURL(appId);
            } catch (URISyntaxException e) {
                return new String[0];
            }
            if (origin == null || origin.isEmpty()) {
                return new String[0];
            }
        }
        // Fetch TrustedFacetsList
        {
            try {
                URL appIdUrl = new URL(appId);
                HttpURLConnection connection = (HttpURLConnection) appIdUrl.openConnection();
                return parseTrustedFacets(connection.getInputStream());
            } catch (IOException e) {
                return getFacetsFromAppId(appId, remainingRetryAttempts - 1);
            }
        }
    }

    public static String[] parseTrustedFacets(InputStream appIdStream) {
        JsonReader jr = new JsonReader(new InputStreamReader(appIdStream, Charset.forName("UTF-8")));
        String[] parsedFacets;
        try {
            AppIdContents appIdContents = JSON.fromJson(jr, AppIdContents.class);
            parsedFacets = appIdContents.trustedFacets[0].ids;
        } catch (JsonSyntaxException e) {
            //Attempt to parse using the old format where the content is just an array of facets
            parsedFacets = JSON.fromJson(jr, String[].class);
        }
        List<String> facets = new ArrayList<>();
        for (String facet : parsedFacets) {
            try {
                String origin = U2FAppIdChecker.getOriginFromURL(facet);
                if (origin != null) {
                    facets.add(origin);
                }
            } catch (Exception e) {
                //Continue
            }
        }
        return facets.toArray(new String[facets.size()]);
    }

    public static class AppIdContents {
        @JSON.JsonRequired
        public TrustedFacetDictionary[] trustedFacets;

        public static class TrustedFacetDictionary {
            @JSON.JsonRequired
            public Version version;
            @JSON.JsonRequired
            public String[] ids;

            public static class Version {
                @JSON.JsonRequired
                public int major;
                @JSON.JsonRequired
                public int minor;
            }
        }
    }
}