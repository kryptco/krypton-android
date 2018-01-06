package co.krypt.krypton.silo;

import android.content.Context;
import android.content.Intent;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.amazonaws.util.Base64;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.crypto.SSHKeyPairI;
import co.krypt.krypton.db.OpenDatabaseHelper;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.exception.MismatchedHostKeyException;
import co.krypt.krypton.exception.ProtocolException;
import co.krypt.krypton.exception.TransportException;
import co.krypt.krypton.git.CommitInfo;
import co.krypt.krypton.git.TagInfo;
import co.krypt.krypton.knownhosts.KnownHost;
import co.krypt.krypton.log.GitCommitSignatureLog;
import co.krypt.krypton.log.GitTagSignatureLog;
import co.krypt.krypton.log.SSHSignatureLog;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.onboarding.TestSSHFragment;
import co.krypt.krypton.pairing.Pairing;
import co.krypt.krypton.pairing.Pairings;
import co.krypt.krypton.pgp.UserID;
import co.krypt.krypton.pgp.asciiarmor.AsciiArmor;
import co.krypt.krypton.pgp.packet.HashAlgorithm;
import co.krypt.krypton.pgp.packet.SignableUtils;
import co.krypt.krypton.policy.Policy;
import co.krypt.krypton.protocol.AckResponse;
import co.krypt.krypton.protocol.GitSignRequest;
import co.krypt.krypton.protocol.GitSignRequestBody;
import co.krypt.krypton.protocol.GitSignResponse;
import co.krypt.krypton.protocol.HostInfo;
import co.krypt.krypton.protocol.HostsRequest;
import co.krypt.krypton.protocol.HostsResponse;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.MeRequest;
import co.krypt.krypton.protocol.MeResponse;
import co.krypt.krypton.protocol.NetworkMessage;
import co.krypt.krypton.protocol.Request;
import co.krypt.krypton.protocol.RequestBody;
import co.krypt.krypton.protocol.Response;
import co.krypt.krypton.protocol.SignRequest;
import co.krypt.krypton.protocol.SignResponse;
import co.krypt.krypton.protocol.UnpairRequest;
import co.krypt.krypton.protocol.UnpairResponse;
import co.krypt.krypton.protocol.UserAndHost;
import co.krypt.krypton.protocol.Versions;
import co.krypt.krypton.transport.BluetoothTransport;
import co.krypt.krypton.transport.SNSTransport;
import co.krypt.krypton.transport.SQSPoller;
import co.krypt.krypton.transport.SQSTransport;

