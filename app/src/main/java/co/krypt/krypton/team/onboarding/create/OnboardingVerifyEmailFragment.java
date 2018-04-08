package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.gson.JsonObject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.atomic.AtomicBoolean;

import co.krypt.krypton.R;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.uiutils.Error;

public class OnboardingVerifyEmailFragment extends Fragment {

    private final String TAG = "OnboardingVerifyEmail";

    private AppCompatEditText emailEditText;

    private ProgressBar progressBar;
    private Button cancelButton;
    private Button changeButton;
    private Button resendButton;
    ConstraintLayout checkYourEmailContainer;

    public OnboardingVerifyEmailFragment() {
    }

    public static OnboardingVerifyEmailFragment newInstance() {
        return new OnboardingVerifyEmailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_verify_email, container, false);
        cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            new CreateTeamProgress(v.getContext()).reset();
            getActivity().finish();
        });

        progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setAlpha(0);

        changeButton = rootView.findViewById(R.id.changeButton);
        changeButton.setEnabled(false);
        resendButton = rootView.findViewById(R.id.resendButton);
        resendButton.setEnabled(false);
        checkYourEmailContainer = rootView.findViewById(R.id.checkYourEmailContainer);
        checkYourEmailContainer.setAlpha(0);

        AppCompatTextView teamName = rootView.findViewById(R.id.teamName);
        teamName.setText(new CreateTeamProgress(getContext()).getTeamOnboardingData().name);

        emailEditText = rootView.findViewById(R.id.profileEmail);

        emailEditText.setOnEditorActionListener((v, i, ev) -> {
            EventBus.getDefault().post(new RequestEmailChallenge());
            return false;
        });
        emailEditText.setOnFocusChangeListener((view, b) -> {
            if (view.isEnabled() && !b) {
                EventBus.getDefault().post(new RequestEmailChallenge());
            }
        });

        resendButton.setOnClickListener(v -> {
            EventBus.getDefault().post(new RequestEmailChallenge());
        });

        changeButton.setOnClickListener(v -> {
            EventBus.getDefault().post(new FocusEmail());
        });

        Button submitButton = rootView.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);
        submitButton.setAlpha(0);
        submitButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.fragment_teams, new OnboardingLoadTeamFragment())
                        .commitNowAllowingStateLoss();
            }
        });

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new IdentityService.GetProfile(getContext()));

        EventBus.getDefault().post(new FocusEmail());
        return rootView;
    }

    private AtomicBoolean updatedInitialEmail = new AtomicBoolean(false);
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateInitialEmail(IdentityService.GetProfileResult r) {
        if (emailEditText.getText().length() == 0 && r.profile != null && !updatedInitialEmail.getAndSet(true)) {
            emailEditText.setText(r.profile.email);
        }
    }

    private static class FocusEmail {}
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void focusEmail(FocusEmail focusEmail) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        emailEditText.setEnabled(true);
        emailEditText.requestFocus();
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        progressBar.animate().alpha(0).setDuration(500).start();
    }

    private static class RequestEmailChallenge {}
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestEmailChallenge(RequestEmailChallenge requestEmailChallenge) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        emailEditText.setEnabled(false);
        emailEditText.clearFocus();
        if (imm != null) {
            imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);
        }
        progressBar.animate().alpha(1).setDuration(1000).start();

        final String email = emailEditText.getEditableText().toString();

        new Thread(() -> {
            MeStorage meStorage = Silo.shared(getContext()).meStorage();
            try {
                meStorage.setEmail(email);
            } catch (CryptoException e) {
                Error.shortToast(getContext(), "Failed to set email: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            Profile me = meStorage.load();
            new CreateTeamProgress(getContext()).updateTeamData((s, d) -> {
                if (s.equals(CreateStage.VERIFY_EMAIL) || s.equals(CreateStage.EMAIL_CHALLENGE_SENT)) {
                    d.creatorProfile = me;
                    Log.i(TAG, "requesting email challenge...");
                    try {
                        final Sigchain.NativeResult<JsonObject> result = TeamDataProvider.requestEmailChallenge(getContext(), email);
                        emailEditText.post(() -> {
                            if (result.success != null) {
                                Log.i(TAG, "request email challenge success");
                                progressBar.animate().alpha(0).setDuration(500).start();
                                checkYourEmailContainer.animate().alpha(1).setDuration(1000).start();
                                changeButton.setEnabled(true);
                                resendButton.setEnabled(true);
                            } else {
                                Log.i(TAG, "request email challenge failure: " + result.error);
                                Error.shortToast(getContext(), "Error sending email: " + result.error);
                                EventBus.getDefault().post(new FocusEmail());
                            }
                        });
                        if (result.success != null) {
                            return CreateStage.EMAIL_CHALLENGE_SENT;
                        } else {
                            return s;
                        }
                    } catch (Native.NotLinked notLinked) {
                        notLinked.printStackTrace();
                    }
                }
                return s;
            });
        }).start();

    }

    @Override
    public void onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        updatedInitialEmail.set(false);
        super.onDestroyView();
    }
}
