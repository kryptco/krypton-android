package co.krypt.kryptonite.onboarding;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;

import co.krypt.kryptonite.R;

public class OnboardingActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        GenerateFragment generateFragment = new GenerateFragment();
        fragmentTransaction.add(R.id.activity_onboarding, generateFragment).commit();
    }
}
