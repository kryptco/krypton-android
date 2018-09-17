package co.krypt.krypton.team;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.atomic.AtomicBoolean;

import co.krypt.krypton.BuildConfig;

/**
 * Created by Kevin King on 1/24/18.
 * Copyright 2018. KryptCo, Inc.
 */
public class Native {
    private static final String TAG = "Native";
    public static AtomicBoolean linked = new AtomicBoolean(false);

    public static class NotLinked extends Exception { }

    public static synchronized native String createTeam(final String dbDir, final String teamOnboardingDataJson);

    public static synchronized native String requestEmailChallenge(final String dbDir, final String email);

    public static synchronized native String deleteDB(final String dbDir);

    public static synchronized native String getTeam(final String dbDir);

    public static synchronized native String getPolicy(final String dbDir);

    public static synchronized native String getPinnedKeysByHost(final String dbDir, final String host);

    public static synchronized native String getTeamCheckpoint(final String dbDir);

    //  JSON(NativeResult<Boolean>)
    public static synchronized native String currentTeamExists(final String dbDir);

    public static synchronized native String executeRequestableOperation(final String dbDir, final String requestableOperationJson);

    //  JSON(NativeResult<DecryptInviteOutput>)
    public static synchronized native String decryptInvite(final String dbDir, final String inviteLink);

    public static synchronized native String acceptInvite(final String dbDir, final String acceptInviteArgsJson);

    public static synchronized native String acceptDirectInvite(final String dbDir, final String acceptDirectInviteArgsJson);

    public static synchronized native String updateTeam(final String dbDir);

    public static synchronized native String generateClient(final String dbDir, final String profileJson);

    public static synchronized native String tryRead(final String dbDir);

    public static synchronized native String encryptLog(final String dbDir, final String logJson);

    public static synchronized native String formatBlocks(final String dbDir);

    public static synchronized native String formatRequestableOp(final String dbDir, final String requestableTeamOperationJson);

    public static synchronized native String subscribeToPushNotifications(final String dbDir, final String token);

    public static synchronized native String signReadToken(final String dbDir, final String readerPublicKeyB64);

    public static synchronized native String unwrapKey(final String dbDir, final String boxedMessageJson);

    public static synchronized native String requestBillingInfo(final String dbDir);

    public static synchronized native String setProd();
    public static synchronized native String setStaging();
    public static synchronized native String setDev();

    static {
        try {
            System.loadLibrary("sodium");
            System.loadLibrary("sigchain_client");
            linked.set(true);
            if (BuildConfig.DEBUG) {
                setDev();
            } else {
                setProd();
            }
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
            Crashlytics.logException(e);
        }
    }
}
