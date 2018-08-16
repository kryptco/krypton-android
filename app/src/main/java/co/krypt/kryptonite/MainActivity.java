package co.krypt.kryptonite;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.approval.ApprovalDialog;
import co.krypt.krypton.developer.DeveloperFragment;
import co.krypt.krypton.devices.DevicesFragment;
import co.krypt.krypton.help.HelpFragment;
import co.krypt.krypton.me.MeFragment;
import co.krypt.krypton.onboarding.OnboardingActivity;
import co.krypt.krypton.onboarding.devops.DevopsOnboardingProgress;
import co.krypt.krypton.onboarding.u2f.U2FOnboardingProgress;
import co.krypt.krypton.pairing.PairFragment;
import co.krypt.krypton.policy.LocalAuthentication;
import co.krypt.krypton.settings.Settings;
import co.krypt.krypton.settings.SettingsFragment;
import co.krypt.krypton.silo.Notifications;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.team.TeamFragment;
import co.krypt.krypton.team.onboarding.TeamOnboardingActivity;
import co.krypt.krypton.transport.BluetoothService;
import co.krypt.krypton.utils.CrashReporting;
import co.krypt.krypton.utils.Services;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int ME_FRAGMENT_POSITION = 0;
    public static final int PAIR_FRAGMENT_POSITION = 1;
    public static final int DEVICES_FRAGMENT_POSITION = 2;
    public static final int DEVELOPER_FRAGMENT_POSITION = 3;
    public static final int TEAM_FRAGMENT_POSITION = 4;

    public static final int CAMERA_PERMISSION_REQUEST = 0;
    public static final int USER_AUTHENTICATION_REQUEST = 2;

    public static final String CAMERA_PERMISSION_GRANTED_ACTION = "co.krypt.android.action.CAMERA_PERMISSION_GRANTED";

    public static final String ACTION_VIEW_TEAMS_TAB = "co.krypt.android.action.VIEW_TEAMS_TAB";

    @SuppressWarnings("unused")
    private static final Services services = new Services();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Silo silo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        CrashReporting.startANRReporting();

        Notifications.setupNotificationChannels(getApplicationContext());

        silo = Silo.shared(getApplicationContext());
        try {
            startService(new Intent(this, BluetoothService.class));
        } catch (IllegalStateException e) {
            //  thrown when starting a service is not allowed
            e.printStackTrace();
        }
        U2FOnboardingProgress u2fProgress = new U2FOnboardingProgress(getApplicationContext());
        DevopsOnboardingProgress devopsProgress = new DevopsOnboardingProgress(getApplicationContext());
        if (u2fProgress.inProgress()
                || (new Settings(getApplicationContext()).developerMode() && devopsProgress.inProgress())) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        if (ConnectionResult.SUCCESS != GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext())) {
            //  TODO: warn about no push notifications, prompt to install google play services
        }

        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                SettingsFragment settingsFragment = new SettingsFragment();
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom, R.anim.enter_from_bottom, R.anim.exit_to_bottom)
                        .addToBackStack(null)
                        .replace(R.id.fragmentOverlay, settingsFragment).commit();
                new Analytics(getApplicationContext()).postPageView("About");
            }
        });

        ImageButton infoButton = (ImageButton) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                HelpFragment helpFragment = new HelpFragment();
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_bottom, R.anim.enter_from_bottom, R.anim.exit_to_bottom)
                        .addToBackStack(null)
                        .replace(R.id.fragmentOverlay, helpFragment).commit();
                new Analytics(getApplicationContext()).postPageView("Help");
            }
        });

        if (getIntent() != null) {
            onNewIntent(getIntent());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(CAMERA_PERMISSION_GRANTED_ACTION);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(cameraIntent);
                }
        }
    }

    public void relayoutTabs() {
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    public void setActiveTab(int position) {
        mViewPager.setCurrentItem(position, true);
    }

    public void postCurrentActivePageView() {
        postActivePage(mSectionsPagerAdapter.lastPrimary);
    }

    private void postActivePage(int position) {
        switch (position) {
            case ME_FRAGMENT_POSITION:
                new Analytics(getApplicationContext()).postPageView("Me");
                break;
            case PAIR_FRAGMENT_POSITION:
                new Analytics(getApplicationContext()).postPageView("Pair");
                break;
            case DEVICES_FRAGMENT_POSITION:
                new Analytics(getApplicationContext()).postPageView("Sessions");
                break;
            case TEAM_FRAGMENT_POSITION:
                new Analytics(getApplicationContext()).postPageView("Team");
                break;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public int lastPrimary = -1;

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (position != lastPrimary) {
                lastPrimary = position;
                postActivePage(position);
            }
        }

        private MeFragment meFragment = new MeFragment();
        private PairFragment pairFragment = PairFragment.newInstance();
        private DevicesFragment devicesFragment = DevicesFragment.newInstance(1);
        private DeveloperFragment developerFragment = new DeveloperFragment();
        private TeamFragment teamFragment = new TeamFragment();

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case ME_FRAGMENT_POSITION:
                    return meFragment;
                case PAIR_FRAGMENT_POSITION:
                    return pairFragment;
                case DEVICES_FRAGMENT_POSITION:
                    return devicesFragment;
                case DEVELOPER_FRAGMENT_POSITION:
                    return developerFragment;
                case TEAM_FRAGMENT_POSITION:
                    return teamFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            //  default Accounts, Pair, Devices
            int numTabs = 3;
            if (new Settings(getApplicationContext()).developerMode()) {
                //  developer tab
                numTabs += 1;
            }
            if (TeamDataProvider.shouldShowTeamsTab(getApplicationContext())) {
                numTabs += 1;
            }
            return numTabs;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case ME_FRAGMENT_POSITION:
                    return "Accounts";
                case PAIR_FRAGMENT_POSITION:
                    return "Pair";
                case DEVICES_FRAGMENT_POSITION:
                    return "Computers";
                case DEVELOPER_FRAGMENT_POSITION:
                    return "Developer";
                case TEAM_FRAGMENT_POSITION:
                    return "Team";

            }
            return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "start");
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_VIEW_TEAMS_TAB)) {
                    setActiveTab(TEAM_FRAGMENT_POSITION);
                }
                Log.i(TAG, action);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "resume");
        silo.start();

        new Thread(this::checkForTeamThenClipboard).start();
    }

    private void checkForTeamThenClipboard() {
        try {
            if (TeamDataProvider.getTeamHomeData(this).success != null) {
                return;
            }
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
            return;
        }
        runOnUiThread(this::checkClipboardForAppLinks);
    }

    private void checkClipboardForAppLinks() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null) {
            return;
        }
        ClipData.Item item = clipData.getItemAt(0);
        if (item == null) {
            return;
        }

        CharSequence pasteData = item.getText();
        if (pasteData == null) {
            Uri pasteUri = item.getUri();
            if (pasteUri != null) {
                pasteData = pasteUri.toString();
            }
        }

        if (pasteData != null) {
            String pasteString = pasteData.toString();
            String[] toks = pasteString.split("\\s");
            for (String tok: toks) {
                if (tok.startsWith("krypton://")) {
                    Uri uri = Uri.parse(tok);
                    if (uri == null) {
                        continue;
                    }

                    // https://stackoverflow.com/questions/23418543/clear-all-clipboard-entries
                    // Attempt to clear secret link from clipboard
                    clipboard.setPrimaryClip(new ClipData(new ClipDescription("", new String[]{}), new ClipData.Item("")));

                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("App Link Found on Clipboard");
                    alertDialog.setMessage(uri.toString());
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Process with Krypton",
                            (dialog, which) -> {
                                dialog.dismiss();
                                Intent appLinkIntent = new Intent(this, TeamOnboardingActivity.class);
                                appLinkIntent.setAction("android.intent.action.VIEW");
                                appLinkIntent.setData(uri);
                                startActivity(appLinkIntent);
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ignore",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                    break;
                }
            }
        }
    }

    @Override
    protected void onPause() {
        silo.stop();
        Log.i(TAG, "pause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        for (Fragment fragment: getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.isVisible()) {
                if (fragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
                    fragment.getChildFragmentManager().popBackStack();
                    return;
                }
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (v.hasFocus() && !outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == USER_AUTHENTICATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                LocalAuthentication.onSuccess();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getStringExtra("requestID") != null) {
            final String requestID = intent.getStringExtra("requestID");
            ApprovalDialog.showApprovalDialog(this, requestID);
        } else {
            Log.d(TAG, "empty intent");
        }
    }
}
