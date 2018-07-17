package co.krypt.krypton.onboarding;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import co.krypt.krypton.R;
import co.krypt.krypton.pairing.PairFragment;

public class FirstPairExtFragment extends Fragment {

    private final PairFragment pairFragment = new PairFragment();

    private BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                next();
            }).start();
        }
    };

    public FirstPairExtFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter pairFilter = new IntentFilter();
        pairFilter.addAction(PairFragment.PAIRING_SUCCESS_ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(pairReceiver, pairFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(pairReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        pairFragment.setUserVisibleHint(true);
    }

    @Override
    public void onPause() {
        pairFragment.setUserVisibleHint(false);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getChildFragmentManager().beginTransaction().add(R.id.pairLayout, pairFragment).commit();

        View root = inflater.inflate(R.layout.fragment_first_pair_ext, container, false);
        Button nextButton = root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> next());

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private boolean proceeding = false;

    private synchronized void next() {
        if (proceeding) {
            return;
        }
        proceeding = true;
        new OnboardingProgress(getContext()).setStage(OnboardingStage.FIRST_PAIR_CLI);
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        FirstPairCliFragment firstPairCliFragment = new FirstPairCliFragment();
        fragmentTransaction
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .hide(this).add(R.id.activity_onboarding, firstPairCliFragment).show(firstPairCliFragment).commitAllowingStateLoss();
    }
}
