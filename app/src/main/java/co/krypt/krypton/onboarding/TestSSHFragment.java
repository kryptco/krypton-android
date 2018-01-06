package co.krypt.krypton.onboarding;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import co.krypt.krypton.MainActivity;
import co.krypt.krypton.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class TestSSHFragment extends Fragment {
    public static final String SSH_ME_ACTION = "co.krypt.android.action.SSH_ME";
    private TextView tryHeader;
    private TextView tryText;

    private TextView githubHeader;
    private TextView githubText;

    private Button nextButton;
    private Button skipButton;

    private static final String ARG_DEVICE_NAME = "device_name";
    private String deviceName;

    public static TestSSHFragment newInstance(String deviceName) {
        TestSSHFragment fragment = new TestSSHFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_NAME, deviceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceName = getArguments().getString(ARG_DEVICE_NAME);
        }
    }

    private final BroadcastReceiver sshMeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryHeader.animate().alpha(.25f).setDuration(1000).start();
            tryText.animate().alpha(.25f).setDuration(1000).start();
            githubHeader.animate().alpha(1.0f).setDuration(1000).setStartDelay(1000).start();
            githubText.animate().alpha(1.0f).setDuration(1000).setStartDelay(1000).start();
            nextButton.animate().alpha(1.0f).setDuration(1000).setStartDelay(1000).start();
            skipButton.animate().alpha(0f).setDuration(1000).setStartDelay(1000).start();
        }
    };

    public TestSSHFragment() { }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter sshMeFilter = new IntentFilter();
        sshMeFilter.addAction(SSH_ME_ACTION);
        context.registerReceiver(sshMeReceiver, sshMeFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getContext().unregisterReceiver(sshMeReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_test_ssh, container, false);
        nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        nextButton.setAlpha(0f);

        skipButton = (Button) root.findViewById(R.id.skipButton);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        tryHeader = (TextView) root.findViewById(R.id.tryHeader);
        tryText = (TextView) root.findViewById(R.id.tryText);
        githubHeader = (TextView) root.findViewById(R.id.githubHeader);
        githubText = (TextView) root.findViewById(R.id.githubText);
        githubHeader.setAlpha(0f);
        githubText.setAlpha(0f);

        TextView deviceNameText = (TextView) root.findViewById(R.id.deviceName);
        if (deviceName != null) {
            deviceNameText.setText(deviceName);
        }
        return root;
    }

    private void next() {
        final OnboardingProgress progress = new OnboardingProgress(getContext());
        progress.reset();
        startActivity(new Intent(getContext(), MainActivity.class));
        getActivity().finish();
    }
}
