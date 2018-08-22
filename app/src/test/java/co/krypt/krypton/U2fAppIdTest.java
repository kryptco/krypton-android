package co.krypt.krypton;

import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import co.krypt.krypton.u2f.AppIdFacetFetcher;
import co.krypt.krypton.u2f.FacetProvider;
import co.krypt.krypton.u2f.U2FAppIdChecker;

public class U2fAppIdTest {
    @Test
    public void nullAppId_works() throws Exception {
        FacetProvider fetcher = (String appId) -> new String[0];
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", null);
    }
    @Test
    public void emptyAppId_works() throws Exception {
        FacetProvider fetcher = (String appId) -> new String[0];
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", "");
    }
    @Test
    public void equivalentAppId_works() throws Exception {
        FacetProvider fetcher = (String appId) -> new String[0];
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", "https://example.com");
    }
    @Test(expected = SecurityException.class)
    public void differentLSPL_fails() throws Exception {
        FacetProvider fetcher = (String appId) -> new String[0];
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com","https://acme.com");
    }
    @Test(expected = Exception.class)
    public void invalidPrivateOrigin_fails() throws Exception {
        FacetProvider fetcher = (String appId) -> new String[0];
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://com", "https://example.com");
    }
    @Test(expected = SecurityException.class)
    public void httpFacet_fails() throws Exception {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[1];
            facets[0] = "http://example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", "https://example.com/appId.json");
    }
    @Test
    public void multipleCorrectOrigins_works() {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[5];
            facets[0] = "https://port-and-path.example.com:25565/index.html";
            facets[1] = "https://service-a.example.com";
            facets[2] = "https://service-b.example.com";
            facets[3] = "https://lOvEmYsHiFtKeY.example.com";
            facets[4] = "https://u2f.example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://u2f.example.com", "https://u2f.example.com/appId.json");
    }
    @Test(expected = SecurityException.class)
    public void originNotInFacets_fails() {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[4];
            facets[0] = "https://port-and-path.example.com:25565/index.html";
            facets[1] = "https://service-a.example.com";
            facets[2] = "https://service-b.example.com";
            facets[3] = "https://lOvEmYsHiFtKeY.example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://unprivileged-service.example.com", "https://u2f.example.com/appId.json");
    }
    @Test
    public void caseInsensitive_works() {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[1];
            facets[0] = "https://U2F.example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://u2f.example.com", "https://u2f.example.com/appId.json");
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://U2f.example.com", "https://u2f.example.com/appId.json");
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://u2f.example.com", "https://u2F.example.com/appId.json");
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://U2f.example.com", "https://u2F.example.com/appId.json");
    }
    @Test
    public void differentSubdomainAppId_Works() {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[1];
            facets[0] = "https://example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", "https://u2f.example.com/appId.json");
    }
    @Test(expected = SecurityException.class)
    public void fakeAppIdUrl_fails() {
        FacetProvider fetcher = (String appId) -> {
            String[] facets = new String[1];
            facets[0] = "https://example.com";
            return facets;
        };
        U2FAppIdChecker.verifyU2FAppId(fetcher, "https://example.com", "https://u2f.example.com.phishing.net");
    }

    @Test
    public void facetFetcherParseBasicAppIdContent_works() throws Exception {
        String json = "{\n" +
                        "  \"trustedFacets\": [{\n" +
                        "    \"version\": { \"major\": 1, \"minor\" : 0 },\n" +
                        "    \"ids\": [\n" +
                        "        \"https://example.com\",\n" +
                        "    ]\n" +
                        "  }]\n" +
                        "}\n" +
                        "\n";
        InputStream appIdStream = new ByteArrayInputStream(json.getBytes());
        List<String> facets = Arrays.asList(AppIdFacetFetcher.parseTrustedFacets(appIdStream));
        Assert.assertTrue(facets.contains("https://example.com"));
    }
    @Test(expected = Exception.class)
    public void facetFetcherParseMalformedAppIdContent_fails() throws Exception {
        String json = "{]][}";
        InputStream appIdStream = new ByteArrayInputStream(json.getBytes());
        AppIdFacetFetcher.parseTrustedFacets(appIdStream);
    }
    @Test
    public void facetFetcherParseOldFacetFormat_works() throws Exception {
        String json = "[\n" +
                        "  \"https://example.com\",\n" +
                        "  \"https://otherfacet.com\",\n" +
                        "]\n" +
                        "\n";
        InputStream appIdStream = new ByteArrayInputStream(json.getBytes());
        List<String> facets = Arrays.asList(AppIdFacetFetcher.parseTrustedFacets(appIdStream));
        Assert.assertTrue(facets.contains("https://example.com"));
        Assert.assertTrue(facets.contains("https://otherfacet.com"));
    }
}
