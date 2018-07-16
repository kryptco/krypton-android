package co.krypt.krypton.onboarding;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.uiutils.Error;

/**
 * A simple {@link Fragment} subclass.
 */
public class EnterEmailFragment extends Fragment {
    private static final String TAG = "EnterEmailFragment";

    private EditText profileEmail;
    private Button nextButton;

    public EnterEmailFragment() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(profileEmail, InputMethodManager.SHOW_IMPLICIT);

        super.onViewCreated(view, savedInstanceState);
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

        Profile me = new MeStorage(getContext()).load();
        if (me != null && me.email != null) {
            profileEmail.setText(me.email);
        } else {
            profileEmail.setText("");
        }

        return root;
    }

    private void next() {
        Analytics analytics = new Analytics(getContext());
        String email = profileEmail.getText().toString();
        if (email == null || email.trim().equals("")) {
            analytics.postEvent("email", "skipped", null, null, false);
            email = MeStorage.getDeviceName();
        } else {
            analytics.postEvent("email", "typed", null, null, false);
            email = email.trim();
        }

        try {
            new MeStorage(getContext()).setEmail(email);
        } catch (CryptoException e) {
            e.printStackTrace();
            Error.shortToast(getContext(), "Failed to set email: " + e.getMessage());
            return;
        }

        final OnboardingProgress progress = new OnboardingProgress(getContext());
        progress.setStage(OnboardingStage.FIRST_PAIR);
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
            nextButton.setText("Skip");
            nextButton.setTextColor(getResources().getColor(R.color.appGray));
        } else {
            nextButton.setText("NEXT");
            nextButton.setTextColor(getResources().getColor(R.color.appGreen));
        }
    }

}
