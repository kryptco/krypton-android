package co.krypt.kryptonite.onboarding;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.policy.LocalAuthentication;

import static co.krypt.kryptonite.MainActivity.CAMERA_PERMISSION_GRANTED_ACTION;
import static co.krypt.kryptonite.MainActivity.CAMERA_PERMISSION_REQUEST;
import static co.krypt.kryptonite.MainActivity.LOCATION_PERMISSION_GRANTED_ACTION;
import static co.krypt.kryptonite.MainActivity.LOCATION_PERMISSION_REQUEST;
import static co.krypt.kryptonite.MainActivity.USER_AUTHENTICATION_REQUEST;

public class OnboardingActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        new Analytics(getApplicationContext()).postEvent("onboard", "start", null, null, false);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        GenerateFragment generateFragment = new GenerateFragment();
        fragmentTransaction.add(R.id.activity_onboarding, generateFragment).commit();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(CAMERA_PERMISSION_GRANTED_ACTION);
                    sendBroadcast(cameraIntent);
                }
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent locationIntent = new Intent(LOCATION_PERMISSION_GRANTED_ACTION);
                    sendBroadcast(locationIntent);
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == USER_AUTHENTICATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                LocalAuthentication.onSuccess();
            }
        }
    }
}
