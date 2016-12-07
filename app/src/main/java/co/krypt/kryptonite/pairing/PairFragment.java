package co.krypt.kryptonite.pairing;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import co.krypt.kryptonite.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairFragment extends Fragment implements Camera.PreviewCallback {
    private static final String TAG = "PairFragment";

    private Camera mCamera;
    private int previewWidth;
    private int previewHeight;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private boolean visible;

    private PairScanner pairScanner;

    public PairFragment() {
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
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PairFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PairFragment newInstance() {
        PairFragment fragment = new PairFragment();
        return fragment;
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
        preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(getContext());
        preview.addView(mPreview);

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
    }

    synchronized private void startCamera() {
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
                            pairScanner = new PairScanner(getContext(), previewHeight, previewWidth);
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
        startCamera();
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
}
