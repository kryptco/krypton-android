package co.krypt.krypton.team.onboarding.create;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.krypton.R;

public class OnboardingCreateFragment extends Fragment {

    private final String TAG = "OnboardingCreateFragment";

    public OnboardingCreateFragment() {
    }

    public static OnboardingCreateFragment newInstance() {
        return new OnboardingCreateFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_create, container, false);
        Button createTeamButton = rootView.findViewById(R.id.createTeamOBButton);
        createTeamButton.setEnabled(false);
        AppCompatEditText teamNameEditText = rootView.findViewById(R.id.teamName);
        teamNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() == 0) {
                    createTeamButton.setEnabled(false);
                } else {
                    createTeamButton.setEnabled(true);
                }
            }
        });
        createTeamButton.setOnClickListener(v -> {
            CreateTeamProgress progress = new CreateTeamProgress(v.getContext());
            progress.updateTeamData((s, d) -> {
                d.name = teamNameEditText.getText().toString();
                return CreateStage.AUDIT_LOGS;
            });
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                        .replace(R.id.fragment_teams, new OnboardingAuditLogsFragment())
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
