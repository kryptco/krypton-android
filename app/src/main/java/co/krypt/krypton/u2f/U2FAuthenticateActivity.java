package co.krypt.krypton.u2f;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.List;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.crypto.SHA256;
import co.krypt.krypton.crypto.U2F;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.exception.InvalidAppIdException;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.U2FAuthenticateRequest;
import co.krypt.krypton.protocol.U2FAuthenticateResponse;
import co.krypt.krypton.protocol.U2FRegisterRequest;
import co.krypt.krypton.protocol.U2FRegisterResponse;
import co.krypt.krypton.uiutils.Error;

public class U2FAuthenticateActivity extends AppCompatActivity {

    private static final String TAG = U2FAuthenticateActivity.class.getName();

    //TODO: fix exception thrown when activity left without taking action and re-opened from another request
    private AlertDialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u2f_authenticate);

        List<String> allowedCallingPackages = Lists.newArrayList(
                "com.android.chrome",
                "com.chrome.canary"
        );

        String callingPackage = getCallingPackage();

        if (!allowedCallingPackages.contains(callingPackage)) {
            Log.e(TAG, "invalid calling package: " + callingPackage);
            finish();
            return;
        }

        Intent intent = getIntent();
        new Thread(() -> {
            if (intent != null) {
                if ("com.google.android.apps.authenticator.AUTHENTICATE".equals(intent.getAction())) {
                    String facetId = intent.getStringExtra("referrer");
                    if (facetId == null) {
                        Log.e(TAG, "no referrer");
                        finish();
                        return;
                    }

                    if (intent.getStringExtra("request") != null) {
                        try {
                            URI facetIdUri = new URI(facetId);
                            facetId = facetIdUri.getScheme() + "://" + facetIdUri.getHost();

                            String requestJSON = URLDecoder.decode(intent.getStringExtra("request"), "UTF-8");

                            JsonObject obj = new JsonParser().parse(requestJSON).getAsJsonObject();
                            JsonElement type = obj.get("type");
                            if (type == null) {
                                Log.e(TAG, "no type");
                                finish();
                                return;
                            }
                            if ("u2f_register_request".equals(type.getAsString())) {
                                ChromeU2FRegisterRequest chromeRequest = JSON.fromJson(requestJSON, ChromeU2FRegisterRequest.class);
                                handleU2FRegister(intent, chromeRequest, facetId);
                                return;
                            } else if ("u2f_sign_request".equals(type.getAsString())) {
                                ChromeU2FAuthenticateRequest chromeRequest = JSON.fromJson(requestJSON, ChromeU2FAuthenticateRequest.class);
                                handleU2FAuthenticate(intent, chromeRequest, facetId);
                                return;
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (CryptoException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InvalidAppIdException e) {
                            //TODO return error code for bad request
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    finish();
                }
            }
        }).start();
    }

    private void handleU2FAuthenticate(Intent intent, ChromeU2FAuthenticateRequest chromeRequest, String facetId) throws InvalidAppIdException, CryptoException, IOException {
        U2FAppIdChecker.verifyU2FAppId(facetId, chromeRequest.appId);
        final U2FAuthenticateRequest request = new U2FAuthenticateRequest();
        request.appId = chromeRequest.appId;
        String clientData = ClientData.createAuthenticate(this, chromeRequest.challenge, facetId);
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
            Error.longToast(U2FAuthenticateActivity.this, "Krypton not added to this account");
            finish();
            return;
        }
        request.keyHandle = keyHandle;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("Krypton Authenticator")
                .setMessage("Sign-in to " + KnownAppIds.displayAppId(request.appId) + "?")
                .setPositiveButton("YES", (d, v) -> {
                    try {

                        U2F.KeyPair keyPair = U2F.KeyManager.loadAccountKeyPair(this, request.keyHandle);
                        U2FAuthenticateResponse response = keyPair.signU2FAuthenticateRequest(request);

                        ChromeU2FResponse chromeResponse = new ChromeU2FResponse();
                        chromeResponse.requestId = chromeRequest.requestId;
                        chromeResponse.type = "u2f_sign_response";
                        ChromeU2FResponse.SignResponseData responseData = new ChromeU2FResponse.SignResponseData();
                        chromeResponse.responseData = responseData;
                        responseData.clientData = Base64.encodeURLSafe(clientData.getBytes("UTF-8"));
                        responseData.keyHandle = Base64.encodeURLSafe(request.keyHandle);

                        ByteArrayOutputStream signatureData = new ByteArrayOutputStream();
                        DataOutputStream signatureDataFmt = new DataOutputStream(signatureData);
                        signatureDataFmt.writeByte(0x01);
                        signatureDataFmt.writeInt((int) response.counter);
                        signatureDataFmt.write(response.signature);
                        signatureDataFmt.close();

                        responseData.signatureData = Base64.encodeURLSafe(signatureData.toByteArray());

                        intent.putExtra("resultData", JSON.toJson(chromeResponse));
                        setResult(RESULT_OK, intent);
                    } catch (IOException | CryptoException e) {
                        Error.longToast(U2FAuthenticateActivity.this, e.getLocalizedMessage());
                    }
                    finish();
                })
                .setNegativeButton("NO", (d, v) -> {
                    finish();
                })
                .setOnCancelListener((v) -> {
                    finish();
                });
        runOnUiThread(() -> {
            dialog = builder.create();
            dialog.show();
        });
    }

    private void handleU2FRegister(Intent intent, ChromeU2FRegisterRequest chromeRequest, String facetId) throws InvalidAppIdException, CryptoException, IOException {
        U2FAppIdChecker.verifyU2FAppId(facetId, chromeRequest.appId);
        U2FRegisterRequest request = new U2FRegisterRequest();
        for (RegisteredKey registeredKey: chromeRequest.registeredKeys) {
            if (U2F.keyHandleMatches(Base64.decodeURLSafe(registeredKey.keyHandle), chromeRequest.appId)) {
                Log.e(TAG, "already registered");
                finish();
                return;
            }
        }
        if (chromeRequest.registerRequests.length < 1) {
            Log.e(TAG, "no register requests");
            finish();
            return;
        }
        String clientData = ClientData.createRegister(this, chromeRequest.registerRequests[0].challenge, facetId);
        request.challenge = SHA256.digest(clientData.getBytes("UTF-8"));
        request.appId = chromeRequest.appId;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("Krypton Authenticator")
                .setMessage("Register with " + KnownAppIds.displayAppId(request.appId) + "?")
                .setPositiveButton("YES", (d, v) -> {
                    try {
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

                        intent.putExtra("resultData", JSON.toJson(chromeResponse));
                        setResult(RESULT_OK, intent);
                    } catch (IOException | CryptoException e) {
                        Error.longToast(getApplicationContext(), e.getLocalizedMessage());
                        //TODO: send error intent result
                    }
                    finish();
                })
                .setNegativeButton("NO", (d, v) -> {
                    //TODO: send error code
                    finish();
                })
                .setOnCancelListener((v) -> {
                    finish();
                });
        runOnUiThread(() -> {
            dialog = builder.create();
            dialog.show();
        });
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
        public String origin;
        public String cid_pubkey;

        public static String createRegister(Context context, String challenge, String facetId) {
            ClientData cd = new ClientData();
            cd.typ = "navigator.id.finishEnrollment";
            cd.challenge = challenge;
            cd.origin = facetId;
            cd.cid_pubkey = "unused";

            return JSON.toJson(cd);
        }

        public static String createAuthenticate(Context context, String challenge, String facetId) {
            ClientData cd = new ClientData();
            cd.typ = "navigator.id.getAssertion";
            cd.challenge = challenge;
            cd.origin = facetId;
            cd.cid_pubkey = "unused";

            return JSON.toJson(cd);
        }
    }

    public static class RegisteredKey {
        @JSON.JsonRequired
        public String keyHandle;
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void finish() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
