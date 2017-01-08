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

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.pairing.PairFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class FirstPairFragment extends Fragment {


    private final PairFragment pairFragment = new PairFragment();

    private BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    next();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getChildFragmentManager().beginTransaction().add(R.id.pairLayout, pairFragment).commit();
        pairFragment.setUserVisibleHint(true);

        View root = inflater.inflate(R.layout.fragment_first_pair, container, false);
        Button nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        TabHost host = (TabHost) root.findViewById(R.id.installInstructions);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator("brew");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator("npm");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("Tab Three");
        spec.setContent(R.id.tab3);
        spec.setIndicator("curl");
        host.addTab(spec);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getChildFragmentManager().beginTransaction().remove(pairFragment).commit();
    }

    private void next() {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        TestSSHFragment testSSHFragment = new TestSSHFragment();
        fragmentTransaction.hide(this).add(R.id.activity_onboarding, testSSHFragment).show(testSSHFragment).commit();
    }

}
