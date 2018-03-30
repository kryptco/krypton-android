package co.krypt.krypton.team.invite.inperson.member;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.team.onboarding.join.JoinStage;
import co.krypt.krypton.team.onboarding.join.JoinTeamProgress;
import co.krypt.krypton.uiutils.Error;

public class MemberVerifyEmail extends Fragment {

    private final String TAG = "MemberInpersonVerifyEm";

    public MemberVerifyEmail() {
    }

    public static MemberVerifyEmail newInstance() {
        return new MemberVerifyEmail();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_invite_inperson_member_verify_email, container, false);

        MemberQR.QRPayloads qrPayloads = MemberQR.qrPayloads.get();
        if (qrPayloads == null) {
            Error.shortToast(getContext(), "No QRcode payloads found.");
            getFragmentManager().popBackStack();
            return rootView;
        }

        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            getActivity().finish();
        });

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setAlpha(0);

        Button resendButton = rootView.findViewById(R.id.resendButton);
        AppCompatTextView checkYourEmailText = rootView.findViewById(R.id.checkYourEmailText);
        AppCompatTextView checkYourEmailSubtext = rootView.findViewById(R.id.checkYourEmailSubtext);

        Runnable onSubmitUI = () -> {
            progressBar.animate().alpha(1).setDuration(1000).start();
            checkYourEmailSubtext.animate().alpha(0).setDuration(500).start();
            checkYourEmailText.animate().alpha(0).setDuration(500).start();
        };
        Runnable onSuccessUI = () -> {
            progressBar.animate().alpha(0).setDuration(500).start();
            resendButton.animate().alpha(1).setDuration(1000).start();
            resendButton.setEnabled(true);
            checkYourEmailSubtext.animate().alpha(1).setDuration(500).start();
            checkYourEmailText.animate().alpha(1).setDuration(500).start();
        };

        Runnable onErrorUI = () -> {
            Toast.makeText(getContext(), "Error sending email verification.", Toast.LENGTH_SHORT).show();
            progressBar.animate().alpha(0).setDuration(500).start();
        };

        resendButton.setAlpha(0);
        checkYourEmailSubtext.setAlpha(0);
        checkYourEmailText.setAlpha(0);
        resendButton.setEnabled(false);

        AppCompatTextView teamName = rootView.findViewById(R.id.teamName);
        teamName.setText(qrPayloads.admin.teamName);

        AppCompatTextView emailText = rootView.findViewById(R.id.profileEmail);
        emailText.setText(qrPayloads.member.email);

        Runnable requestEmailChallenge = () -> {
            onSubmitUI.run();

            new Thread(() -> {
                Log.i(TAG, "requesting email challenge...");
                try {
                    final Sigchain.NativeResult<JsonObject> result = TeamDataProvider.requestEmailChallenge(getContext(), qrPayloads.member.email);
                    emailText.post(() -> {
                        if (result.success != null) {
                            Log.i(TAG, "request email challenge success");
                            onSuccessUI.run();
                            new JoinTeamProgress(getContext()).updateTeamData((s, d) -> JoinStage.INPERSON_CHALLENGE_EMAIL_SENT);
                        } else {
                            Log.i(TAG, "request email challenge failure");
                            onErrorUI.run();
                            resendButton.animate().alpha(1).setDuration(1000).start();
                            resendButton.setEnabled(true);
                        }
                    });
                } catch (Native.NotLinked notLinked) {
                    notLinked.printStackTrace();
                }
            }).start();
        };

        resendButton.setOnClickListener(v -> {
            requestEmailChallenge.run();
        });

        requestEmailChallenge.run();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
