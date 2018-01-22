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
import android.widget.TextView;

import co.krypt.kryptonite.MainActivity;
import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.pairing.PairFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class FirstPairFragment extends Fragment {


    private Button curlButton;
    private Button brewButton;
    private Button npmButton;
    private Button moreButton;

    private TextView installCommand;

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

        View root = inflater.inflate(R.layout.fragment_first_pair, container, false);
        Button nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skip();
            }
        });


        curlButton = root.findViewById(R.id.curlButton);
        brewButton = root.findViewById(R.id.brewButton);
        npmButton = root.findViewById(R.id.npmButton);
        moreButton = root.findViewById(R.id.moreButton);

        installCommand = root.findViewById(R.id.installCommand);

        curlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ curl https://krypt.co/kr | sh");

                resetButtons();
                curlButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("onboard_install", "curl", null, null, false);
            }
        });

        brewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ brew install kryptco/tap/kr");

                resetButtons();
                brewButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("onboard_install", "brew", null, null, false);
            }
        });

        npmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ npm install -g krd # mac only");

                resetButtons();
                npmButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("onboard_install", "npm", null, null, false);
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("# go to https://krypt.co/install");

                resetButtons();
                moreButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("onboard_install", "more", null, null, false);
            }
        });


        return root;
    }

    private void resetButtons() {
        curlButton.setTextColor(getResources().getColor(R.color.appGray));
        brewButton.setTextColor(getResources().getColor(R.color.appGray));
        npmButton.setTextColor(getResources().getColor(R.color.appGray));
        moreButton.setTextColor(getResources().getColor(R.color.appGray));
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
                .hide(this).add(R.id.activity_onboarding, testSSHFragment).show(testSSHFragment).commitAllowingStateLoss();
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
