package co.krypt.kryptonite.pairing;


import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera preview class https://developer.android.com/guide/topics/media/camera.html#preview-layout */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;

    private Camera.Size previewSize;

    private static final String TAG = "CAMERA_PREVIEW";

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {}
    public void surfaceDestroyed(SurfaceHolder holder) {}
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}

    public void setup(Camera camera) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera.setPreviewDisplay(mHolder);
            previewSize = camera.getParameters().getPreviewSize();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void clear() {
        // This trick rebuilds the surface, making it black again. Starting with a black surface
        // while the camera loads looks nicer than having an odd frozen image from previous run.
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mHolder.setFormat(PixelFormat.OPAQUE);
    }

    public Camera.Size getPreviewSize() {
        return previewSize;
    }
}