/**
 * Created by Kevin King on 12/3/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class Silo {
    private static final String TAG = "Silo";

    public static final String KNOWN_HOSTS_CHANGED_ACTION = "co.krypt.kryptonite.action.KNOWN_HOSTS_CHANGED";

    public static final long CLOCK_SKEW_TOLERANCE_SECONDS = 15*60;

    private static Silo singleton;

    private final Pairings pairingStorage;
    private final MeStorage meStorage;
    private Map<UUID, Pairing> activePairingsByUUID = new HashMap<>();
    private Map<Pairing, SQSPoller> pollers = new HashMap<>();

    private final BluetoothTransport bluetoothTransport;
    public final Context context;
    private final Map<Pairing, Long> lastRequestTimeSeconds = Collections.synchronizedMap(new HashMap<Pairing, Long>());
    private final LruCache<String, Response> responseMemCacheByRequestID = new LruCache<>(8192);
    @Nullable
    private DiskLruCache responseDiskCacheByRequestID;
    private final OpenDatabaseHelper dbHelper;

    private final Object pairingsLock = new Object();
    private final Object databaseLock = new Object();
    private final Object policyLock = new Object();

    private Silo(Context context) {
        this.context = context;
        pairingStorage = new Pairings(context);
        meStorage = new MeStorage(context);
        Set<Pairing> pairings = pairingStorage.loadAll();
        bluetoothTransport = BluetoothTransport.init(context);
        for (Pairing p : pairings) {
            activePairingsByUUID.put(p.uuid, p);
            if (bluetoothTransport != null) {
                bluetoothTransport.add(p);
            }
        }

        try {
             responseDiskCacheByRequestID = DiskLruCache.open(context.getCacheDir(), 0, 1, 2 << 19);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dbHelper = OpenHelperManager.getHelper(context, OpenDatabaseHelper.class);
    }

    public static synchronized Silo shared(Context context) {
        if (singleton == null) {
            singleton = new Silo(context);
        }
        return singleton;
    }

    public boolean hasActivity(Pairing pairing) {
        return lastRequestTimeSeconds.get(pairing) != null;
    }

    public Pairings pairings() {
        return pairingStorage;
    }

    public MeStorage meStorage() {
        return meStorage;
    }

    public void start() {
        synchronized (pairingsLock) {
            for (Pairing pairing : activePairingsByUUID.values()) {
                Log.i(TAG, "starting "+ Base64.encodeAsString(pairing.workstationPublicKey));
                SQSPoller poller = pollers.remove(pairing);
                if (poller != null) {
                    poller.stop();
                }
                pollers.put(pairing, new SQSPoller(context, pairing));
            }
        }
    }

    public void stop() {
        Log.i(TAG, "stopping");
        synchronized (pairingsLock) {
            for (SQSPoller poller: pollers.values()) {
                poller.stop();
            }
            pollers.clear();
        }
    }

    public synchronized void exit() {
        bluetoothTransport.stop();
        if (responseDiskCacheByRequestID != null && responseDiskCacheByRequestID.isClosed()) {
            try {
                responseDiskCacheByRequestID.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Pairing pair(Pairing pairing) throws CryptoException, TransportException {
        synchronized (pairingsLock) {
            Pairing oldPairing = activePairingsByUUID.get(pairing.uuid);
            if (oldPairing != null) {
                Log.w(TAG, "already paired with " + pairing.workstationName);
                return oldPairing;
            }
            byte[] wrappedKey = pairing.wrapKey();
            NetworkMessage wrappedKeyMessage = new NetworkMessage(NetworkMessage.Header.WRAPPED_PUBLIC_KEY, wrappedKey);
            send(pairing, wrappedKeyMessage);

            pairingStorage.pair(pairing);
            activePairingsByUUID.put(pairing.uuid, pairing);
            pollers.put(pairing, new SQSPoller(context, pairing));
            if (bluetoothTransport != null) {
                bluetoothTransport.add(pairing);
                bluetoothTransport.send(pairing, wrappedKeyMessage);
            }
        }
        return pairing;
    }

    public void unpair(Pairing pairing, boolean sendResponse) {
        if (sendResponse) {
            Response unpairResponse = new Response();
            unpairResponse.requestID = "";
            unpairResponse.unpairResponse = new UnpairResponse();
            try {
                send(pairing, unpairResponse);
            } catch (CryptoException | TransportException e) {
                e.printStackTrace();
            }
        }
        synchronized (pairingsLock) {
            pairingStorage.unpair(pairing);
            activePairingsByUUID.remove(pairing.uuid);
            SQSPoller poller = pollers.remove(pairing);
            if (poller != null) {
                poller.stop();
            }
            bluetoothTransport.remove(pairing);
        }
    }

    public void unpairAll() {
        synchronized (pairingsLock) {
            List<Pairing> toDelete = new ArrayList<>(activePairingsByUUID.values());
            for (Pairing pairing: toDelete) {
                unpair(pairing, true);
            }
        }
    }

    public void onMessage(UUID pairingUUID, byte[] incoming, String communicationMedium) {
        try {
            NetworkMessage message = NetworkMessage.parse(incoming);
            Pairing pairing;
            synchronized (pairingsLock) {
                pairing = activePairingsByUUID.get(pairingUUID);
            }
            if (pairing == null) {
                Log.e(TAG, "not valid pairing: " + pairingUUID);
                return;
            }
            switch (message.header) {
                case CIPHERTEXT:
                    byte[] json = pairing.unseal(message.message);
                    Request request = JSON.fromJson(json, Request.class);
                    handle(pairing, request, communicationMedium);
                    break;
                case WRAPPED_KEY:
                    break;
                case WRAPPED_PUBLIC_KEY:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void send(Pairing pairing, Response response) throws CryptoException, TransportException {
        byte[] responseJson = JSON.toJson(response).getBytes();
        byte[] sealed = pairing.seal(responseJson);
        send(pairing, new NetworkMessage(NetworkMessage.Header.CIPHERTEXT, sealed));
    }

    private void send(final Pairing pairing, final NetworkMessage message) throws TransportException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothTransport.send(pairing, message);
                } catch (TransportException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SQSTransport.sendMessage(pairing, message);
                } catch (TransportException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private synchronized void trySetupCache() throws IOException {
        if (responseDiskCacheByRequestID == null || responseDiskCacheByRequestID.isClosed()) {
            responseDiskCacheByRequestID = DiskLruCache.open(context.getCacheDir(), 0, 1, 2 << 19);
        }
    }

    private synchronized boolean sendCachedResponseIfPresent(Pairing pairing, Request request) throws CryptoException, TransportException, IOException {
        trySetupCache();
        if (responseDiskCacheByRequestID != null && !responseDiskCacheByRequestID.isClosed()) {
            DiskLruCache.Snapshot cacheEntry = responseDiskCacheByRequestID.get(request.requestIDCacheKey(pairing));
            if (cacheEntry != null) {
                String cachedJSON = cacheEntry.getString(0);
                if (cachedJSON != null) {
                    send(pairing, JSON.fromJson(cachedJSON, Response.class));
                    Log.i(TAG, "sent cached response to " + request.requestID);
                    return true;
                } else {
                    Log.v(TAG, "no cache JSON");
                }
            } else {
                Log.v(TAG, "no cache entry");
            }
        }
        Response cachedResponse = responseMemCacheByRequestID.get(request.requestIDCacheKey(pairing));
        if (cachedResponse != null) {
            send(pairing, cachedResponse);
            Log.i(TAG, "sent memory cached response to " + request.requestID);
            return true;
        }
        return false;
    }


    private synchronized void handle(Pairing pairing, Request request, String communicationMedium) throws Exception {
        //  Allow 15 minutes of clock skew
        if (Math.abs(request.unixSeconds - (System.currentTimeMillis() / 1000)) > CLOCK_SKEW_TOLERANCE_SECONDS) {
            throw new ProtocolException("invalid request time");
        }

        if (request.body instanceof UnpairRequest) {
            unpair(pairing, false);
            new Analytics(context).postEvent("device", "unpair", "request", null, false);
        }

        if (sendCachedResponseIfPresent(pairing, request)) {
            return;
        }

        lastRequestTimeSeconds.put(pairing, System.currentTimeMillis() / 1000);

        if (Policy.isApprovedNow(context, pairing, request)) {
            respondToRequest(pairing, request, true);
        } else {
            if (Policy.requestApproval(context, pairing, request)) {
                new Analytics(context).postEvent(request.analyticsCategory(), "requires approval", communicationMedium, null, false);
            }
            if (request.sendACK != null && request.sendACK) {
                Response ackResponse = Response.with(request);
                ackResponse.ackResponse = new AckResponse();
                send(pairing, ackResponse);
            }
        }
    }

    public synchronized void respondToRequest(Pairing pairing, Request request, boolean signatureAllowed) throws Exception {
        if (sendCachedResponseIfPresent(pairing, request)) {
            return;
        }

        Response response = Response.with(request);
        Analytics analytics = new Analytics(context);
        if (analytics.isDisabled()) {
            response.trackingID = "disabled";
        } else {
            response.trackingID = analytics.getClientID();
        }

        request.body.visit(new RequestBody.Visitor<Void, Exception>() {
            @Override
            public Void visit(MeRequest meRequest) throws Exception {
                SSHKeyPairI key = KeyManager.loadMeRSAOrEdKeyPair(context);
                response.meResponse = new MeResponse(meStorage.loadWithUserID(key, meRequest.userID(), pairing));
                return null;
            }

            @Override
            public Void visit(SignRequest signRequest) throws Exception {
                signRequest.validate();
                response.signResponse = new SignResponse();
                if (signatureAllowed) {
                    try {
                        SSHKeyPairI key = KeyManager.loadMeRSAOrEdKeyPair(context);
                        if (MessageDigest.isEqual(signRequest.publicKeyFingerprint, key.publicKeyFingerprint())) {
                            if (signRequest.verifyHostName()) {
                                String hostName = signRequest.hostAuth.hostNames[0];
                                String hostKey = Base64.encodeAsString(signRequest.hostAuth.hostKey);
                                synchronized (databaseLock) {
                                    List<KnownHost> matchingKnownHosts =  dbHelper.getKnownHostDao().queryForEq("host_name", hostName);
                                    if (matchingKnownHosts.size() == 0) {
                                        dbHelper.getKnownHostDao().create(new KnownHost(hostName, hostKey, System.currentTimeMillis()/1000));
                                        broadcastKnownHostsChanged();
                                    } else {
                                        KnownHost pinnedHost = matchingKnownHosts.get(0);
                                        if (!pinnedHost.publicKey.equals(hostKey)) {
                                            throw new MismatchedHostKeyException("Expected " + pinnedHost.publicKey + " received " + hostKey);
                                        }
                                    }
                                }
                            }
                            String algo = signRequest.algo();
                            if (request.semVer().lessThan(Versions.KR_SUPPORTS_RSA_SHA256_512)) {
                                algo = "ssh-rsa";
                            }
                            response.signResponse.signature = key.signDigestAppendingPubkey(signRequest.data, algo);
                            SSHSignatureLog log = new SSHSignatureLog(
                                    signRequest.data,
                                    true,
                                    signRequest.command,
                                    signRequest.user(),
                                    signRequest.firstHostnameIfExists(),
                                    System.currentTimeMillis() / 1000,
                                    signRequest.verifyHostName(),
                                    JSON.toJson(signRequest.hostAuth),
                                    pairing.getUUIDString(),
                                    pairing.workstationName);
                            pairingStorage.appendToSSHLog(log);
                            Notifications.notifySuccess(context, pairing, request, log);
                            if (signRequest.verifiedHostNameOrDefault("unknown host").equals("me.krypt.co")) {
                                Intent sshMeIntent = new Intent(TestSSHFragment.SSH_ME_ACTION);
                                context.sendBroadcast(sshMeIntent);
                            }
                            if (signRequest.hostAuth == null) {
                                new Analytics(context).postEvent("host", "unknown", null, null, false);
                            } else if (!signRequest.verifyHostName()) {
                                new Analytics(context).postEvent("host", "unverified", null, null, false);
                            }
                        } else {
                            Log.e(TAG, Base64.encodeAsString(signRequest.publicKeyFingerprint) + " != " + Base64.encodeAsString(key.publicKeyFingerprint()));
                            response.signResponse.error = "unknown key fingerprint";
                        }
                    } catch (NoSuchAlgorithmException | SignatureException e) {
                        response.signResponse.error = "unknown error";
                        e.printStackTrace();
                    } catch (SQLException e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        response.signResponse.error = "SQL error: " + e.getMessage() + "\n" + sw.toString();
                        e.printStackTrace();
                    } catch (MismatchedHostKeyException e) {
                        response.signResponse.error = "host public key mismatched";
                        Notifications.notifyReject(context, pairing, request, "Host public key mismatched.");
                        e.printStackTrace();
                    }
                } else {
                    response.signResponse.error = "rejected";
                    pairingStorage.appendToSSHLog(new SSHSignatureLog(
                            signRequest.data,
                            false,
                            signRequest.command,
                            signRequest.user(),
                            signRequest.firstHostnameIfExists(),
                            System.currentTimeMillis() / 1000,
                            signRequest.verifyHostName(),
                            JSON.toJson(signRequest.hostAuth),
                            pairing.getUUIDString(),
                            pairing.workstationName));
                }
                return null;
            }

            @Override
            public Void visit(GitSignRequest gitSignRequest) throws Exception {
                if (signatureAllowed) {
                    try {
                        SSHKeyPairI key = KeyManager.loadMeRSAOrEdKeyPair(context);
                        new MeStorage(context).loadWithUserID(key, UserID.parse(gitSignRequest.userID), pairing);
                        co.krypt.krypton.log.Log log =
                        gitSignRequest.body.visit(new GitSignRequestBody.Visitor<co.krypt.krypton.log.Log, Exception>() {
                            @Override
                            public co.krypt.krypton.log.Log visit(CommitInfo commit) throws Exception {
                                    byte[] signature = SignableUtils.signBinaryDocument(commit, key, HashAlgorithm.SHA512);
                                    response.gitSignResponse = new GitSignResponse(
                                            signature,
                                            null
                                    );
                                    GitCommitSignatureLog commitLog = new GitCommitSignatureLog(
                                            pairing,
                                            commit,
                                            new AsciiArmor(AsciiArmor.HeaderLine.SIGNATURE, AsciiArmor.DEFAULT_HEADERS, signature).toString()
                                    );
                                    pairings().appendToCommitLogs(
                                            commitLog
                                    );
                                    return commitLog;
                            }

                            @Override
                            public co.krypt.krypton.log.Log visit(TagInfo tag) throws Exception {
                                byte[] signature = SignableUtils.signBinaryDocument(tag, key, HashAlgorithm.SHA512);
                                response.gitSignResponse = new GitSignResponse(
                                        signature,
                                        null
                                );
                                GitTagSignatureLog tagLog = new GitTagSignatureLog(
                                        pairing,
                                        tag,
                                        new AsciiArmor(AsciiArmor.HeaderLine.SIGNATURE, AsciiArmor.DEFAULT_HEADERS, signature).toString()
                                );
                                pairings().appendToTagLogs(
                                        tagLog
                                );
                                return tagLog;
                            }
                        });
                        Notifications.notifySuccess(context, pairing, request, log);
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.gitSignResponse = new GitSignResponse(null, "unknown error");
                    }
                } else {
                    response.gitSignResponse = new GitSignResponse(null, "rejected");
                    gitSignRequest.body.visit(new GitSignRequestBody.Visitor<Void, RuntimeException>() {

                        @Override
                        public Void visit(CommitInfo commit) throws RuntimeException {
                            pairings().appendToCommitLogs(
                                    new GitCommitSignatureLog(
                                            pairing,
                                            commit,
                                            null
                                    )
                            );
                            return null;
                        }

                        @Override
                        public Void visit(TagInfo tag) throws RuntimeException {
                            pairings().appendToTagLogs(
                                    new GitTagSignatureLog(
                                            pairing,
                                            tag,
                                            null
                                    )
                            );
                            return null;
                        }
                    });
                }
                return null;
            }

            @Override
            public Void visit(UnpairRequest unpairRequest) throws Exception {
                return null;
            }

            @Override
            public Void visit(HostsRequest hostsRequest) throws Exception {
                HostsResponse hostsResponse = new HostsResponse();

                synchronized (databaseLock) {
                    try {
                        List<SSHSignatureLog> sshLogs = dbHelper.getSSHSignatureLogDao().queryBuilder()
                                .groupByRaw("user, host_name").query();
                        List<UserAndHost> userAndHosts = new LinkedList<>();
                        for (SSHSignatureLog log: sshLogs) {
                            UserAndHost userAndHost = new UserAndHost();
                            userAndHost.user = log.user;
                            userAndHost.host = log.hostName;
                            userAndHosts.add(userAndHost);
                        }

                        HostInfo hostInfo = new HostInfo();
                        UserAndHost[] userAndHostsArray = new UserAndHost[userAndHosts.size()];
                        userAndHosts.toArray(userAndHostsArray);
                        hostInfo.hosts = userAndHostsArray;

                        List<UserID> userIDs = meStorage.getUserIDs();
                        List<String> userIDStrings = new LinkedList<>();
                        for (UserID userID: userIDs) {
                            userIDStrings.add(userID.toString());
                        }
                        String[] userIDArray = new String[userIDs.size()];
                        userIDStrings.toArray(userIDArray);
                        hostInfo.pgpUserIDs = userIDArray;

                        hostsResponse.hostInfo = hostInfo;

                    } catch (SQLException e1) {
                        hostsResponse.error = "sql exception";
                    }
                }

                response.hostsResponse = hostsResponse;
                return null;
            }
        });

        response.snsEndpointARN = SNSTransport.getInstance(context).getEndpointARN();

        if (responseDiskCacheByRequestID != null && !responseDiskCacheByRequestID.isClosed()) {
            DiskLruCache.Editor cacheEditor = responseDiskCacheByRequestID.edit(request.requestIDCacheKey(pairing));
            cacheEditor.set(0, JSON.toJson(response));
            cacheEditor.commit();
            responseDiskCacheByRequestID.flush();
        }
        responseMemCacheByRequestID.put(request.requestIDCacheKey(pairing), response);

        send(pairing, response);
    }

    public List<KnownHost> getKnownHosts() throws SQLException {
        synchronized (databaseLock) {
            return dbHelper.getKnownHostDao().queryForAll();
        }
    }

    public void deleteKnownHost(String hostName) throws SQLException {
        synchronized (databaseLock) {
            List<KnownHost> matchingHosts = dbHelper.getKnownHostDao().queryForEq("host_name", hostName);
            if (matchingHosts.size() == 0) {
                Log.e(TAG, "host to delete not found");
                return;
            }
            dbHelper.getKnownHostDao().delete(matchingHosts.get(0));
        }
        broadcastKnownHostsChanged();
    }

    public boolean hasKnownHost(String hostName) {
        synchronized (databaseLock) {
            List<KnownHost> matchingHosts;
            try {
                matchingHosts = dbHelper.getKnownHostDao().queryForEq("host_name", hostName);
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return matchingHosts.size() > 0;
        }
    }

    private void broadcastKnownHostsChanged() {
        Intent intent = new Intent(KNOWN_HOSTS_CHANGED_ACTION);
        context.sendBroadcast(intent);
    }

}
