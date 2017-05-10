package co.krypt.kryptonite;

import com.amazonaws.util.Base64;
import com.google.gson.JsonParseException;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;

import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.SignRequest;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ClientAuthParsingTest {
    @Test
    public void clientAuthParsingRSASHA512_works() throws Exception {
        SignRequest signRequest = new SignRequest();
        signRequest.data = Base64.decode("AAAAIJplnt2SRhPtYQqFfZcJwsSYrIIMaAjOxhTlrM/zNV6WMgAAAARyb290AAAADnNzaC1jb25uZWN0aW9uAAAACXB1YmxpY2tleQEAAAAMcnNhLXNoYTItNTEy");
        Assert.assertTrue(signRequest.algo().equals("rsa-sha2-512"));
        Assert.assertTrue(signRequest.user().equals("root"));
        Assert.assertTrue(Arrays.equals(signRequest.sessionID(), Base64.decode("mmWe3ZJGE+1hCoV9lwnCxJisggxoCM7GFOWsz/M1XpY=")));
    }
    @Test
    public void clientAuthParsingRSASHA256_works() throws Exception {
        SignRequest signRequest = new SignRequest();
        signRequest.data = Base64.decode("AAAAIFFE46OxITOv4tIsO9u7Jq/W6vnf4diwbdSEoeGMDB9yMgAAAAh0ZXN0dXNlcgAAAA5zc2gtY29ubmVjdGlvbgAAAAlwdWJsaWNrZXkBAAAADHJzYS1zaGEyLTI1Ng==");
        Assert.assertTrue(signRequest.algo().equals("rsa-sha2-256"));
        Assert.assertTrue(signRequest.user().equals("testuser"));
        Assert.assertTrue(Arrays.equals(signRequest.sessionID(), Base64.decode("UUTjo7EhM6/i0iw727smr9bq+d/h2LBt1ISh4YwMH3I=")));
    }
    @Test
    public void clientAuthParsingRSASHA1_works() throws Exception {
        SignRequest signRequest = new SignRequest();
        signRequest.data = Base64.decode("AAAAIFrZQlwF8k3UCrkwZ2E0U+qGx57wehv5ABkHJStoOCc3MgAAAANnaXQAAAAOc3NoLWNvbm5lY3Rpb24AAAAJcHVibGlja2V5AQAAAAdzc2gtcnNh");
        Assert.assertTrue(signRequest.algo().equals("ssh-rsa"));
        Assert.assertTrue(signRequest.user().equals("git"));
        Assert.assertTrue(Arrays.equals(signRequest.sessionID(), Base64.decode("WtlCXAXyTdQKuTBnYTRT6obHnvB6G/kAGQclK2g4Jzc=")));
    }
}