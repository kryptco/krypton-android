package co.krypt.krypton.silo;

import android.content.Context;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;


@SuppressWarnings("unused,WeakerAccess")
public class IdentityService {

    private static IdentityService instance = new IdentityService();
    private IdentityService() {
        EventBus.getDefault().register(this);
    }

    public static IdentityService instance() {
        return instance;
    }

    private final static String TAG = "IdentityService";

    public static final class GetProfile {
        private final Context context;

        public GetProfile(Context context) {
            this.context = context;
        }
    }

    public static final class GetProfileResult {
        @Nullable
        public final Profile profile;

        public GetProfileResult(@Nullable Profile profile) {
            this.profile = profile;
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void getProfile(GetProfile request) {
        EventBus.getDefault().postSticky(new GetProfileResult(new MeStorage(request.context).load()));
    }

    public static final class AccountsUpdated {}

}
