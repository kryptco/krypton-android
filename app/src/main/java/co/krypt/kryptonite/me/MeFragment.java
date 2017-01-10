package co.krypt.kryptonite.me;

import android.content.Context;
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
import android.widget.TextView;

import com.docuverse.identicon.NineBlockIdenticonRenderer;

import java.math.BigInteger;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.crypto.SHA256;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.protocol.Profile;
import co.krypt.kryptonite.silo.Silo;
import co.krypt.kryptonite.uiutils.MLRoundedImageView;

public class MeFragment extends Fragment {
    private static final String TAG = "MeFragment";
    private EditText profileEmail;

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

        Profile me = Silo.shared(getContext()).meStorage().load();
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
}
