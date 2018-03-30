package co.krypt.krypton.team;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.krypton.R;
import co.krypt.krypton.team.invite.inperson.member.MemberScan;
import co.krypt.krypton.team.onboarding.TeamOnboardingActivity;
import co.krypt.krypton.team.onboarding.create.CreateTeamProgress;
import co.krypt.krypton.team.onboarding.join.JoinTeamProgress;
import co.krypt.krypton.uiutils.Transitions;

public class IntroFragment extends Fragment {
    /**
     * Onboarding order:
     *  Team name
     *  Audit logs
     *  Policy
     *  Pin hosts
     *  Verify email
     *  Load team
     *  Complete
     */
    /*
        Accept Invite Order:
        Load Invite (decrypt)
        Verify your email (with restriction), click Join
        Joining (Load team screen)
        Complete (click Done)
     */

    private final String TAG = "IntroFragment";

    public IntroFragment() {
    }

    public static IntroFragment newInstance() {
        return new IntroFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_intro, container, false);
        Button createTeamButton = rootView.findViewById(R.id.createTeamOBButton);
        createTeamButton.setOnClickListener(v -> {
            new CreateTeamProgress(getContext()).reset();
            new JoinTeamProgress(getContext()).reset();
            startActivity(new Intent(v.getContext(), TeamOnboardingActivity.class));
        });
        Button joinTeamButton = rootView.findViewById(R.id.joinTeamButton);
        joinTeamButton.setOnClickListener(b -> {
            new JoinTeamProgress(getContext()).reset();
            new CreateTeamProgress(getContext()).reset();
            Transitions.beginSlide(this)
                    .replace(R.id.fragmentOverlay, new MemberScan())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        return rootView;
    }
}
