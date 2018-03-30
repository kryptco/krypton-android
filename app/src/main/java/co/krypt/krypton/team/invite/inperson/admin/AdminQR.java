package co.krypt.krypton.team.invite.inperson.admin;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import co.krypt.krypton.R;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.uiutils.Error;
import co.krypt.krypton.uiutils.Transitions;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 *
 * Flow:
 *  Member scan Admin
 *  Admin click Next
 *  Admin scan Member
 *  Admin verify Member's email
 *  Member polls until invite posted to chain and read/accept invite succeed
 */

public class AdminQR extends Fragment{

    private AppCompatImageView qr = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_invite_inperson_admin_qr, container, false);

        qr = v.findViewById(R.id.adminQRCode);

        AppCompatButton nextButton = v.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v_ -> {
            Transitions.beginSlide(this)
                    .replace(R.id.fragmentOverlay, new AdminScan())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        AppCompatButton cancelButton = v.findViewById(R.id.cancelInPersonAdmin);
        cancelButton.setOnClickListener(b -> getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE));

        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new TeamService.GetTeamHomeData(C.background(getContext())));
        return v;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onGetTeamHomeData(TeamService.GetTeamHomeDataResult d) {
        if (d.r.error != null) {
            Error.shortToast(getContext(), d.r.error);
            Transitions.beginFade(this).remove(this).commitAllowingStateLoss();
            return;
        }
        Sigchain.TeamHomeData teamHomeData = d.r.success;
        Sigchain.QRPayload qrPayload = new Sigchain.QRPayload(new Sigchain.AdminQRPayload(
                teamHomeData.teamPublicKey,
                teamHomeData.lastBlockHash,
                teamHomeData.name
        ));
        try {
            BitMatrix qrData = new QRCodeWriter().encode(JSON.toJson(qrPayload), BarcodeFormat.DATA_MATRIX.QR_CODE, 500, 500);
            qr.setImageBitmap(new BarcodeEncoder().createBitmap(qrData));
        } catch (WriterException e) {
            e.printStackTrace();
            Error.shortToast(getContext(), "Error creating QRCode");
            Transitions.beginFade(this).remove(this).commitAllowingStateLoss();
            return;
        }
    }
}
