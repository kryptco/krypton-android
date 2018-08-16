package co.krypt.krypton.team;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.silo.Notifications;
import co.krypt.krypton.team.billing.Billing;


@SuppressWarnings("unused,WeakerAccess")
public class TeamService {
    private final static String TAG = "TeamService";

    public interface ResponseHandler<R> {
        void onResponse(R r);
    }
    /*
        Wrapper for Context and an optional callback to pass the response (of type R) to.
     */
    public static final class C<R> {

        /*
          When passed an activity, operation status indicator is rendered to it.
          If confirm is true, an approval dialog will be shown before executing the operation.
         */
        @android.support.annotation.Nullable
        public final Activity a;
        public final boolean confirm;
        public final Context c;

        /*
          Optional callback to be run on the main thread
           */
        @android.support.annotation.Nullable
        public final ResponseHandler<R> r;

        /*
        No confirmation, status UI, or callback
         */
        public static <R> C<R> background(Context c) {
            return new C<>(c, null, false);
        }

        public static <R> C<R> withCallback(Context c, ResponseHandler<R> r) {
            return new C<>(c, r, false);
        }

        public static <R> C<R> withConfirmStatus(Activity a) {
            return new C<>(a, null, true);
        }

        public static <R> C<R> withStatus(Activity a) {
            return new C<>(a, null, false);
        }

        public static <R> C<R> withStatusCallback(Activity a, ResponseHandler<R> r) {
            return new C<>(a, r, false);
        }

        public static <R> C<R> withConfirmStatusCallback(Activity a, ResponseHandler<R> r) {
            return new C<>(a, r, true);
        }

        private C(Context c, ResponseHandler<R> r, boolean confirm) {
            this.a = null;
            this.c = c;
            this.r = r;
            this.confirm = confirm;
        }

        private C(Activity a, ResponseHandler<R> r, boolean confirm) {
            this.a = a;
            this.c = a;
            this.r = r;
            this.confirm = confirm;
        }

        public void dispatchResponse(R response) {
            if (r != null) {
                new Handler(Looper.getMainLooper()).post(() -> r.onResponse(response));
            }
        }
    }

    public static final class UpdateTeamHomeData {
        private final Context context;

        public UpdateTeamHomeData(Context context) {
            this.context = context;
        }
    }

    public static final class UpdateTeamHomeDataWithNotification {
        private final Context context;

        public UpdateTeamHomeDataWithNotification(Context context) {
            this.context = context;
        }
    }

    public static final class UpdateTeamHomeDataIfOutdated {
        private final Context context;

        public UpdateTeamHomeDataIfOutdated(Context context) {
            this.context = context;
        }
    }

    public static final class UpdateTeamHomeDataResult {
        public final Sigchain.NativeResult<Sigchain.TeamHomeData> r;

        public UpdateTeamHomeDataResult(Sigchain.NativeResult<Sigchain.TeamHomeData> r) {
            this.r = r;
        }
    }

    public static final class RequestTeamOperation {
        public final Sigchain.RequestableTeamOperation op;
        public final C<Sigchain.NativeResult<Sigchain.TeamOperationResponse>> c;

        public RequestTeamOperation(Sigchain.RequestableTeamOperation op, C<Sigchain.NativeResult<Sigchain.TeamOperationResponse>> c) {
            this.op = op;
            this.c = c;
        }
    }

    public static final class RequestTeamOperationResult {
        public final Sigchain.NativeResult<Sigchain.TeamOperationResponse> r;

        public RequestTeamOperationResult(Sigchain.NativeResult<Sigchain.TeamOperationResponse> r) {
            this.r = r;
        }
    }

    private TeamService() {
            EventBus.getDefault().register(this);
            Log.i(TAG, "registered");
    }

    public static TeamService instance() {
        synchronized (TeamService.class) {
            if (instance == null) {
                instance = new TeamService();
            }
            return instance;
        }
    }
    private static TeamService instance = new TeamService();

