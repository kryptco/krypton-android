package co.krypt.kryptonite;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PairFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PairFragment extends Fragment implements Camera.PreviewCallback {
    private static final String TAG = "PairFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private BarcodeDetector detector;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Camera mCamera;
    private int previewWidth;
    private int previewHeight;
    private CameraPreview mPreview;
    private FrameLayout preview;

    public PairFragment() {
        // Required empty public constructor
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
        if (visible) {
            startCamera();
        } else {
            stopCamera();
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
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        detector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        if(!detector.isOperational()){
            Log.e(TAG, "Could not set up the detector!");
            return;
        }
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
                        mCamera.release();
                        mCamera = null;
                    }
                }
            }
        }).start();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        Allocation bmData = renderScriptNV21ToRGBA888(
                getContext(),
                previewWidth,
                previewHeight,
                data);
        bmData.copyTo(bitmap);
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Barcode> barcodes = detector.detect(frame);
        if (barcodes.size() > 0) {
            Log.i(TAG, "found " + barcodes.size() + " barcodes");
        }
    }

    public Allocation renderScriptNV21ToRGBA888(Context context, int width, int height, byte[] nv21) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }
}
