package co.krypt.kryptonite.help;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;

/**
 * A simple {@link Fragment} subclass.
 */
public class HelpFragment extends Fragment {


    public HelpFragment() {
    }

    private Button curlButton;
    private Button brewButton;
    private Button npmButton;
    private Button moreButton;

    private TextView installCommand;


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
                        .hide(self).commit();
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

        curlButton = root.findViewById(R.id.curlHelp);
        brewButton = root.findViewById(R.id.brewHelp);
        npmButton = root.findViewById(R.id.npmHelp);
        moreButton = root.findViewById(R.id.moreHelp);

        installCommand = root.findViewById(R.id.installCommandHelp);

        curlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ curl https://krypt.co/kr | sh");

                resetButtons();
                curlButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("help_install", "curl", null, null, false);
            }
        });

        brewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ brew install kryptco/tap/kr");

                resetButtons();
                brewButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("help_install", "brew", null, null, false);
            }
        });

        npmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("$ npm install -g krd # mac only");

                resetButtons();
                npmButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("help_install", "npm", null, null, false);
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installCommand.setText("# go to https://krypt.co/install");

                resetButtons();
                moreButton.setTextColor(getResources().getColor(R.color.appGreen));

                new Analytics(getContext()).postEvent("help_install", "more", null, null, false);
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

}
