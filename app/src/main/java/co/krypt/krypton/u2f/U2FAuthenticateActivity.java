package co.krypt.krypton.u2f;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.crypto.U2F;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.U2FAuthenticateRequest;
import co.krypt.krypton.protocol.U2FAuthenticateResponse;
import co.krypt.krypton.protocol.U2FRegisterRequest;
import co.krypt.krypton.protocol.U2FRegisterResponse;

public class U2FAuthenticateActivity extends AppCompatActivity {

    private static final String TAG = U2FAuthenticateActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u2f_authenticate);

        List<String> allowedCallingPackages = Lists.newArrayList(
                "com.android.chrome"
        );

        String callingPackage = getCallingPackage();

        if (!allowedCallingPackages.contains(callingPackage)) {
            Log.e(TAG, "invalid calling package: " + callingPackage);
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent != null) {
            if ("com.google.android.apps.authenticator.AUTHENTICATE".equals(intent.getAction())) {
                if (intent.getStringExtra("request") != null) {
                    try {
                        String requestJSON = URLDecoder.decode(intent.getStringExtra("request"), "UTF-8");
                        Log.d(TAG, requestJSON);

                        JsonObject obj = new JsonParser().parse(requestJSON).getAsJsonObject();
                        JsonElement type = obj.get("type");
                        if (type == null) {
                            Log.e(TAG, "no type");
                            finish();
                            return;
                        }
                        if ("u2f_register_request".equals(type.getAsString())) {
                            ChromeU2FRegisterRequest chromeRequest = JSON.fromJson(requestJSON, ChromeU2FRegisterRequest.class);
                            handleU2FRegister(intent, chromeRequest, callingPackage);
                            finish();
                            return;
                        } else if ("u2f_sign_request".equals(type.getAsString())){
                            ChromeU2FAuthenticateRequest chromeRequest = JSON.fromJson(requestJSON, ChromeU2FAuthenticateRequest.class);
                            handleU2FAuthenticate(intent, chromeRequest, callingPackage);
                            finish();
                            return;
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (CryptoException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        finish();
    }

    private void handleU2FAuthenticate(Intent intent, ChromeU2FAuthenticateRequest chromeRequest, String callingPackage) throws CryptoException, IOException {
        U2FAuthenticateRequest request = new U2FAuthenticateRequest();
        request.appId = chromeRequest.appId;
        String clientData = ClientData.createAuthenticate(this, chromeRequest.challenge, callingPackage);
        request.challenge = SHA256.digest(clientData.getBytes("UTF-8"));
        byte[] keyHandle = null;
        for (RegisteredKey key: chromeRequest.registeredKeys) {
            byte[] potentialKeyHandle = Base64.decodeURLSafe(key.keyHandle);
            if (U2F.keyHandleMatches(potentialKeyHandle, request.appId)) {
                keyHandle = potentialKeyHandle;
                break;
            }
        }
        if (keyHandle == null) {
            Log.e(TAG, "no matching key");
            return;
        }
        request.keyHandle = keyHandle;
        U2F.KeyPair keyPair = U2F.KeyManager.loadAccountKeyPair(this, keyHandle);
        U2FAuthenticateResponse response = keyPair.signU2FAuthenticateRequest(request);

        ChromeU2FResponse chromeResponse = new ChromeU2FResponse();
        chromeResponse.requestId = chromeRequest.requestId;
        chromeResponse.type = "u2f_sign_response";
        ChromeU2FResponse.SignResponseData responseData = new ChromeU2FResponse.SignResponseData();
        chromeResponse.responseData = responseData;
        responseData.clientData = Base64.encodeURLSafe(clientData.getBytes("UTF-8"));
        responseData.keyHandle = Base64.encodeURLSafe(keyHandle);

        ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
        DataOutputStream signatureDataFmt = new DataOutputStream(signatureData);
        signatureDataFmt.writeByte(0x01);
        signatureDataFmt.writeInt((int) response.counter);
        signatureDataFmt.write(response.signature);
        signatureDataFmt.close();

        responseData.signatureData = Base64.encodeURLSafe(signatureData.toByteArray());

        Log.d(TAG, JSON.toJson(chromeResponse));
        intent.putExtra("resultData", JSON.toJson(chromeResponse));
        setResult(RESULT_OK, intent);

    }

    private void handleU2FRegister(Intent intent, ChromeU2FRegisterRequest chromeRequest, String callingPackage) throws CryptoException, IOException {
        U2FRegisterRequest request = new U2FRegisterRequest();
        for (RegisteredKey registeredKey: chromeRequest.registeredKeys) {
            if (U2F.keyHandleMatches(Base64.decodeURLSafe(registeredKey.keyHandle), chromeRequest.appId)) {
                Log.e(TAG, "already registered");
                return;
            }
        }
        if (chromeRequest.registerRequests.length < 1) {
            Log.e(TAG, "no register requests");
            return;
        }
        String clientData = ClientData.createRegister(this, chromeRequest.registerRequests[0].challenge, callingPackage);
        request.challenge = SHA256.digest(clientData.getBytes("UTF-8"));
        request.appId = chromeRequest.appId;
        U2F.KeyPair keyPair = U2F.KeyManager.generateAccountKeyPair(this, request.appId);
        U2FRegisterResponse response = keyPair.signU2FRegisterRequest(request);

        ChromeU2FResponse chromeResponse = new ChromeU2FResponse();
        chromeResponse.requestId = chromeRequest.requestId;
        chromeResponse.type = "u2f_register_response";
        ChromeU2FResponse.RegisterResponseData responseData = new ChromeU2FResponse.RegisterResponseData();
        chromeResponse.responseData = responseData;
        responseData.clientData = Base64.encodeURLSafe(clientData.getBytes("UTF-8"));
        responseData.version = "U2F_V2";

        ByteArrayOutputStream registerData = new ByteArrayOutputStream();
        DataOutputStream registerDataFmt = new DataOutputStream(registerData);
        registerDataFmt.writeByte(0x05);
        registerDataFmt.write(response.publicKey);
        registerDataFmt.writeByte(response.keyHandle.length);
        registerDataFmt.write(response.keyHandle);
        registerDataFmt.write(response.attestationCertificate);
        registerDataFmt.write(response.signature);
        registerDataFmt.close();

        responseData.registrationData = Base64.encodeURLSafe(registerData.toByteArray());

        Log.d(TAG, JSON.toJson(chromeResponse));
        intent.putExtra("resultData", JSON.toJson(chromeResponse));
        setResult(RESULT_OK, intent);

    }

    public static class ChromeU2FRegisterRequest {
        @JSON.JsonRequired
        public String appId;
        @JSON.JsonRequired
        public Challenge[] registerRequests;
        @JSON.JsonRequired
        public RegisteredKey[] registeredKeys;
        public Long requestId;

        public static class Challenge {
            @JSON.JsonRequired
            public String challenge;
        }
    }

    public static class ChromeU2FAuthenticateRequest {
        @JSON.JsonRequired
        public String appId;
        @JSON.JsonRequired
        public String challenge;
        @JSON.JsonRequired
        public RegisteredKey[] registeredKeys;
        public Long requestId;
    }

    public static class ChromeU2FResponse {
        public String type;

        @Nullable
        public Long requestId;

        @JSON.JsonRequired
        public ResponseData responseData;

        public interface ResponseData {};

        public static class RegisterResponseData implements ResponseData {
            public String version;
            public String registrationData;
            public String clientData;
        }

        public static class SignResponseData implements ResponseData {
            public String keyHandle;
            public String signatureData;
            public String clientData;
        }
    }

    public static class ClientData {
        public String typ;
        public String challenge;
        @Nullable public String origin;
        public String cid_pubkey;

        public static String createRegister(Context context, String challenge, @Nullable String callingPackage) {
            ClientData cd = new ClientData();
            cd.typ = "navigator.id.finishEnrollment";
            cd.challenge = challenge;
            cd.origin = computeFacetId(context, callingPackage);
            cd.cid_pubkey = "unused";

            return JSON.toJson(cd);
        }

        public static String createAuthenticate(Context context, String challenge, @Nullable String callingPackage) {
            ClientData cd = new ClientData();
            cd.typ = "navigator.id.getAssertion";
            cd.challenge = challenge;
            cd.origin = computeFacetId(context, callingPackage) + "bad";
            cd.cid_pubkey = "unused";

            return JSON.toJson(cd);
        }

        private static String computeFacetId(Context context, String callingPackage) {
            if (callingPackage == null) {
                return null;
            }
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(callingPackage, PackageManager.GET_SIGNATURES);

                byte[] cert = info.signatures[0].toByteArray();
                InputStream input = new ByteArrayInputStream(cert);

                CertificateFactory cf = CertificateFactory.getInstance("X509");
                X509Certificate c = (X509Certificate) cf.generateCertificate(input);

                MessageDigest md = MessageDigest.getInstance("SHA1");

                return "android:apk-key-hash:" +
                        android.util.Base64.encodeToString(md.digest(c.getEncoded()), android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
            }
            catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | CertificateException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    public static class RegisteredKey {
        @JSON.JsonRequired
        public String keyHandle;
    }
}
