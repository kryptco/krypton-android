package co.krypt.kryptonite.onboarding;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.docuverse.identicon.NineBlockIdenticonRenderer;

import java.math.BigInteger;

import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.crypto.KeyManager;
import co.krypt.kryptonite.crypto.SSHKeyPair;
import co.krypt.kryptonite.me.MeStorage;
import co.krypt.kryptonite.uiutils.MLRoundedImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class EnterEmailFragment extends Fragment {
    private static final String TAG = "EnterEmailFragment";

    private EditText profileEmail;
    private Button nextButton;

    public EnterEmailFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_enter_email, container, false);
        nextButton = (Button) root.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });

        profileEmail = (EditText) root.findViewById(R.id.profileEmail);
        profileEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int keyCode, KeyEvent event) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        });
        profileEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                onEmailChanged();
            }
        });

        MLRoundedImageView identiconImage = (MLRoundedImageView) root.findViewById(R.id.identicon);
        try {
            SSHKeyPair keyPair = KeyManager.loadOrGenerateKeyPair(KeyManager.MY_RSA_KEY_TAG);
            BigInteger hash = new BigInteger(keyPair.publicKeyFingerprint());

            NineBlockIdenticonRenderer renderer = new NineBlockIdenticonRenderer();
            renderer.setBackgroundColor(Color.TRANSPARENT);
            renderer.setPatchSize(80);
            Bitmap identicon = renderer.render(hash, 2000);
            identiconImage.setImageBitmap(identicon);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return root;
    }

    private void next() {
        Analytics analytics = new Analytics(getContext());
        String email = profileEmail.getText().toString();
        if (email == null || email.trim().equals("")) {
            analytics.postEvent("email", "typed", null, null, false);
            email = Build.MODEL;
        } else {
            analytics.postEvent("email", "skipped", null, null, false);
            email = email.trim();
        }
        analytics.publishEmailToTeamsIfNeeded(email);

        new MeStorage(getContext()).setEmail(email);
        FirstPairFragment firstPairFragment = new FirstPairFragment();
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
                .add(R.id.activity_onboarding, firstPairFragment)
                .hide(this).show(firstPairFragment).commit();

    }

    private void onEmailChanged() {
        String email = profileEmail.getText().toString().trim();
        if (email.length() == 0) {
            nextButton.setText("SKIP");
            nextButton.setTextColor(getResources().getColor(R.color.appGray));
        } else {
            nextButton.setText("NEXT");
            nextButton.setTextColor(getResources().getColor(R.color.appGreen));
        }
    }

}
