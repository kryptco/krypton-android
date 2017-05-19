package co.krypt.kryptonite.onboarding;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.pairing.PairFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class FirstPairFragment extends Fragment {


    private final PairFragment pairFragment = new PairFragment();

    private BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    next(intent.getStringExtra("deviceName"));
                }
            }).start();
        }
    };

    public FirstPairFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter pairFilter = new IntentFilter();
        pairFilter.addAction(PairFragment.PAIRING_SUCCESS_ACTION);
        context.registerReceiver(pairReceiver, pairFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getContext().unregisterReceiver(pairReceiver);
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

        View root = inflater.inflate(R.layout.fragment_first_pair, container, false);
        Button nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skip();
            }
        });

        TabHost host = (TabHost) root.findViewById(R.id.installInstructions);
        host.setup();

        TabHost.TabSpec spec = host.newTabSpec("curl");
        spec.setContent(R.id.tab1);
        spec.setIndicator("curl");
        host.addTab(spec);

        TabHost.TabSpec specBrew = host.newTabSpec("brew");
        specBrew.setContent(R.id.tab2);
        specBrew.setIndicator("brew");
        host.addTab(specBrew);

        TabHost.TabSpec specNPM = host.newTabSpec("npm");
        specNPM.setContent(R.id.tab3);
        specNPM.setIndicator("npm");
        host.addTab(specNPM);

        TabHost.TabSpec specMore = host.newTabSpec("more");
        specMore.setContent(R.id.tab4);
        specMore.setIndicator("more");
        host.addTab(specMore);

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                new Analytics(getContext()).postEvent("onboard_install", tabId, null, null, false);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private boolean proceeding = false;

    private synchronized void next(String deviceName) {
        if (proceeding) {
            return;
        }
        proceeding = true;
        new OnboardingProgress(getContext()).setStage(OnboardingStage.TEST_SSH);
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        TestSSHFragment testSSHFragment = TestSSHFragment.newInstance(deviceName);
        fragmentTransaction
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .hide(this).add(R.id.activity_onboarding, testSSHFragment).show(testSSHFragment).commit();
    }

    private synchronized void skip() {
        if (proceeding) {
            return;
        }
        proceeding = true;
        new OnboardingProgress(getContext()).reset();
        startActivity(new Intent(getActivity(), MainActivity.class));
        getActivity().finish();
    }

}
