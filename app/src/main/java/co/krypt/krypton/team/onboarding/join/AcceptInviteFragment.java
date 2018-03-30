package co.krypt.krypton.team.onboarding.join;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.annotation.Nullable;

import co.krypt.krypton.R;
import co.krypt.krypton.crypto.Base64;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.uiutils.Transitions;

public class AcceptInviteFragment extends Fragment {

    private final String TAG = "OnboardingLoadTeam";

    public AcceptInviteFragment() {
    }

    public static AcceptInviteFragment newInstance() {
        return new AcceptInviteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    JoinTeamProgress progress;

    @Nullable AppCompatTextView errorText;
    @Nullable ProgressBar progressBar;
    @Nullable AppCompatTextView progressText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_load_team, container, false);

        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            progress.resetAndDeleteTeam(v.getContext());
            getActivity().finish();
        });

        progressBar = rootView.findViewById(R.id.progressBar);
        errorText = rootView.findViewById(R.id.errorText);
        errorText.setAlpha(0);
        progressText = rootView.findViewById(R.id.progressText);
        progressText.setText("JOINING TEAM");

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new AcceptInvite());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        errorText = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        progress = new JoinTeamProgress(context);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        progress = null;
        super.onDetach();
    }

    private static class AcceptInvite {}

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void acceptInvite(AcceptInvite _) {
        progress.updateTeamData((s, d) -> {
            byte[] challengeNonceBytes = null;
            if (d.emailChallengeNonce == null) {
                EventBus.getDefault().post(new Fail("No email verification found, please join team again."));
                return s;
            }
            try {
                challengeNonceBytes = Base64.decodeURLSafe(d.emailChallengeNonce);
            } catch (CryptoException e) {
                EventBus.getDefault().post(new Fail("invalid email verification link"));
                e.printStackTrace();
                return s;
            }

            try {
                Sigchain.NativeResult<JsonObject> result = null;
                if (s.equals(JoinStage.CHALLENGE_EMAIL_SENT)) {
                    result = TeamDataProvider.acceptInvite(
                            getContext(),
                            new Sigchain.AcceptInviteArgs(
                                    d.decryptInviteOutput.indirectInvitationSecret,
                                    challengeNonceBytes,
                                    d.profile
                            ));
                    s = JoinStage.ACCEPT_INVITE;
                } else if (s.equals(JoinStage.INPERSON_CHALLENGE_EMAIL_SENT)) {
                    result = TeamDataProvider.acceptDirectInvite(
                            getContext(),
                            new Sigchain.AcceptDirectInviteArgs(
                                    d.identity,
                                    challengeNonceBytes
                            ));
                    s = JoinStage.INPERSON_ACCEPT_INVITE;
                }

                if (result == null) {
                    EventBus.getDefault().post(new Fail("please try again"));
                    return s;
                }
                if (result.success != null) {
                    EventBus.getDefault().post(new Success());
                    if (progress != null) {
                        s = JoinStage.COMPLETE;
                    }
                } else {
                    EventBus.getDefault().post(new Fail(result.error));
                }
            } catch (Native.NotLinked notLinked) {
                notLinked.printStackTrace();
            }
            return s;
        });
    }

    private static class Success {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSuccess(Success _) {
        Transitions.beginSlide(this)
                .replace(R.id.fragment_teams, new JoinCompleteFragment())
                .commitAllowingStateLoss();
    }

    private static class Fail {
        public final String error;

        private Fail(String error) {
            this.error = error;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFail(Fail f) {
        errorText.setText("Failed to accept invite: " + f.error);
        errorText.animate().alpha(1f).setDuration(500).start();
        progressBar.animate().alpha(0f).setDuration(500).start();
        progressText.animate().alpha(0f).setDuration(500).start();
    }
}
