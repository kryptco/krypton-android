package co.krypt.kryptonite;

import com.google.gson.JsonParseException;

import junit.framework.Assert;

import org.junit.Test;

import co.krypt.kryptonite.git.CommitInfo;
import co.krypt.kryptonite.protocol.GitSignRequest;
import co.krypt.kryptonite.protocol.JSON;
import co.krypt.kryptonite.protocol.Request;
import co.krypt.kryptonite.protocol.SignRequest;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class JSONUnitTest {
    @Test(expected=JsonParseException.class)
    public void requestDeserializationMissingFields_fails() throws Exception {
        String json = "{\"request_id\":\"132\", \"v\": \"1.0.0\"}";
        JSON.fromJson(json, Request.class);
    }
    @Test
    public void requestDeserialization_works() throws Exception {
        String json = "{\"request_id\":\"132\", \"unix_seconds\":0, \"v\": \"1.0.0\", \"me_request\":{}}";
        JSON.fromJson(json, Request.class);
    }
    @Test
    public void nestedRequestDeserialization_works() throws Exception {
        String json = "{\"request_id\":\"132\", \"unix_seconds\":0, \"sign_request\":{\"public_key_fingerprint\":\"aGkK\", \"data\": \"aGkK\"}, \"v\": \"1.0.0\"}";
        Request request = JSON.fromJson(json, Request.class);
        if (!(request.body instanceof SignRequest)) {
            throw new JsonParseException("expected sign request");
        }
    }
    @Test(expected = JsonParseException.class)
    public void nestedRequestDeserializationMissingFields_fails() throws Exception {
        String json = "{\"request_id\":\"132\", \"unix_seconds\":0, \"sign_request\":{\"public_key_fingerprint\":\"aGkK\"}, \"v\": \"1.0.0\"}";
        JSON.fromJson(json, Request.class);
    }
    @Test
    public void gitSignRequestDeserialization_works() throws Exception {
        String json = "{\"request_id\":\"132\", \"unix_seconds\":0, \"v\": \"1.0.0\", \"git_sign_request\":{\"user_id\": \"kevin\", " +
                "\"commit\": {\"tree\":\"7fe58682fc6e3cb5e90f77f74ae479eb41d2a13a\", \"author\": \"John Doe <jd@example.com>\", \"committer\": \"John Doe <jd@example.com>\", \"message\": \"bWVzc2FnZQo=\"}}}";
        Request request = JSON.fromJson(json, Request.class);
        Assert.assertTrue(request.body instanceof GitSignRequest);
        Assert.assertTrue(((GitSignRequest) request.body).body instanceof CommitInfo);
    }
}