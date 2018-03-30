package co.krypt.krypton.team.invite.inperson.admin;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;

import co.krypt.krypton.R;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.uiutils.Error;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class AdminVerify extends Fragment{
    private static final String TAG = "AdminVerify";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_invite_inperson_admin_confirm_email, container, false);

        Sigchain.MemberQRPayload qr = AdminScan.lastScannedPayload.get();
        if (qr != null) {
            AppCompatTextView email = v.findViewById(R.id.confirmEmailLabel);
            email.setText(qr.email);

            AppCompatButton confirmButton = v.findViewById(R.id.confirmMember);
            confirmButton.setOnClickListener(b -> {
                EventBus.getDefault().post(
                        new TeamService.RequestTeamOperation(
                                new Sigchain.RequestableTeamOperation(
                                        new Sigchain.DirectInvitation(qr.publicKey, qr.email)
                                ),
                                C.withStatusCallback(getActivity(), this::onInviteCreated)
                        )
                );
            });

            AppCompatButton cancelButton = v.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(b -> {
                getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            });
        } else {
            Error.shortToast(getContext(), "Please scan the new member's QR code again.");
            getFragmentManager().popBackStack();
        }



        return v;
    }

    public void onInviteCreated(Sigchain.NativeResult<Sigchain.TeamOperationResponse> r) {
        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

}
