package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.krypton.R;

public class OnboardingAuditLogsFragment extends Fragment {

    private final String TAG = "OnboardingAuditLogsFragment";

    public OnboardingAuditLogsFragment() {
    }

    public static OnboardingAuditLogsFragment newInstance() {
        return new OnboardingAuditLogsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_auditlogs, container, false);
        Button nextButton = rootView.findViewById(R.id.nextButton);
        SwitchCompat switch_ = rootView.findViewById(R.id.enableAuditLogsSwitch);
        nextButton.setOnClickListener(v -> {
            CreateTeamProgress progress = new CreateTeamProgress(v.getContext());
            progress.updateTeamData((s, d) -> {
                d.enableAuditLogs = switch_.isEnabled();
                return CreateStage.POLICY;
            });
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.fragment_teams, new OnboardingPolicyFragment())
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