    private static AtomicLong lastTeamUpdateSeconds = new AtomicLong(0);

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateTeamHomeData(UpdateTeamHomeData u) {
        lastTeamUpdateSeconds.set(System.currentTimeMillis()/1000);
        Log.i(TAG, "updating teamHomeData");
        Sigchain.NativeResult<Sigchain.TeamHomeData> data = null;
        try {
            data = TeamDataProvider.refreshTeamHomeData(u.context);
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
            return;
        }

        EventBus.getDefault().postSticky(new UpdateTeamHomeDataResult(data));
        EventBus.getDefault().postSticky(new GetTeamHomeDataResult(data));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateTeamHomeData(UpdateTeamHomeDataWithNotification u) {
        lastTeamUpdateSeconds.set(System.currentTimeMillis()/1000);
        Log.i(TAG, "updating teamHomeData with notification");
        try {
            Sigchain.NativeResult<Sigchain.UpdateTeamOutput> data = TeamDataProvider.updateTeam(u.context);

            Sigchain.NativeResult<Sigchain.TeamHomeData> teamHomeData = TeamDataProvider.getTeamHomeData(u.context);

            if (data.success != null && data.success.lastFormattedBlock != null && teamHomeData.success != null) {
                if (teamHomeData.success.is_admin) {
                    Notifications.notifyTeamUpdate(u.context, teamHomeData.success.name, data.success.lastFormattedBlock);
                }
            }

            EventBus.getDefault().postSticky(new UpdateTeamHomeDataResult(teamHomeData));
            EventBus.getDefault().postSticky(new GetTeamHomeDataResult(teamHomeData));
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateTeamHomeData(UpdateTeamHomeDataIfOutdated u) {
        if ((System.currentTimeMillis()/1000) - lastTeamUpdateSeconds.get() < 60*15) {
            return;
        }
        Log.i(TAG, "updating outdated teamHomeData");
        updateTeamHomeData(new UpdateTeamHomeDataWithNotification(u.context));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void requestOperation(RequestTeamOperation op) {
        // Present dialog and use on-screen loader if confirm true
        Runnable executeRequest = () -> {
            Log.i(TAG, "requesting team op");
            if (op.c.a != null) {
                LoaderOverlay.start(op.c.a, op.op.loadingText());
            }
            try {
                Sigchain.NativeResult<Sigchain.TeamOperationResponse> result = TeamDataProvider.requestTeamOperation(op.c.c, op.op);
                if (result.error != null) {
                    if (op.c.a != null) {
                        LoaderOverlay.error(op.c.a, result.error);
                    }
                } else {
                    LoaderOverlay.success(op.c.a, op.op.successText());
                }
                EventBus.getDefault().post(new RequestTeamOperationResult(result));
                op.c.dispatchResponse(result);
            } catch (Native.NotLinked notLinked) {
                notLinked.printStackTrace();
            }
        };

        if (op.c.confirm) {
            try {
                Sigchain.NativeResult<Sigchain.FormattedRequestableOp> formatResult = TeamDataProvider.formatRequestableOp(op.c.c, op.op);
                if (formatResult.success == null) {
                    return;
                }
                op.c.a.runOnUiThread(() -> {
                    AlertDialog alertDialog = new AlertDialog.Builder(op.c.a).create();
                    alertDialog.setTitle(formatResult.success.header);
                    alertDialog.setMessage(formatResult.success.body + "?");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                            (dialog, which) -> {
                                dialog.dismiss();
                                new Thread(executeRequest).start();
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                });
            } catch (Native.NotLinked notLinked) {
                notLinked.printStackTrace();
            }
        } else {
            executeRequest.run();
        }
    }

    public static class GetTeamHomeData {
        public final C<Sigchain.TeamHomeData> c;

        public GetTeamHomeData(C<Sigchain.TeamHomeData> c) {
            this.c = c;
        }
    }

    public static class GetTeamHomeDataResult {
        public final Sigchain.NativeResult<Sigchain.TeamHomeData> r;

        public GetTeamHomeDataResult(Sigchain.NativeResult<Sigchain.TeamHomeData> r) {
            this.r = r;
        }
    }

    /**
     * Return latest team home data without reading from the team server
     * @param r
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void getTeamHomeData(GetTeamHomeData r) {
        try {
            GetTeamHomeDataResult result = new GetTeamHomeDataResult(TeamDataProvider.getTeamHomeData(r.c.c));
            EventBus.getDefault().postSticky(result);
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    public static class GenerateClient {
        public final C<Sigchain.Identity> c;

        public final Sigchain.GenerateClientInput i;

        public GenerateClient(C<Sigchain.Identity> c, Sigchain.GenerateClientInput i) {
            this.c = c;
            this.i = i;
        }
    }

    public static class GenerateClientResult {
        public final Sigchain.NativeResult<Sigchain.Identity> c;

        public GenerateClientResult(Sigchain.NativeResult<Sigchain.Identity> c) {
            this.c = c;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void generateClient(GenerateClient r) {
        try {
            GenerateClientResult result = new GenerateClientResult(TeamDataProvider.generateClient(r.c.c, r.i));
            EventBus.getDefault().post(result);
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    public static class EncryptLog {
        co.krypt.krypton.team.log.Log log;

        public final C<Object> c;

        public EncryptLog(C<Object> c, co.krypt.krypton.team.log.Log log) {
            this.c = c;
            this.log = log;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void encryptLog(EncryptLog r) {
        if (Locale.getDefault().equals(Locale.FRANCE)) {
            return;
        }
        try {
            TeamDataProvider.encryptLog(r.c.c, r.log);
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    public static class FormatBlocks {
        public final C<Sigchain.NativeResult<List<Sigchain.FormattedBlock>>> c;

        public FormatBlocks(C<Sigchain.NativeResult<List<Sigchain.FormattedBlock>>> c) {
            this.c = c;
        }
    }

    public static class FormatBlocksResult {
        public final Sigchain.NativeResult<List<Sigchain.FormattedBlock>> r;

        public FormatBlocksResult(Sigchain.NativeResult<List<Sigchain.FormattedBlock>> r) {
            this.r = r;
        }
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void formatBlocks(FormatBlocks r) {
        try {
            EventBus.getDefault().post(new FormatBlocksResult(TeamDataProvider.formatBlocks(r.c.c)));
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    public static class RequestBillingInfo {

        public final Context c;

        public RequestBillingInfo(Context c) {
            this.c = c;
        }
    }

    public static class BillingInfoResult {
        public final Sigchain.NativeResult<Billing> r;

        public BillingInfoResult(Sigchain.NativeResult<Billing> r) {
            this.r = r;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateBillingInfo(RequestBillingInfo requestBillingInfo) {
        Log.i(TAG, "requesting BillingInfo");
        try {
            Sigchain.NativeResult<Billing> result = TeamDataProvider.requestBillingInfo(requestBillingInfo.c);

            Log.i(TAG, "billing info result: " + JSON.toJson(result));

            EventBus.getDefault().postSticky(new BillingInfoResult(result));
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

}
