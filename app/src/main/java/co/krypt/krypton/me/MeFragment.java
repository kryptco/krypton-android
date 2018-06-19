package co.krypt.krypton.me;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.IdentityService;
import co.krypt.krypton.silo.Silo;

public class MeFragment extends Fragment {
    private static final String TAG = "MeFragment";
    private EditText profileEmail;
    private ImageButton shareButton;

    private Button githubButton;
    private Button digitaloceanButton;
    private Button awsButton;

    private TextView addKeyCommandTextView;

    public MeFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_me, container, false);
        profileEmail = (EditText) v.findViewById(R.id.profileEmail);
        profileEmail.setText("loading...");
        profileEmail.setTextColor(getResources().getColor(R.color.appGray, null));
        profileEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int keyCode, KeyEvent event) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                onEmailChanged(v.getText().toString());
                return false;
            }
        });
        profileEmail.setOnFocusChangeListener((v1, hasFocus) -> {
            if (!hasFocus) {
                EditText editText = (EditText) v1;
                onEmailChanged(editText.getText().toString());
            }
        });

        githubButton = v.findViewById(R.id.githubButton);
        digitaloceanButton = v.findViewById(R.id.digitaloceanButton);
        awsButton = v.findViewById(R.id.awsButton);
        addKeyCommandTextView = v.findViewById(R.id.addKeyTextView);

        githubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addKeyCommandTextView.setText("$ kr github");

                githubButton.setTextColor(getResources().getColor(R.color.appGreen));
                digitaloceanButton.setTextColor(getResources().getColor(R.color.appGray));
                awsButton.setTextColor(getResources().getColor(R.color.appGray));

                new Analytics(getContext()).postEvent("add key", "GitHub", null, null, false);
            }
        });

        digitaloceanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addKeyCommandTextView.setText("$ kr digitalocean");

                digitaloceanButton.setTextColor(getResources().getColor(R.color.appGreen));
                githubButton.setTextColor(getResources().getColor(R.color.appGray));
                awsButton.setTextColor(getResources().getColor(R.color.appGray));

                new Analytics(getContext()).postEvent("add key", "DigitalOcean", null, null, false);
            }
        });

        awsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addKeyCommandTextView.setText("$ kr aws");

                awsButton.setTextColor(getResources().getColor(R.color.appGreen));
                githubButton.setTextColor(getResources().getColor(R.color.appGray));
                digitaloceanButton.setTextColor(getResources().getColor(R.color.appGray));

                new Analytics(getContext()).postEvent("add key", "AWS", null, null, false);
            }
        });


        shareButton = (ImageButton) v.findViewById(R.id.shareButton);

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new IdentityService.GetProfile(getContext()));
        return v;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(IdentityService.GetProfileResult r) {
        Profile me = r.profile;
        if (me != null) {
            profileEmail.setText(me.email);
            profileEmail.setTextColor(getResources().getColor(R.color.appBlack, null));

            shareButton.setOnClickListener(v -> {
                if (me != null) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, me.shareText());
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share your Krypton SSH Public Key"));
                }
            });
        } else {
            Log.e(TAG, "no profile");
        }
    }

    private void onEmailChanged(String email) {
        Context context = getContext();
        if (context == null){
            return;
        }

        Profile me = Silo.shared(getContext()).meStorage().load();
        if (me == null) {
            me = new Profile(email, null, null, null);
        }
        me.email = email;
        Silo.shared(getContext()).meStorage().set(me);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        profileEmail.setOnEditorActionListener(null);
    }

}
