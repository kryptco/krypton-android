package co.krypt.krypton.team.onboarding;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import co.krypt.krypton.R;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.team.onboarding.create.CreateStage;
import co.krypt.krypton.team.onboarding.create.CreateTeamProgress;
import co.krypt.krypton.team.onboarding.create.OnboardingCreateFragment;
import co.krypt.krypton.team.onboarding.create.OnboardingLoadTeamFragment;
import co.krypt.krypton.team.onboarding.join.JoinStage;
import co.krypt.krypton.team.onboarding.join.JoinTeamProgress;
import co.krypt.krypton.utils.CrashReporting;
import co.krypt.krypton.utils.Services;
import co.krypt.kryptonite.MainActivity;

public class TeamOnboardingActivity extends FragmentActivity {

    private static final String TAG = "TeamOnboarding";

    private static final Services services = new Services();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        CrashReporting.startANRReporting();

        setContentView(R.layout.fragment_teams);

        if (getIntent() != null && getIntent().getAction() != null) {
            onNewIntent(getIntent());
            return;
        }

        Fragment initialFragment = null;

        CreateTeamProgress createTeamProgress = new CreateTeamProgress(getApplicationContext());
        JoinTeamProgress joinTeamProgress = new JoinTeamProgress(getApplicationContext());

        if (createTeamProgress.inProgress()) {
            initialFragment = createTeamProgress.currentStage().getFragment();
        } else if (joinTeamProgress.inProgress()) {
            initialFragment = joinTeamProgress.currentStage().getFragment();
        } else {
            //  default to create team
            initialFragment = new OnboardingCreateFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_teams, initialFragment)
                .commitAllowingStateLoss();

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
    protected void onResume() {
        super.onResume();
        Silo.shared(getApplicationContext()).start();
    }

    @Override
    protected void onPause() {
        Silo.shared(getApplicationContext()).stop();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (TeamDataProvider.getTeamHomeData(this).success != null) {
                Toast.makeText(getApplicationContext(), "You must leave your current team before joining another.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            }
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
            return;
        }
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW")
                && intent.getData() != null) {
            Uri data = intent.getData();
            if ((data.getScheme().equals("krypton") && data.getHost().equals("verify_email")) ||
                    (data.getScheme().equals("https") && data.getHost().equals("krypt.co") && data.getPath().equals("/app/verify_email.html"))) {

                final String nonce;
                // Handle https:// link
                String queryParam = data.getQueryParameter("nonce");
                if (queryParam != null) {
                    nonce = queryParam;
                } else {
                    // Handle krypton:// link
                    nonce = data.getLastPathSegment();
                }

                Log.i(TAG, "received VERIFY_EMAIL intent");
                CreateTeamProgress createTeamProgress = new CreateTeamProgress(this);
                JoinTeamProgress joinTeamProgress = new JoinTeamProgress(this);

                if (createTeamProgress.inProgress()) {
                    createTeamProgress.updateTeamData((s, d) -> {
                        d.emailChallengeNonce = nonce;
                        return CreateStage.LOAD_TEAM;
                    });
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                            .replace(R.id.fragment_teams, new OnboardingLoadTeamFragment())
                            .commitAllowingStateLoss();
                } else if (joinTeamProgress.inProgress()) {
                    joinTeamProgress.updateTeamData((s, d) -> {
                        d.emailChallengeNonce = nonce;
                        if (s.equals(JoinStage.CHALLENGE_EMAIL_SENT)) {
                            return s;
                        } else if (s.equals(JoinStage.INPERSON_CHALLENGE_EMAIL_SENT)) {
                            return s;
                        } else if (s.equals(JoinStage.VERIFY_EMAIL)) {
                            // Allows clicking on an old verification link before sending a new challenge in case email is rate limited
                            return JoinStage.CHALLENGE_EMAIL_SENT;
                        }
                        return JoinStage.NONE;
                    });
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                            .replace(R.id.fragment_teams, joinTeamProgress.currentStage().getFragment())
                            .commitAllowingStateLoss();
                } else {
                    Toast.makeText(getApplicationContext(), "Begin joining a team before verifying your email.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                    return;
                }
            } else if (data.getScheme().equals("krypton") && data.getHost().equals("join_team")) {
                Log.i(TAG, "received JOIN_TEAM intent");

                new CreateTeamProgress(this).reset();
                JoinTeamProgress progress = new JoinTeamProgress(getApplicationContext());
                progress.updateTeamData((s, d) -> {
                    try {
                        Sigchain.NativeResult<Sigchain.TeamHomeData> teamData = TeamDataProvider.getTeamHomeData(getApplicationContext());
                        if (s.equals(JoinStage.NONE)) {
                            s = JoinStage.DECRYPT_INVITE;
                            d.inviteLink = data.toString();
                        } else {
                            //  continue current onboarding
                        }
                    } catch (Native.NotLinked notLinked) {
                        notLinked.printStackTrace();
                        return s;
                    }
                    Profile me = Silo.shared(getBaseContext()).meStorage().load();
                    if (me == null) {
                        Toast.makeText(getApplicationContext(), "Welcome to Krypton! Let's generate your key pair before joining the team.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                        return s;
                    }
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_left)
                            .replace(R.id.fragment_teams, s.getFragment())
                            .commitAllowingStateLoss();
                    return s;
                });

            }
        }
    }
}
