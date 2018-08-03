package co.krypt.krypton.u2f;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.crypto.U2F;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.U2FAuthenticateRequest;
import co.krypt.krypton.protocol.U2FAuthenticateResponse;

public class U2FAuthenticateActivity extends AppCompatActivity {

    private static final String TAG = U2FAuthenticateActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u2f_authenticate);

        Intent intent = getIntent();
        if (intent != null) {
            if ("com.google.android.apps.authenticator.AUTHENTICATE".equals(intent.getAction())) {
                if (intent.getStringExtra("request") != null) {
                    try {
                        String requestJSON = URLDecoder.decode(intent.getStringExtra("request"), "UTF-8");
                        ChromeU2FAuthenticateRequest chromeRequest = JSON.fromJson(requestJSON, ChromeU2FAuthenticateRequest.class);
                        U2FAuthenticateRequest request = new U2FAuthenticateRequest();
                        request.appId = chromeRequest.appId;
                        String clientData = ClientData.create(chromeRequest.challenge, getCallingPackage());
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

                        ChromeU2FAuthenticateResponse chromeResponse = new ChromeU2FAuthenticateResponse();
                        chromeResponse.requestId = chromeRequest.requestId;
                        chromeResponse.type = "u2f_sign_response";
                        chromeResponse.responseData = new ChromeU2FAuthenticateResponse.ResponseData();
                        chromeResponse.responseData.clientData = Base64.encodeURLSafe(clientData.getBytes("UTF-8"));
                        chromeResponse.responseData.keyHandle = Base64.encodeURLSafe(keyHandle);

                        ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
                        DataOutputStream signatureDataFmt = new DataOutputStream(signatureData);
                        signatureDataFmt.writeByte(0x01);
                        signatureDataFmt.writeInt((int) response.counter);
                        signatureDataFmt.write(response.signature);
                        signatureDataFmt.close();

                        chromeResponse.responseData.signatureData = Base64.encodeURLSafe(signatureData.toByteArray());

                        Log.d(TAG, requestJSON);
                        Log.d(TAG, JSON.toJson(chromeResponse));
                        intent.putExtra("response", JSON.toJson(chromeResponse));
//                        setResult(RESULT_OK, intent);
                        finish();
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

    public static class ChromeU2FAuthenticateResponse {
        public String type;

        @Nullable
        public Long requestId;

        @JSON.JsonRequired
        public ResponseData responseData;

        public static class ResponseData {
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

        public static String create(String challenge, @Nullable String callingPackage) {
            ClientData cd = new ClientData();
            cd.typ = "navigator.id.getAssertion";
            cd.challenge = challenge;
            cd.origin = callingPackage;
            cd.cid_pubkey = "unused";

            return JSON.toJson(cd);
        }
    }


    public static class RegisteredKey {
        public String keyHandle;
    }
}
