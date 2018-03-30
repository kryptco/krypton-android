package co.krypt.krypton.team.invite.inperson.member;

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
import android.widget.ProgressBar;

import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.atomic.AtomicReference;

import co.krypt.krypton.R;
import co.krypt.krypton.me.MeStorage;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.Profile;
import co.krypt.krypton.team.Native;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.TeamDataProvider;
import co.krypt.krypton.team.TeamService;
import co.krypt.krypton.team.TeamService.C;
import co.krypt.krypton.team.onboarding.join.JoinTeamProgress;
import co.krypt.krypton.uiutils.Error;
import co.krypt.krypton.uiutils.Transitions;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class MemberQR extends Fragment{

    private AppCompatImageView qr = null;
    private ProgressBar loadQRProgress;
    private ProgressBar joinProgress;

    public static class QRPayloads {
        public final Sigchain.MemberQRPayload member;

        public final Sigchain.AdminQRPayload admin;

        public QRPayloads(Sigchain.MemberQRPayload member, Sigchain.AdminQRPayload admin) {
            this.member = member;
            this.admin = admin;
        }
    }

    public static final AtomicReference<QRPayloads> qrPayloads = new AtomicReference<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_invite_inperson_member_qr, container, false);

        qrPayloads.set(null);

        qr = v.findViewById(R.id.memberQRCode);

        AppCompatButton cancelButton = v.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v_ -> {
            Transitions.beginFade(this)
                    .remove(this)
                    .commitAllowingStateLoss();
        });

        loadQRProgress = v.findViewById(R.id.loadQRProgressBar);
        joinProgress = v.findViewById(R.id.memberJoinProgress);
        joinProgress.setAlpha(0);

        Sigchain.AdminQRPayload adminQRPayload = MemberScan.lastScannedPayload.get();
        if (adminQRPayload == null) {
            getFragmentManager().popBackStackImmediate();
        } else {
            new Thread(() ->  {
                Profile p = new MeStorage(getContext()).load();
                if (p == null) {
                    Error.shortToast(getContext(), "Error loading profile, first generate your Krypton SSH key");
                } else {
                    EventBus.getDefault().register(this);
                    EventBus.getDefault().post(new TeamService.GenerateClient(C.background(getContext()), new Sigchain.GenerateClientInput(
                            p,
                            adminQRPayload.teamPublicKey,
                            adminQRPayload.lastBlockHash
                    )));
                }
            }).start();
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCreateClient(TeamService.GenerateClientResult r) {
        if (r.c.error != null) {
            Error.shortToast(getContext(), r.c.error);
            Transitions.beginFade(this).remove(this).commitAllowingStateLoss();
            return;
        }
        Sigchain.Identity identity = r.c.success;
        Sigchain.MemberQRPayload memberQRPayload = new Sigchain.MemberQRPayload(
                identity.email,
                identity.publicKey
        );
        Sigchain.QRPayload qrPayload = new Sigchain.QRPayload(memberQRPayload);
        try {
            BitMatrix qrData = new QRCodeWriter().encode(JSON.toJson(qrPayload), BarcodeFormat.DATA_MATRIX.QR_CODE, 500, 500);
            qr.setImageBitmap(new BarcodeEncoder().createBitmap(qrData));
            qrPayloads.set(new QRPayloads(memberQRPayload, MemberScan.lastScannedPayload.get()));

            loadQRProgress.setAlpha(0);
            joinProgress.animate().setDuration(1000).alpha(1).start();

            new JoinTeamProgress(getContext()).updateTeamData((s, d) -> {
                d.identity = identity;
                d.teamName = qrPayloads.get().admin.teamName;
                return s;
            });

        } catch (WriterException e) {
            e.printStackTrace();
            Error.shortToast(getContext(), "Error creating QRCode");
            Transitions.beginFade(this).remove(this).commitAllowingStateLoss();
            return;
        }
        EventBus.getDefault().post(new PollRead());
    }

    private static class PollRead {
        int tries = 0;
    }

    private static class Fail {
        public final String error;

        public Fail(String error) {
            this.error = error;
        }
    }

    private static class Succeed {}

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void pollRead(PollRead p) {
        try {
            Sigchain.NativeResult<JsonObject> result = TeamDataProvider.tryRead(getContext());
            if (result.success != null) {
                EventBus.getDefault().post(new Succeed());
                return;
            }
            if (p.tries > 30) {
                EventBus.getDefault().post(new Fail(result.error));
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            p.tries++;
            EventBus.getDefault().post(p);
        } catch (Native.NotLinked notLinked) {
            notLinked.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReadSuccess(Succeed _) {
        EventBus.getDefault().unregister(this);
        Transitions.beginSlide(this)
                .replace(R.id.fragmentOverlay, new MemberVerifyEmail())
                .commitAllowingStateLoss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReadFail(Fail _) {
        EventBus.getDefault().unregister(this);
        Error.shortToast(getContext(), "Failed to join team, please try again.");
        getFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
