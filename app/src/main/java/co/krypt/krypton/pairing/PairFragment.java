package co.krypt.krypton.pairing;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonParseException;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import co.krypt.krypton.R;
import co.krypt.krypton.analytics.Analytics;
import co.krypt.krypton.crypto.TOTP;
import co.krypt.krypton.exception.CryptoException;
import co.krypt.krypton.exception.TransportException;
import co.krypt.krypton.onboarding.OnboardingActivity;
import co.krypt.krypton.protocol.JSON;
import co.krypt.krypton.protocol.PairingQR;
import co.krypt.krypton.silo.Silo;
import co.krypt.krypton.team.Sigchain;
import co.krypt.krypton.team.invite.inperson.member.MemberEnterEmail;
import co.krypt.krypton.team.invite.inperson.member.MemberScan;
import co.krypt.krypton.uiutils.Transitions;
import co.krypt.kryptonite.MainActivity;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairFragment extends Fragment implements PairDialogFragment.PairListener {
    private static final String TAG = "PairFragment";
    public static final String PAIRING_SUCCESS_ACTION = "co.krypt.android.action.PAIRING_SUCCESS";

    // This threadpool should only contain one thread. It is used to perform background tasks
    // synchronized amongst each other.
    private ThreadPoolExecutor threadPool;

    private AtomicBoolean fragmentVisible = new AtomicBoolean();

    private BarcodeView barcodeView;
    private View pairingStatusView;
    private TextView pairingStatusText;

    private ConstraintLayout cameraPermissionInfoLayout;
    private TextView cameraPermissionHeader;
    private TextView cameraPermissionText;
    private Button requestCameraPermissionButton;
    private final BroadcastReceiver permissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.CAMERA_PERMISSION_GRANTED_ACTION:
                    onCameraPermissionGranted();
                    break;
            }
        }
    };

    private AtomicReference<PairingQR> pendingPairingQR = new AtomicReference<>();
    private boolean isTotpDialogOpen = false;

    public PairingQR getPendingPairingQR() {
        return pendingPairingQR.get();
    }

    public PairFragment() {
        // 1 core thread, 1 max thread, 60 second timeout.
        threadPool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public void onPairingScanned(PairingQR pairingQR) {
        if (pendingPairingQR.compareAndSet(null, pairingQR)) {
            PairDialogFragment pairDialog = new PairDialogFragment();
            pairDialog.setTargetFragment(this, 0);
            pairDialog.show(getFragmentManager(), "PAIR_NEW_DEVICE");
        }
    }

    public synchronized void onTOTPScanned(String totpURI) {
        if(!isTotpDialogOpen) {
            FragmentActivity activity = getActivity();
            MainActivity mainActivity;
            if (!(activity instanceof MainActivity)) {
                // If we're in an onboarding activity, the pair fragment should only be handling pairings, not TOTP.
                return;
            } else {
                mainActivity = (MainActivity) activity;
            }
            try {
                if (TOTP.checkAccountExists(getContext(), totpURI)) {
                    Toast.makeText(getContext(), "This account has already been registered", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Bad QR code", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Would you like to register this Backup Code?");
            builder.setNegativeButton("No", (dialog, id) -> {
                return;
            });
            builder.setPositiveButton("Yes", (dialog, id) -> {
                try {
                    TOTP.registerTOTPAccount(getContext(), totpURI);
                } catch(URISyntaxException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Bad QR code", Toast.LENGTH_SHORT).show();
                } catch (SQLException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Unable to save QR code", Toast.LENGTH_SHORT).show();
                }
                mainActivity.setActiveTab(MainActivity.CODES_FRAGMENT_POSITION);
            });
            builder.setOnDismissListener(dialogInterface -> {
                isTotpDialogOpen = false;
            });
            isTotpDialogOpen = true;
            builder.create().show();
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PairFragment.
     */
    public static PairFragment newInstance() {
        PairFragment fragment = new PairFragment();
        return fragment;
    }

    private static void onClickRequestCameraPermission(Activity context) {
        switch (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.CAMERA)) {
            case PackageManager.PERMISSION_DENIED:
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.CAMERA},
                        MainActivity.CAMERA_PERMISSION_REQUEST);
        }
    }

    private void onCameraPermissionGranted() {
        Log.i(TAG, "camera permission granted");
        refreshCameraPermissionInfoVisibility();
    }

    private void refreshCameraPermissionInfoVisibility() {
        if (cameraPermissionInfoLayout == null) {
            Log.v(TAG, "layout refreshed before onViewCreate()");
            return;
        }
        Log.v(TAG, "updating camera permission layout");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(cameraPermissionInfoLayout.getContext(),
                        android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionInfoLayout.setVisibility(View.VISIBLE);
                } else {
                    cameraPermissionInfoLayout.setVisibility(View.GONE);
                }
            }
        };
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            r.run();
        } else {
            if (!cameraPermissionInfoLayout.post(r)) {
                Log.e(TAG, "could not post to cameraPermissionInfoLayout");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_pair, container, false);

        barcodeView = (BarcodeView) rootView.findViewById(R.id.camera_preview);
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    if(result.getText().startsWith("otpauth://")) {
                        onTOTPScanned(result.getText());
                        return;
                    }
                    //  handle in-person team invite
                    try {
                        Sigchain.QRPayload qr = JSON.fromJson(result.getText(), Sigchain.QRPayload.class);
                        if (qr != null && qr.adminQRPayload != null && MemberScan.lastScannedPayload.compareAndSet(null, qr.adminQRPayload)) {
                            Transitions.beginFade(PairFragment.this)
                                    .addToBackStack(null)
                                    .replace(R.id.fragmentOverlay, new MemberEnterEmail())
                                    .commitAllowingStateLoss();
                            return;
                        }
                    } catch (JsonParseException e) {

                    }
                    try {
                        PairingQR pairingQR = PairingQR.parseJson(result.getText());
                        if(pairingQR != null && pairingQR.workstationPublicKey != null) {
                            Log.i(TAG, "found pairingQR: " + Base64.encodeToString(pairingQR.workstationPublicKey, Base64.DEFAULT));
                            onPairingScanned(pairingQR);
                        }
                    } catch (JsonParseException e) {
                        Log.e(TAG, "could not parse QR code", e);
                    }
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });

        pairingStatusView = rootView.findViewById(R.id.pairingStatusLayout);
        pairingStatusText = (TextView) rootView.findViewById(R.id.pairingStatusText);

        cameraPermissionInfoLayout = (ConstraintLayout) rootView.findViewById(R.id.cameraPermissionInfo);
        cameraPermissionInfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRequestCameraPermission(getActivity());
            }
        });

        requestCameraPermissionButton = (Button) rootView.findViewById(R.id.requestCameraPermissionButton);
        requestCameraPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRequestCameraPermission(getActivity());
            }
        });

        refreshCameraPermissionInfoVisibility();

        cameraPermissionHeader = (TextView) rootView.findViewById(R.id.cameraPermissionTitle);
        cameraPermissionText = (TextView) rootView.findViewById(R.id.cameraPermissionExplanation);

        if (getActivity() instanceof OnboardingActivity) {
            cameraPermissionHeader.setTextSize(16);
            cameraPermissionText.setTextSize(14);
        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter permissionFilter = new IntentFilter();
        permissionFilter.addAction(MainActivity.CAMERA_PERMISSION_GRANTED_ACTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(permissionReceiver, permissionFilter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        IntentFilter permissionFilter = new IntentFilter();
        permissionFilter.addAction(MainActivity.CAMERA_PERMISSION_GRANTED_ACTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(permissionReceiver, permissionFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(permissionReceiver);
    }


    private void updateCamera(Context context) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            if (fragmentVisible.get()) {
                if (barcodeView != null) {
                    barcodeView.resume();
                }
                refreshCameraPermissionInfoVisibility();
            } else if (!fragmentVisible.get()) {
                if (barcodeView != null) {
                    barcodeView.pause();
                }
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);

        fragmentVisible.set(visible);
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                updateCamera(getContext());
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                updateCamera(getContext());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // We need to let go of the camera when pausing, regardless of visibility.
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    private void onPairingSuccess(final Pairing pairing) {
        Intent successIntent = new Intent(PAIRING_SUCCESS_ACTION);
        successIntent.putExtra("deviceName", pairing.workstationName);
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(successIntent);
            new Analytics(context).postEvent("device", "pair", "success", null, false);
        }

        pairingStatusText.setText("Paired with\n" + pairing.workstationName);
        pairingStatusView.setVisibility(View.VISIBLE);

        pairingStatusView.postDelayed(new Runnable() {
            @Override
            public void run() {
                pendingPairingQR.set(null);
                pairingStatusView.setVisibility(View.INVISIBLE);
            }
        }, 2000);
    }

    private void onPairingFailure(final Pairing pairing) {
        Silo silo = Silo.shared(getContext());
        silo.unpair(pairing, false);

        pendingPairingQR.set(null);

        new Analytics(silo.context).postEvent("device", "pair", "failed", null, false);

        pairingStatusText.setText("Failed to pair with\n" + pairing.workstationName);
        pairingStatusView.setVisibility(View.VISIBLE);

        pairingStatusView.postDelayed(new Runnable() {
            @Override
            public void run() {
                pairingStatusView.setVisibility(View.INVISIBLE);
            }
        }, 2000);
    }

    public void pairingReady(final Pairing pairing) {
        final long pairTime = System.currentTimeMillis();
        pairingStatusText.setText("Pairing with\n" + pairing.workstationName);
        pairingStatusView.setVisibility(View.VISIBLE);

        final Runnable checker = new Runnable() {
            @Override
            public void run() {
                if (isDetached()) {
                    return;
                }
                if (Silo.shared(getContext()).hasActivity(pairing)) {
                    onPairingSuccess(pairing);
                } else if ((System.currentTimeMillis() - pairTime) >= 20000) {
                    onPairingFailure(pairing);
                } else {
                    // Wait a second to check again.
                    pairingStatusView.postDelayed(this, 1000);
                }
            }
        };

        checker.run();
    }

    @Override
    public void pair() {
        Log.i(TAG, "pair");
        final PairingQR qr = pendingPairingQR.get();
        if (qr == null) {
            Log.e(TAG, "pendingQR null");
            return;
        }

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // This part contains crypto, so we run on a background thread. If the crypto
                    // is proven to be fast enough for the UI thread on slower devices, then this
                    // thread should be removed to greatly simplify the code.
                    final Pairing pairing = Pairing.generate(getContext(), qr);
                    Silo.shared(getContext()).pair(pairing);

                    pairingStatusView.post(new Runnable() {
                        @Override
                        public void run() {
                            pairingReady(pairing);
                        }
                    });
                } catch (CryptoException | TransportException e) {
                    pendingPairingQR.set(null);
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void cancel() {
        pendingPairingQR.set(null);
    }
}
