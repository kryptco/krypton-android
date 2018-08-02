package co.krypt.krypton.u2f;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import co.krypt.krypton.R;

public class U2FAuthenticateActivity extends AppCompatActivity {

    private static final String TAG = U2FAuthenticateActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u2f_authenticate);

        Intent intent = getIntent();
        if (intent != null) {
            Log.d(TAG, intent.toString());
            Log.d(TAG, intent.getStringExtra("request"));
        }
    }
}
