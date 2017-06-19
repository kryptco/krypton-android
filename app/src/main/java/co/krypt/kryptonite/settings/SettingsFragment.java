package co.krypt.kryptonite.settings;


import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.knownhosts.KnownHostsFragment;
import co.krypt.kryptonite.log.AuditLogContentProvider;
import co.krypt.kryptonite.me.MeStorage;
import co.krypt.kryptonite.onboarding.OnboardingActivity;
import co.krypt.kryptonite.policy.LocalAuthentication;
import co.krypt.kryptonite.silo.Silo;

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
                LocalAuthentication.requestAuthentication(getActivity(), "Destroy key pair permanently?", "You will have to generate a new key pair and re-add it to services like GitHub. Enter your device password or pattern to confirm.",
                        new Runnable() {
                            @Override
                            public void run() {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            new Analytics(getContext()).postEvent("keypair", "destroy", null, null, false);
                                            Silo.shared(getContext()).unpairAll();
                                            KeyManager.deleteAllMeKeyPairs(getContext());
                                            new MeStorage(getContext()).delete();
                                            startActivity(new Intent(getContext(), OnboardingActivity.class));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        });
            }
        });

        Button contactButton = (Button) root.findViewById(R.id.contactUsButton);
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:hello@krypt.co?subject=Kryptonite%20Feedback&body=");
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

        Switch disableAnalyticsSwitch = (Switch) root.findViewById(R.id.disableAnalyticsSwitch);
        disableAnalyticsSwitch.setChecked(new Analytics(getContext()).isDisabled());
        disableAnalyticsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Analytics(getContext()).setAnalyticsDisabled(isChecked);
            }
        });

        Switch enableApprovedNotifications = (Switch) root.findViewById(R.id.enableAutoApproveNotificationsSwitch);
        enableApprovedNotifications.setChecked(new Settings(getContext()).approvedNotificationsEnabled());
        enableApprovedNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Settings(getContext()).setApprovedNotificationsEnabled(isChecked);
            }
        });

        Switch silenceNotifications = (Switch) root.findViewById(R.id.silenceNotificationsSwitch);
        silenceNotifications.setChecked(new Settings(getContext()).silenceNotifications());
        silenceNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new Settings(getContext()).setSilenceNotifications(isChecked);
            }
        });

        ImageButton exportLogsButton = (ImageButton) root.findViewById(R.id.exportLogsButton);
        exportLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String token = AuditLogContentProvider.setAuditLogToken(v.getContext());
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Kryptonite Audit Log");
                    sendIntent.setType("application/x-sqlite3");
                    Uri auditLogUriWithToken = AuditLogContentProvider.getAuditLogURIWithToken();
                    if (auditLogUriWithToken == null) {
                        return;
                    }
                    sendIntent.putExtra(Intent.EXTRA_STREAM, auditLogUriWithToken);
                    startActivity(sendIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(v.getContext(), "Error exporting audit log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        Button editKnownHostsButton = (Button) root.findViewById(R.id.editKnownHostsButton);
        editKnownHostsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getView().setTranslationZ(0);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                KnownHostsFragment knownHostsFragment = new KnownHostsFragment();
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.delayed)
                        .replace(R.id.fragmentOverlay, knownHostsFragment).commit();
                new Analytics(getActivity().getApplicationContext()).postPageView("KnownHostsEdit");
            }
        });

        return root;
    }

}
