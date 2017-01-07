package co.krypt.kryptonite.pairing;

import android.Manifest;
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

import co.krypt.kryptonite.MainActivity;
import co.krypt.kryptonite.R;
import co.krypt.kryptonite.exception.CryptoException;
import co.krypt.kryptonite.exception.TransportException;
import co.krypt.kryptonite.protocol.PairingQR;
import co.krypt.kryptonite.silo.Silo;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairFragment extends Fragment implements Camera.PreviewCallback, PairDialogFragment.PairListener {
    private static final String TAG = "PairFragment";

    private Camera mCamera;
    private int previewWidth;
    private int previewHeight;
    private CameraPreview mPreview;
    private CroppedCameraPreview preview;
    private boolean visible;

    private View pairingStatusView;
    private TextView pairingStatusText;

    private ConstraintLayout locationPermissionLayout;

    private ConstraintLayout cameraPermissionInfoLayout;
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
    private PairingQR pendingPairingQR;

    public PairingQR getPendingPairingQR() {
        return pendingPairingQR;
    }

    public PairFragment() {
    }

    public synchronized void onPairingScanned(PairingQR pairingQR) {
        if (pendingPairingQR == null) {
            pendingPairingQR = pairingQR;
            PairDialogFragment pairDialog = new PairDialogFragment();
            pairDialog.setTargetFragment(this, 0);
            pairDialog.show(getFragmentManager(), "PAIR_NEW_DEVICE");
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (visible && isResumed())
        {
            //Only manually call onResume if fragment is already visible
            //Otherwise allow natural fragment lifecycle to call onResume
            onResume();
        }
        Log.i(TAG, "visible: " + String.valueOf(visible));
        synchronized (this) {
            this.visible = visible;
        }
        refreshCameraPermissionInfoVisibility();
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

    private void onClickRequestCameraPermission() {
        switch (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.CAMERA)) {
            case PackageManager.PERMISSION_DENIED:
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA},
                        MainActivity.CAMERA_PERMISSION_REQUEST);
        }
    }

    private void onClickRequestLocationPermission() {
        switch (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            case PackageManager.PERMISSION_DENIED:
                ActivityCompat.requestPermissions(getActivity(),
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
        if (getActivity() == null || cameraPermissionInfoLayout == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionInfoLayout.setVisibility(View.VISIBLE);
        } else {
            cameraPermissionInfoLayout.setVisibility(View.GONE);
        }
    }

    private void refreshLocationPermissionInfoVisibility() {
        if (getActivity() == null || locationPermissionLayout == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLayout.setVisibility(View.VISIBLE);
        } else {
            locationPermissionLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
//        preview.addView(mPreview);

        cameraPermissionInfoLayout = (ConstraintLayout) rootView.findViewById(R.id.cameraPermissionInfo);
        refreshCameraPermissionInfoVisibility();

        requestCameraPermissionButton = (Button) rootView.findViewById(R.id.requestCameraPermissionButton);
        requestCameraPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRequestCameraPermission();
            }
        });

        locationPermissionLayout = (ConstraintLayout) rootView.findViewById(R.id.locationPermissionLayout);
        locationPermissionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickRequestLocationPermission();
            }
        });
        refreshLocationPermissionInfoVisibility();

        return rootView;
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            if (c != null) {
                c.setDisplayOrientation(90);
            }
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
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
    public void onDetach() {
        super.onDetach();
        getContext().unregisterReceiver(permissionReceiver);
    }

    synchronized private void startCamera(final Context context) {
        final PairFragment self = this;
        if (mCamera != null) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                final Camera camera = getCameraInstance();
                if (camera == null) {
                    return;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (self) {
                            mCamera = camera;
                            previewWidth = camera.getParameters().getPreviewSize().width;
                            previewHeight = camera.getParameters().getPreviewSize().height;
                            mPreview.setCamera(mCamera);
                            camera.setPreviewCallback(self);
                            pairScanner = new PairScanner(context, self, previewHeight, previewWidth);

                            preview.requestLayout();
                        }
                    }
                });

            }
        }).start();
    }

    synchronized private void stopCamera() {
        final PairFragment self = this;
        new Thread(new Runnable() {
            public void run() {
                synchronized (self) {
                    if (mCamera != null) {
                        mCamera.stopPreview();
                        mCamera.setPreviewCallback(null);
                        mCamera.release();
                        mCamera = null;
                    }
                    if (pairScanner != null) {
                        pairScanner.stop();
                        pairScanner = null;
                    }
                }
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "resume");
        startCamera(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "pause");
        stopCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        boolean visible;
        synchronized (this) {
            visible = this.visible;
            if (!visible) {
                return;
            }
            if (pairScanner != null) {
                pairScanner.pushFrame(data);
            }
        }
    }

    private synchronized void onPairingSuccess(final Pairing pairing) {
        pendingPairingQR = null;
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        pairingStatusText.setText("Paired with\n" + pairing.workstationName);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            pairingStatusView.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        pairingStatusView.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    private synchronized void onPairingFailure(final Pairing pairing) {
        Silo.shared(getContext()).unpair(pairing, false);
        pendingPairingQR = null;
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        pairingStatusText.setText("Failed to pair with\n" + pairing.workstationName);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            pairingStatusView.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        pairingStatusView.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Override
    public synchronized void pair() {
        Log.i(TAG, "pair");
        if (pendingPairingQR != null) {
            final PairingQR qr = pendingPairingQR;
            try {
                final Pairing pairing = Silo.shared(getContext()).pair(qr);
                final long pairTime = System.currentTimeMillis();
                new Handler(Looper.getMainLooper()).post(
                        new Runnable() {
                            @Override
                            public void run() {
                                pairingStatusText.setText("Pairing with\n" + qr.workstationName);
                                pairingStatusView.setVisibility(View.VISIBLE);
                            }
                        });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while((System.currentTimeMillis() - pairTime) < 20000) {
                            if (Silo.shared(getContext()).hasActivity(pairing)) {
                                onPairingSuccess(pairing);
                                return;
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return;
                            }
                        }
                        onPairingFailure(pairing);
                    }
                }).start();
            } catch (CryptoException | TransportException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "pendingQR null");
        }
    }

    @Override
    public synchronized void cancel() {
        pendingPairingQR = null;
    }
}
