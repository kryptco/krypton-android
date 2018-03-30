package co.krypt.krypton.team.onboarding.create;

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

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import co.krypt.krypton.R;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;

public class OnboardingLoadTeamFragment extends Fragment {

    private final String TAG = "OnboardingLoadTeam";

    public OnboardingLoadTeamFragment() {
    }

    public static OnboardingLoadTeamFragment newInstance() {
        return new OnboardingLoadTeamFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    CreateTeamProgress progress;

    @Nullable AppCompatTextView errorText;
    ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_load_team, container, false);

        final CreateTeamProgress progress = new CreateTeamProgress(getContext());

        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            progress.reset();
            getActivity().finish();
        });

        progressBar = rootView.findViewById(R.id.progressBar);
        errorText = rootView.findViewById(R.id.errorText);
        errorText.setAlpha(0);
        AppCompatTextView progressText = rootView.findViewById(R.id.progressText);
        progressText.setText("CREATING TEAM");


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
        progress = new CreateTeamProgress(context);
        progress.updateTeamData((s, d) -> {
            if (s.equals(CreateStage.LOAD_TEAM)) {
                new Thread(() -> {
                    try {
                        Sigchain.NativeResult<JsonObject> result = TeamDataProvider.createTeam(getContext(), progress.getTeamOnboardingData());
                        Log.i(TAG, "create team result: " + JSON.toJson(result));
                        if (result.success != null) {
                            if (progress != null) {
                                progress.updateTeamData((s_, d_) -> CreateStage.COMPLETE);
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                FragmentManager fragmentManager = getFragmentManager();
                                if (fragmentManager != null) {

                                    fragmentManager.beginTransaction()
                                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                                            .replace(R.id.fragment_teams, new OnboardingCompleteFragment())
                                            .commitNowAllowingStateLoss();
                                }
                            });
                        } else {
                            if (errorText != null) {
                                errorText.post(() -> {
                                    progressBar.setIndeterminate(false);
                                    progressBar.setProgress(0);
                                    errorText.setText("Error creating team: " + result.error);
                                    errorText.setAlpha(1f);
                                });
                            }
                        }
                    } catch (Native.NotLinked notLinked) {
                        notLinked.printStackTrace();
                    }
                }).start();
            }
            return CreateStage.TEAM_CREATING;
        });
    }

    @Override
    public void onDetach() {
        progress = null;
        super.onDetach();
    }
}
