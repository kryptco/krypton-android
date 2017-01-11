package co.krypt.kryptonite.help;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;

/**
 * A simple {@link Fragment} subclass.
 */
public class HelpFragment extends Fragment {


    public HelpFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_help, container, false);

        Button doneButton = (Button) root.findViewById(R.id.doneButton);
        final Fragment self = this;
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                activity.postCurrentActivePageView();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom)
                        .hide(self).remove(self).commit();
            }
        });

        TabHost host = (TabHost) root.findViewById(R.id.installInstructions);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("brew");
        spec.setContent(R.id.tab1);
        spec.setIndicator("brew");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("npm");
        spec.setContent(R.id.tab2);
        spec.setIndicator("npm");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("curl");
        spec.setContent(R.id.tab3);
        spec.setIndicator("curl");
        host.addTab(spec);

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                new Analytics(getContext()).postEvent("install", tabId, null, null, false);
            }
        });

        Button pairButton = (Button) root.findViewById(R.id.pairButton);
        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity != null && activity instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom)
                            .hide(self).remove(self).commit();
                    mainActivity.setActiveTab(MainActivity.PAIR_FRAGMENT_POSITION);
                }
            }
        });

        return root;
    }
}
