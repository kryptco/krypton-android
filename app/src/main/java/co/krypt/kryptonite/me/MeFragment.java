package co.krypt.kryptonite.me;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.docuverse.identicon.NineBlockIdenticonRenderer;

import java.math.BigInteger;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.silo.Silo;
import co.krypt.kryptonite.uiutils.MLRoundedImageView;

public class MeFragment extends Fragment {
    private static final String TAG = "MeFragment";
    private EditText profileEmail;
    private ImageButton shareButton;

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
        profileEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    EditText editText = (EditText) v;
                    onEmailChanged(editText.getText().toString());
                }
            }
        });

        final Profile me = Silo.shared(getContext()).meStorage().load();
        if (me != null) {
            profileEmail.setText(me.email);
            try {
                BigInteger hash = new BigInteger(SHA256.digest(me.sshWirePublicKey));
                MLRoundedImageView identiconImage = (MLRoundedImageView) v.findViewById(R.id.identicon);

                NineBlockIdenticonRenderer renderer = new NineBlockIdenticonRenderer();
                renderer.setBackgroundColor(Color.TRANSPARENT);
                renderer.setPatchSize(80);
                Bitmap identicon = renderer.render(hash, 2000);
                identiconImage.setImageBitmap(identicon);
            } catch (CryptoException e) {
                e.printStackTrace();
            }

        } else {
            Log.e(TAG, "no me profile");
        }

        TabHost host = (TabHost) v.findViewById(R.id.addKeyInstructions);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("GitHub");
        spec.setContent(R.id.tab1);
        spec.setIndicator("GitHub");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("DigitalOcean");
        spec.setContent(R.id.tab2);
        spec.setIndicator("DigitalOcean");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("AWS");
        spec.setContent(R.id.tab3);
        spec.setIndicator("AWS");
        host.addTab(spec);

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                new Analytics(getContext()).postEvent("add key", tabId, null, null, false);
            }
        });

        final TabWidget tw = (TabWidget)host.findViewById(android.R.id.tabs);
        for (int i = 0; i < tw.getChildCount(); ++i)
        {
            final View tabView = tw.getChildTabViewAt(i);
            tabView.getLayoutParams().height = (int) (40 * getResources().getDisplayMetrics().density);
            final TextView tv = (TextView)tabView.findViewById(android.R.id.title);
            tv.setTextSize(12);
        }

        shareButton = (ImageButton) v.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (me != null) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, me.shareText());
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share your Kryptonite SSH Public Key"));
                }
            }
        });

        return v;
    }

    private void onEmailChanged(String email) {
        Profile me = Silo.shared(getContext()).meStorage().load();
        if (me == null) {
            me = new Profile(email, null);
        }
        me.email = email;
        Silo.shared(getContext()).meStorage().set(me);
        new Analytics(getContext()).publishEmailToTeamsIfNeeded(email);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
        profileEmail.setOnEditorActionListener(null);
    }

}
