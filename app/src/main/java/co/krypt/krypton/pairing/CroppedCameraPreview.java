package co.krypt.krypton.pairing;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * Created by Kevin King on 12/28/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class CroppedCameraPreview extends FrameLayout {
    private static final String TAG = "CroppedCameraPreview";
    private CameraPreview cameraPreview;

    public CroppedCameraPreview(Context context) {
        super(context);
    }

    public CroppedCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CroppedCameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CroppedCameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPreview(CameraPreview preview) {
        this.cameraPreview = preview;
        addView(preview);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (cameraPreview != null && cameraPreview.getPreviewSize() != null) {
            cameraPreview.layout(0, 0, cameraPreview.getPreviewSize().height, cameraPreview.getPreviewSize().width);
            Log.v(TAG, "cropping");
        } else {
            super.onLayout(changed, left, top, right, bottom);
            Log.v(TAG, "no cameraPreview size");
        }
    }
}
