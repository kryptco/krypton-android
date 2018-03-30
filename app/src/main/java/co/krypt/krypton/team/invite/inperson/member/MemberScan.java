package co.krypt.krypton.team.invite.inperson.member;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonParseException;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import co.krypt.krypton.R;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.uiutils.Error;
import co.krypt.krypton.uiutils.Transitions;

/**
 * Created by Kevin King on 2/6/18.
 * Copyright 2018. KryptCo, Inc.
 */

public class MemberScan extends Fragment{

    public static final AtomicReference<Sigchain.AdminQRPayload> lastScannedPayload = new AtomicReference<>();
    private static final String TAG = "MemberScan";

    private BarcodeView barcodeView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_teams_invite_inperson_scan, container, false);

        lastScannedPayload.set(null);

        barcodeView = v.findViewById(R.id.camera_preview_in_person_scan);
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    try {
                        Sigchain.AdminQRPayload qr = JSON.fromJson(result.getText(), Sigchain.QRPayload.class).adminQRPayload;
                        if (qr == null) {
                            Error.shortToast(getContext(), "Make sure you are scanning an admin that is already on a team.");
                            return;
                        }
                        if (lastScannedPayload.compareAndSet(null, qr)) {
                            Transitions.beginSlide(MemberScan.this)
                                    .addToBackStack(null)
                                    .replace(R.id.fragmentOverlay, new MemberEnterEmail())
                                    .commitAllowingStateLoss();
                        } else {
                            Log.i(TAG, "not first scan");
                        }
                    } catch (JsonParseException e) {
                        Log.e(TAG, "could not parse QR code", e);
                        Error.shortToast(getContext(), "Invalid QR code");
                    }
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });

        AppCompatTextView detailText = v.findViewById(R.id.inPersonScanDetail);
        detailText.setText("Scan your team admin's QR code to join their team.");

        return v;
    }

    @Override
    public void onPause() {
        barcodeView.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        barcodeView.resume();
        super.onResume();
    }
}
