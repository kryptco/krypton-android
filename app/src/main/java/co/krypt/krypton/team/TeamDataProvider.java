package co.krypt.krypton.team;

import android.content.Context;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.settings.Settings;
import co.krypt.krypton.team.billing.Billing;
import co.krypt.krypton.team.onboarding.create.CreateTeamData;
import co.krypt.krypton.uiutils.Error;

/**
 * Created by Kevin King on 1/22/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class TeamDataProvider {
    public final static String TAG = "TeamDataProvider";

    private static void checkSigchainNativeCodeAvailable(Context context) throws Native.NotLinked {
        if (!Native.linked.get()) {
            throw new Native.NotLinked();
        }
    }

    public static boolean shouldShowTeamsTab(Context context)  {
        Profile profile = new MeStorage(context).load();
        Settings settings = new Settings(context);
        return settings.developerMode() && (profile != null) && (profile.teamCheckpoint != null) && Native.linked.get();
    }

    public static void deleteDB(Context context) throws Native.NotLinked {
        tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.deleteDB(context.getApplicationContext().getFilesDir().getAbsolutePath()));
    }

    public static Sigchain.NativeResult<Sigchain.TeamHomeData> getTeamHomeData(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.TeamHomeData>>(){},
                () -> Native.getTeam(context.getApplicationContext().getFilesDir().getAbsolutePath()));
    }

    public static Sigchain.NativeResult<Sigchain.TeamCheckpoint> getTeamCheckpoint(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.TeamCheckpoint>>(){},
                () -> Native.getTeamCheckpoint(context.getApplicationContext().getFilesDir().getAbsolutePath()));
    }

    public static Sigchain.NativeResult<Sigchain.UpdateTeamOutput> updateTeam(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.UpdateTeamOutput>>(){},
                () -> Native.updateTeam(context.getApplicationContext().getFilesDir().getAbsolutePath()));
    }

    public static Sigchain.NativeResult<Sigchain.TeamHomeData> refreshTeamHomeData(Context context) throws Native.NotLinked {
        Sigchain.NativeResult<JsonObject> updateResult = tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.updateTeam(context.getApplicationContext().getFilesDir().getAbsolutePath()));

        if (updateResult.error != null) {
            Log.e(TAG, "UpdateTeam error: " + updateResult.error);
            return new Sigchain.NativeResult<>(updateResult.error);
        }

        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.TeamHomeData>>(){},
                () -> Native.getTeam(context.getApplicationContext().getFilesDir().getAbsolutePath()));
    }

    public static Sigchain.NativeResult<Sigchain.TeamOperationResponse> requestTeamOperation(Context context, Sigchain.RequestableTeamOperation op) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.TeamOperationResponse>>(){},
                () -> Native.executeRequestableOperation(context.getFilesDir().getAbsolutePath(), JSON.toJson(op))
        );
    }

    public static Sigchain.NativeResult<Sigchain.Policy> getPolicy(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.Policy>>(){},
                () -> Native.getPolicy(context.getApplicationContext().getFilesDir().getAbsolutePath())
        );
    }

    public static Sigchain.NativeResult<List<Sigchain.PinnedHost>> getPinnedKeysByHost(Context context, String host) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<List<Sigchain.PinnedHost>>>(){},
                () -> Native.getPinnedKeysByHost(context.getApplicationContext().getFilesDir().getAbsolutePath(), host)
        );
    }

    public static Sigchain.NativeResult<JsonObject> requestEmailChallenge(Context context, String email) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.requestEmailChallenge(
                        context.getFilesDir().getAbsolutePath(),
                        email
                )
        );
    }

    public static Sigchain.NativeResult<JsonObject> createTeam(Context context, CreateTeamData data) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.createTeam(context.getFilesDir().getAbsolutePath(),
                        JSON.toJson(data)
                ));
    }

    public static Sigchain.NativeResult<JsonObject> acceptInvite(Context context, Sigchain.AcceptInviteArgs args) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.acceptInvite(context.getFilesDir().getAbsolutePath(),
                        JSON.toJson(args)
                ));
    }

    public static Sigchain.NativeResult<JsonObject> acceptDirectInvite(Context context, Sigchain.AcceptDirectInviteArgs args) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>() {},
                () -> Native.acceptDirectInvite(context.getFilesDir().getAbsolutePath(),
                        JSON.toJson(args)
                ));
    }

    public static Sigchain.NativeResult<Sigchain.Identity> generateClient(Context context, Sigchain.GenerateClientInput input) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Sigchain.Identity>>(){},
                () -> Native.generateClient(
                        context.getApplicationContext().getFilesDir().getAbsolutePath(),
                        JSON.toJson(input)
                )
        );
    }

    public static Sigchain.NativeResult<JsonObject> tryRead(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.tryRead(
                        context.getApplicationContext().getFilesDir().getAbsolutePath()
                )
        );
    }

    public static Sigchain.NativeResult<JsonObject> encryptLog(Context context, co.krypt.krypton.team.log.Log log) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.encryptLog(
                        context.getApplicationContext().getFilesDir().getAbsolutePath(),
                        JSON.toJson(log)
                )
        );
    }

    public static Sigchain.NativeResult<List<Sigchain.FormattedBlock>> formatBlocks(Context context) throws Native.NotLinked {
        return tryNativeCall(context,
                new TypeToken<Sigchain.NativeResult<List<Sigchain.FormattedBlock>>>(){},
                () -> Native.formatBlocks(context.getApplicationContext().getFilesDir().getAbsolutePath())
        );
    }

    public static Sigchain.NativeResult<Sigchain.FormattedRequestableOp> formatRequestableOp(Context context, Sigchain.RequestableTeamOperation op) throws Native.NotLinked {
        return tryNativeCall(context,
                new TypeToken<Sigchain.NativeResult<Sigchain.FormattedRequestableOp>>(){},
                () -> Native.formatRequestableOp(
                        context.getApplicationContext().getFilesDir().getAbsolutePath(),
                        JSON.toJson(op)
                )
        );
    }

    public static Sigchain.NativeResult<JsonObject> subscribeToPushNotifications(Context context, String token) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.subscribeToPushNotifications(
                        context.getApplicationContext().getFilesDir().getAbsolutePath(), token)
        );
    }

    public static Sigchain.NativeResult<JsonObject> signReadToken(Context context, byte[] readerPublicKey) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.signReadToken( context.getApplicationContext().getFilesDir().getAbsolutePath(), Base64.encode(readerPublicKey))
        );
    }

    public static Sigchain.NativeResult<JsonObject> unwrapKey(Context context, JsonObject wrappedKey) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<JsonObject>>(){},
                () -> Native.unwrapKey(
                        context.getApplicationContext().getFilesDir().getAbsolutePath(),
                        JSON.toJson(wrappedKey)
                ));
    }

    public static Sigchain.NativeResult<Billing> requestBillingInfo(Context context) throws Native.NotLinked {
        return tryNativeCall(context, new TypeToken<Sigchain.NativeResult<Billing>>(){},
                () -> Native.requestBillingInfo(context.getApplicationContext().getFilesDir().getAbsolutePath()
                ));
    }

    private interface NativeCall {
        String call();
    }

    private static final AtomicLong lastLinkingWarningMillis = new AtomicLong(0);

    private static <T> T tryNativeCall(Context context, TypeToken t, NativeCall c) throws Native.NotLinked {
        try {
            checkSigchainNativeCodeAvailable(context);
            return JSON.fromJson(c.call(), t.getType());
        } catch (UnsatisfiedLinkError | JsonSyntaxException e) {
            long now = System.currentTimeMillis();
            if (now - lastLinkingWarningMillis.get() > 60 * 60 * 1000) {
                lastLinkingWarningMillis.set(now);
                Error.longToast(context, "Teams is not yet available on this phone. Please report your phone model to support@krypt.co.");
            }
            FirebaseCrash.report(e);
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            throw new Native.NotLinked();
        }
    }
}
