package co.krypt.kryptonite.pairing;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.analytics.Analytics;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.onboarding.OnboardingActivity;
import co.krypt.kryptonite.protocol.PairingQR;
import co.krypt.kryptonite.silo.Silo;


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

    private Camera mCamera;
    private CameraPreview mPreview;
    private CroppedCameraPreview preview;
    private AtomicBoolean fragmentVisible = new AtomicBoolean();

    private View pairingStatusView;
    private TextView pairingStatusText;

    private ConstraintLayout locationPermissionLayout;

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
                case MainActivity.LOCATION_PERMISSION_GRANTED_ACTION:
                    onLocationPermissionGranted();
                    break;
            }
        }
    };

    private PairScanner pairScanner;
    private AtomicReference<PairingQR> pendingPairingQR = new AtomicReference<>();

    public PairingQR getPendingPairingQR() {
        return pendingPairingQR.get();
    }

    public PairFragment() {
        // 1 core thread, 1 max thread, 60 second timeout.
        threadPool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void onPairingScanned(PairingQR pairingQR) {
        if (pendingPairingQR.compareAndSet(null, pairingQR)) {
            PairDialogFragment pairDialog = new PairDialogFragment();
            pairDialog.setTargetFragment(this, 0);
            pairDialog.show(getFragmentManager(), "PAIR_NEW_DEVICE");
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

    private static void onClickRequestLocationPermission(Activity context) {
        switch (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            case PackageManager.PERMISSION_DENIED:
                ActivityCompat.requestPermissions(context,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MainActivity.LOCATION_PERMISSION_REQUEST);
        }
    }

    private void onCameraPermissionGranted() {
        Log.i(TAG, "camera permission granted");
        refreshCameraPermissionInfoVisibility();
    }

    private void onLocationPermissionGranted() {
        Log.i(TAG, "location permission granted");
        refreshLocationPermissionInfoVisibility();
    }

    private void refreshCameraPermissionInfoVisibility() {
        Context context;
        if (getParentFragment() != null) {
            context = getParentFragment().getContext();
        } else if (getActivity() != null) {
            context = getActivity();
        } else {
            return;
        }
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionInfoLayout.setVisibility(View.VISIBLE);
        } else {
            cameraPermissionInfoLayout.setVisibility(View.GONE);
        }
        refreshLocationPermissionInfoVisibility();
    }

    private void refreshLocationPermissionInfoVisibility() {
        Context context;
        if (getParentFragment() != null) {
            context = getParentFragment().getContext();
        } else if (getActivity() != null) {
            context = getActivity();
        } else {
            return;
        }
        if ((ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                &&
                (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            locationPermissionLayout.setVisibility(View.VISIBLE);
        } else {
            locationPermissionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_pair, container, false);
        preview = (CroppedCameraPreview) rootView.findViewById(R.id.camera_preview);
        pairingStatusView = rootView.findViewById(R.id.pairingStatusLayout);
        pairingStatusText = (TextView) rootView.findViewById(R.id.pairingStatusText);
        mPreview = new CameraPreview(getContext());
        preview.setPreview(mPreview);

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

        locationPermissionLayout = (ConstraintLayout) rootView.findViewById(R.id.locationPermissionLayout);
        locationPermissionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRequestLocationPermission(getActivity());
            }
        });

        refreshCameraPermissionInfoVisibility();
        refreshLocationPermissionInfoVisibility();

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
        permissionFilter.addAction(MainActivity.LOCATION_PERMISSION_GRANTED_ACTION);
        context.registerReceiver(permissionReceiver, permissionFilter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        IntentFilter permissionFilter = new IntentFilter();
        permissionFilter.addAction(MainActivity.CAMERA_PERMISSION_GRANTED_ACTION);
        permissionFilter.addAction(MainActivity.LOCATION_PERMISSION_GRANTED_ACTION);
        getContext().registerReceiver(permissionReceiver, permissionFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getContext().unregisterReceiver(permissionReceiver);
    }

    private void startCamera(final Context context) {
        if (mCamera != null) {
            return;
        }

        Log.v(TAG, "starting camera");

        try {
            mCamera = Camera.open();
            if (mCamera == null) {
                return;
            }

            // Configure camera
            mCamera.setDisplayOrientation(90);
            List<String> focusModes = mCamera.getParameters().getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                Camera.Parameters params = mCamera.getParameters();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.setParameters(params);
            }

            // Setup PairScanner
            int previewWidth = mCamera.getParameters().getPreviewSize().width;
            int previewHeight = mCamera.getParameters().getPreviewSize().height;

            pairScanner = new PairScanner(context, this, previewHeight, previewWidth);
            mCamera.setPreviewCallback(pairScanner);

            // Start preview
            mPreview.setup(mCamera);
            mCamera.startPreview();

        } catch (RuntimeException e) {
            Log.d(TAG, "Error setting up camera: " + e.getMessage());
            e.printStackTrace();
            FirebaseCrash.report(e);
        }
    }

    private void stopCamera(boolean clear) {
        if (mCamera != null) {
            Log.v(TAG, "stopping camera");
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if (pairScanner != null) {
            pairScanner.stop();
            pairScanner = null;
        }
        if (clear && mPreview != null) {
            mPreview.clear();
        }
    }

    private void updateCamera(Context context) {
        if (fragmentVisible.get() && mCamera == null) {
            startCamera(context);
            refreshCameraPermissionInfoVisibility();
        } else if (!fragmentVisible.get() && mCamera != null) {
            stopCamera(true);
        }
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
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                // We need to let go of the camera when pausing, regardless of visibility.
                stopCamera(false);
            }
        });
    }

    private void onPairingSuccess(final Pairing pairing) {
        Intent successIntent = new Intent(PAIRING_SUCCESS_ACTION);
        successIntent.putExtra("deviceName", pairing.workstationName);
        getContext().sendBroadcast(successIntent);
        new Analytics(getContext()).postEvent("device", "pair", "success", null, false);

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
                    final Pairing pairing = Pairing.generate(qr);
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
