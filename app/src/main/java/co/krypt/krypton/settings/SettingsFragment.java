package co.krypt.krypton.settings;


import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.crypto.KeyManager;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.onboarding.OnboardingActivity;
import co.krypt.krypton.onboarding.devops.DevopsOnboardingProgress;
import co.krypt.krypton.policy.LocalAuthentication;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.team.onboarding.create.CreateTeamProgress;
import co.krypt.krypton.team.onboarding.join.JoinTeamProgress;
import co.krypt.kryptonite.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {


    public SettingsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        Settings settings = new Settings(getContext());

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
        TextView versionText = (TextView) root.findViewById(R.id.versionText);
        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            versionText.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        ImageButton deleteButton = (ImageButton) root.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalAuthentication.requestAuthentication(getActivity(), "Destroy key pair permanently?", "You will have to generate a new key pair and re-add it to services like GitHub.",
                        () -> new Thread(() -> {
                            try {
                                new Analytics(getContext()).postEvent("keypair", "destroy", null, null, false);
                                EventBus.getDefault().post(new TeamService.RequestTeamOperation(
                                                Sigchain.RequestableTeamOperation.leave(),
                                                C.background(getContext())
                                        )
                                );
                                Silo.shared(getContext()).unpairAll();
                                KeyManager.deleteAllMeKeyPairs(getContext());
                                new MeStorage(getContext()).delete();
                                new JoinTeamProgress(getContext()).resetAndDeleteTeam(getContext());
                                new CreateTeamProgress(getContext()).reset();
                                new DevopsOnboardingProgress(getContext()).reset();
                                startActivity(new Intent(getContext(), OnboardingActivity.class));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start());
            }
        });
        Runnable updateDeleteButton = () -> {
            deleteButton.setEnabled(settings.developerMode());
            if (settings.developerMode()) {
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                deleteButton.setVisibility(View.INVISIBLE);
            }
        };
        updateDeleteButton.run();

        Button contactButton = (Button) root.findViewById(R.id.contactUsButton);
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:hello@krypt.co?subject=Krypton%20Feedback&body=");
                intent.setData(data);
                startActivity(intent);
            }
        });

        Button softwareButton = (Button) root.findViewById(R.id.softwareButton);
        softwareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://krypt.co/app/open-source-libraries/"));
                startActivity(browserIntent);
            }
        });

        Button privacyButton = (Button) root.findViewById(R.id.privacyButton);
        privacyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://krypt.co/app/privacy/"));
                startActivity(browserIntent);
            }
        });

        SwitchCompat oneTouchLoginSwitch = root.findViewById(R.id.oneTouchLoginToggle);
        oneTouchLoginSwitch.setChecked(settings.oneTouchLogin());
        oneTouchLoginSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setOneTouchLogin(isChecked);
            }
        });

        SwitchCompat disableAnalyticsSwitch = (SwitchCompat) root.findViewById(R.id.disableAnalyticsSwitch);
        disableAnalyticsSwitch.setChecked(new Analytics(getContext()).isDisabled());
        disableAnalyticsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Analytics(getContext()).setAnalyticsDisabled(isChecked);
            }
        });

        SwitchCompat developerMode = root.findViewById(R.id.developerModeSwitch);
        developerMode.setChecked(settings.developerMode());
        developerMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.setDeveloperMode(isChecked);
            updateDeleteButton.run();
            new Handler(Looper.getMainLooper()).post(() -> ((MainActivity) getActivity()).relayoutTabs());
            if (isChecked && new DevopsOnboardingProgress(getContext()).inProgress()) {
                startActivity(new Intent(getContext(), OnboardingActivity.class));
                getActivity().finish();
            }
        });

        SwitchCompat enableApprovedNotifications = (SwitchCompat) root.findViewById(R.id.enableAutoApproveNotificationsSwitch);
        enableApprovedNotifications.setChecked(settings.approvedNotificationsEnabled());
        enableApprovedNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setApprovedNotificationsEnabled(isChecked);
            }
        });

        SwitchCompat silenceNotifications = (SwitchCompat) root.findViewById(R.id.silenceNotificationsSwitch);
        silenceNotifications.setChecked(settings.silenceNotifications());
        silenceNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setSilenceNotifications(isChecked);
            }
        });

        return root;
    }

}
