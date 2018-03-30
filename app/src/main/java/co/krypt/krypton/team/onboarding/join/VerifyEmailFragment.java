package co.krypt.krypton.team.onboarding.join;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicReference;

import co.krypt.krypton.R;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.uiutils.Email;

public class VerifyEmailFragment extends Fragment {

    private final String TAG = "OnboardingVerifyEmail";

    private JoinTeamProgress progress;

    public VerifyEmailFragment() {
    }

    public static VerifyEmailFragment newInstance() {
        return new VerifyEmailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_teams_onboarding_verify_email, container, false);
        Button cancelButton = rootView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            progress.resetAndDeleteTeam(v.getContext());
            getActivity().finish();
        });

        ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setAlpha(0);

        Button submitButton = rootView.findViewById(R.id.submitButton);
        Button changeButton = rootView.findViewById(R.id.changeButton);
        Button resendButton = rootView.findViewById(R.id.resendButton);
        AppCompatTextView checkYourEmailText = rootView.findViewById(R.id.checkYourEmailText);
        AppCompatTextView checkYourEmailSubtext = rootView.findViewById(R.id.checkYourEmailSubtext);

        AppCompatEditText emailEditText = rootView.findViewById(R.id.profileEmail);

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        Runnable onSubmitUI = () -> {
            emailEditText.setEnabled(false);
            emailEditText.clearFocus();
            if (imm != null) {
                imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);
            }
            progressBar.animate().alpha(1).setDuration(1000).start();
            submitButton.setEnabled(false);
            submitButton.animate().alpha(0).setDuration(500).start();
            checkYourEmailSubtext.animate().alpha(0).setDuration(500).start();
            checkYourEmailText.animate().alpha(0).setDuration(500).start();
        };
        Runnable onEditUI = () -> {
            emailEditText.setEnabled(true);

            submitButton.animate().alpha(1).setDuration(500).start();
            changeButton.animate().alpha(0).setDuration(500).start();
            resendButton.animate().alpha(0).setDuration(500).start();
            submitButton.setEnabled(true);
            changeButton.setEnabled(false);
            resendButton.setEnabled(false);
            checkYourEmailSubtext.animate().alpha(0).setDuration(500).start();
            checkYourEmailText.animate().alpha(0).setDuration(500).start();
            progressBar.animate().alpha(0).setDuration(500).start();
        };
        Runnable onSuccessUI = () -> {
            progressBar.animate().alpha(0).setDuration(500).start();
            changeButton.animate().alpha(1).setDuration(1000).start();
            resendButton.animate().alpha(1).setDuration(1000).start();
            submitButton.animate().alpha(0).setDuration(500).start();
            changeButton.setEnabled(true);
            resendButton.setEnabled(true);
            checkYourEmailSubtext.animate().alpha(1).setDuration(500).start();
            checkYourEmailText.animate().alpha(1).setDuration(500).start();
        };

        Runnable focusEmail = () -> {
            onEditUI.run();
            emailEditText.requestFocus();

            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        };
        Runnable onErrorUI = () -> {
            focusEmail.run();
            Toast.makeText(getContext(), "Error sending email verification.", Toast.LENGTH_SHORT).show();
        };

        submitButton.setAlpha(1);
        changeButton.setAlpha(0);
        resendButton.setAlpha(0);
        checkYourEmailSubtext.setAlpha(0);
        checkYourEmailText.setAlpha(0);
        changeButton.setEnabled(false);
        resendButton.setEnabled(false);

        AppCompatTextView teamName = rootView.findViewById(R.id.teamName);
        teamName.setText(progress.getTeamOnboardingData().decryptInviteOutput.teamName);

        String initialEmail = Silo.shared(getContext().getApplicationContext()).meStorage().load().email;
        emailEditText.setText(initialEmail);

        final Sigchain.IndirectInvitationRestriction restriction = progress.getTeamOnboardingData().decryptInviteOutput.indirectInvitationSecret.restriction;
        Runnable requestEmailChallenge = () -> {
            final String email = emailEditText.getEditableText().toString();
            if (!Email.verifyEmailPattern.matcher(email).matches()) {
                Toast.makeText(getContext(), "Please enter a valid email.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!restriction.isSatisfiedByEmail(email)) {
                if (restriction.domain != null) {
                    Toast.makeText(getContext(), "Email must end with @" + restriction.domain, Toast.LENGTH_SHORT).show();
                } else if (restriction.emails != null) {
                    Toast.makeText(getContext(), "Invite link not valid for this email address.", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            onSubmitUI.run();

            new Thread(() -> {
                MeStorage meStorage = Silo.shared(getContext()).meStorage();
                try {
                    meStorage.setEmail(email);
                } catch (CryptoException e) {
                    co.krypt.krypton.uiutils.Error.shortToast(getContext(), "Failed to set email: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                Profile me = meStorage.load();
                progress.updateTeamData((s, d) -> {
                    if (s.equals(JoinStage.VERIFY_EMAIL)) {
                        d.profile = me;
                        Log.i(TAG, "requesting email challenge...");
                        try {
                            final Sigchain.NativeResult<JsonObject> result = TeamDataProvider.requestEmailChallenge(getContext(), email);
                            emailEditText.post(() -> {
                                if (result.success != null) {
                                    Log.i(TAG, "request email challenge success");
                                    onSuccessUI.run();
                                } else {
                                    Log.i(TAG, "request email challenge failure");
                                    onErrorUI.run();
                                }
                            });
                            if (result.success != null) {
                                return JoinStage.CHALLENGE_EMAIL_SENT;
                            } else {
                                return s;
                            }
                        } catch (Native.NotLinked notLinked) {
                            notLinked.printStackTrace();
                        }
                    }
                    return s;
                });
            }).start();
        };
        final Runnable setSelectionBeforeDomain = () -> {
            String currentEmail = emailEditText.getText().toString();
            if (restriction.domain != null) {
                String domainSuffix = "@" + restriction.domain;
                if (currentEmail.length() >= domainSuffix.length()) {
                    if (emailEditText.getSelectionStart() == emailEditText.getSelectionEnd()) {
                        if (emailEditText.getSelectionStart() > currentEmail.length() - domainSuffix.length()) {
                            emailEditText.setSelection(currentEmail.length() - domainSuffix.length());
                        }
                    }
                }
            }
        };
        emailEditText.setOnEditorActionListener((v, i, ev) -> {
            requestEmailChallenge.run();
            return false;
        });
        //  focus before adding listener
        focusEmail.run();
        setSelectionBeforeDomain.run();
        emailEditText.setOnFocusChangeListener((view, b) -> {
            if (view.isEnabled()) {
                if (b) {
                    setSelectionBeforeDomain.run();
                }
            }
        });
        emailEditText.setOnClickListener(v -> setSelectionBeforeDomain.run());
        final AtomicReference<String> lastEmail = new AtomicReference<>(new String(emailEditText.getText().toString()));
        if (!restriction.isSatisfiedByEmail(lastEmail.get())) {
            if (restriction.domain != null) {
                lastEmail.set("@" + restriction.domain);
            } else if (restriction.emails != null && restriction.emails.length == 1) {
                lastEmail.set(restriction.emails[0]);
            } else {
                lastEmail.set("");
            }
            emailEditText.setText(lastEmail.get());
        }

        final Runnable updateTextColor = () -> {
            if (Email.verifyEmailPattern.matcher(emailEditText.getText()).matches()) {
                emailEditText.setTextColor(getContext().getColor(R.color.appGreen));
            } else {
                emailEditText.setTextColor(getContext().getColor(R.color.appGray));
            }
        };
        updateTextColor.run();
        final TextWatcher domainCheck = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastEmail.set(s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTextColor.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (restriction.domain != null) {
                    if (!s.toString().endsWith("@" + restriction.domain)) {
                        emailEditText.removeTextChangedListener(this);
                        s.clear();
                        s.append(lastEmail.get());
                        setSelectionBeforeDomain.run();
                        emailEditText.addTextChangedListener(this);
                        Toast.makeText(getContext(), "Email must end with @" + restriction.domain, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        emailEditText.addTextChangedListener(domainCheck);

        resendButton.setOnClickListener(v -> {
            requestEmailChallenge.run();
        });

        changeButton.setOnClickListener(v -> {
            focusEmail.run();
        });

        submitButton.setOnClickListener(v -> requestEmailChallenge.run());
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        progress = new JoinTeamProgress(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        progress = null;
    }
}
