package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.sql.SQLException;

import co.krypt.krypton.R;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Sigchain;

public class OnboardingPolicyFragment extends Fragment {

    private final String TAG = "TeamFragment";

    public OnboardingPolicyFragment() {
    }

    public static OnboardingPolicyFragment newInstance() {
        return new OnboardingPolicyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_approvalwindow, container, false);

        AppCompatRadioButton alwaysAsk = rootView.findViewById(R.id.alwaysAsk);
        AppCompatRadioButton oneHour = rootView.findViewById(R.id.oneHour);
        AppCompatRadioButton threeHours = rootView.findViewById(R.id.threeHours);

        alwaysAsk.setOnClickListener(v -> {
            oneHour.setChecked(false);
            threeHours.setChecked(false);
        });

        oneHour.setOnClickListener(v -> {
            alwaysAsk.setChecked(false);
            threeHours.setChecked(false);
        });

        threeHours.setOnClickListener(v -> {
            alwaysAsk.setChecked(false);
            oneHour.setChecked(false);
        });


        Button nextButton = rootView.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            long temporaryApprovalSeconds = 0;
            if (oneHour.isChecked()) {
                temporaryApprovalSeconds = 3600;
            }
            if (threeHours.isChecked()) {
                temporaryApprovalSeconds = 3600*3;
            }
            if (alwaysAsk.isChecked()) {
                temporaryApprovalSeconds = 0;
            }

            final long finalTemporaryApprovalSeconds = temporaryApprovalSeconds;
            CreateTeamProgress progress = new CreateTeamProgress(v.getContext());
            progress.updateTeamData((s, d) -> {
                d.temporaryApprovalSeconds = finalTemporaryApprovalSeconds;
                try {
                    if (Silo.shared(getContext()).getKnownHosts().size() > 0) {
                        return CreateStage.PIN_HOSTS;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                d.pinnedHosts = new Sigchain.PinnedHost[]{};
                return CreateStage.VERIFY_EMAIL;
            });

            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.fragment_teams, progress.currentStage().getFragment())
                        .commitNowAllowingStateLoss();
            }
        });
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
