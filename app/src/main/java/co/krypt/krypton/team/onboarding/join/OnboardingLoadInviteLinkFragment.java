package co.krypt.krypton.team.onboarding.join;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.gson.reflect.TypeToken;

import javax.annotation.Nullable;

import co.krypt.krypton.R;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.uiutils.Error;

public class OnboardingLoadInviteLinkFragment extends Fragment {

    private final String TAG = "OnboardingLoadInvite";

    public OnboardingLoadInviteLinkFragment() {
    }

    public static OnboardingLoadInviteLinkFragment newInstance() {
        return new OnboardingLoadInviteLinkFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    JoinTeamProgress progress;

    @Nullable AppCompatTextView errorText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_load_team, container, false);

        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            progress.resetAndDeleteTeam(v.getContext());
            getActivity().finish();
        });

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        errorText = rootView.findViewById(R.id.errorText);
        errorText.setAlpha(0);
        AppCompatTextView progressText = rootView.findViewById(R.id.progressText);
        progressText.setText("LOADING TEAM INVITE");


        return rootView;
    }

    @Override
    public void onDestroyView() {
        errorText = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        progress = new JoinTeamProgress(context);
        new Thread(() -> {
            progress.updateTeamData((s, d) -> {
                if (s.equals(JoinStage.DECRYPT_INVITE)) {
                    Sigchain.NativeResult<Sigchain.DecryptInviteOutput> decryptedInvite =
                            JSON.fromJson(Native.decryptInvite(getContext().getFilesDir().getAbsolutePath(), d.inviteLink),
                                    new TypeToken<Sigchain.NativeResult<Sigchain.DecryptInviteOutput>>(){}.getType());
                    if (decryptedInvite.success != null) {
                        d.decryptInviteOutput = decryptedInvite.success;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            FragmentManager fragmentManager = getFragmentManager();
                            if (fragmentManager != null) {

                                fragmentManager.beginTransaction()
                                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                                        .replace(R.id.fragment_teams, new VerifyEmailFragment())
                                        .commit();
                            } else {
                                Log.e(TAG, "fragmentManager null");
                            }
                        });
                        return JoinStage.VERIFY_EMAIL;
                    } else {
                        Error.shortToast(getContext(), "Failed to load invitation. Please ask your team admin for a new invite.");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            getActivity().finish();
                        });
                        return JoinStage.NONE;
                    }
                } else {
                    return s;
                }
            });
        }).start();
    }

    @Override
    public void onDetach() {
        progress = null;
        super.onDetach();
    }
}
