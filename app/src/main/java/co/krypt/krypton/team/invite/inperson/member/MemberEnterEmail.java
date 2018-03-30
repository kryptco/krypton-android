package co.krypt.krypton.team.invite.inperson.member;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.R;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.silo.IdentityService.GetProfile;
import co.krypt.krypton.silo.IdentityService.GetProfileResult;
import co.krypt.krypton.uiutils.Email;
import co.krypt.krypton.uiutils.Transitions;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class MemberEnterEmail extends Fragment{
    private static final String TAG = "MemberEnterEmail";

    private AppCompatEditText email;
    private AppCompatButton nextButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_invite_inperson_member_enter_email, container, false);

        email = v.findViewById(R.id.memberEmailInPerson);

        email.setText("loading...");
        email.setTextColor(getResources().getColor(R.color.appGray, null));
        email.setEnabled(false);

        Email.colorValidEmail(email);

        nextButton = v.findViewById(R.id.inpersonEmailNext);
        nextButton.setOnClickListener(b -> {
            EventBus.getDefault().unregister(this);
            nextButton.setEnabled(false);
            new Thread(() -> {
                try {
                    new MeStorage(getContext()).setEmail(email.getText().toString().trim());
                } catch (CryptoException e) {
                    co.krypt.krypton.uiutils.Error.shortToast(getContext(), "Failed to set email: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                getActivity().runOnUiThread(() ->
                        Transitions.beginSlide(this)
                                .replace(R.id.fragmentOverlay, new MemberQR())
                                .addToBackStack(null)
                                .commitAllowingStateLoss()
                );
            }).start();
        });
        nextButton.setEnabled(false);

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new GetProfile(getContext()));
        return v;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void updateEmail(GetProfileResult r) {
        if (r.profile != null) {
            email.setText(r.profile.email);
            email.setTextColor(getResources().getColor(R.color.appBlack, null));
            email.setEnabled(true);
            nextButton.setEnabled(true);
        }
    }
}
