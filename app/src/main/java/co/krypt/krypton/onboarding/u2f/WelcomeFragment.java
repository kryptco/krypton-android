package co.krypt.krypton.onboarding.u2f;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.krypton.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class WelcomeFragment extends Fragment {
    private static final String TAG = "WelcomeFragment";

    public WelcomeFragment() {
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_welcome, container, false);
        Button nextButton = root.findViewById(R.id.generateButton);
        nextButton.setOnClickListener(v -> next());

        return root;
    }

    private void next() {
        final FragmentActivity context = getActivity();
        final U2FOnboardingProgress progress = new U2FOnboardingProgress(getContext());
        progress.setStage(U2FOnboardingStage.FIRST_PAIR_EXT);
        final FirstPairExtFragment firstPairExtFragment = new FirstPairExtFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .replace(R.id.activity_onboarding, firstPairExtFragment).commit();
    }

}
