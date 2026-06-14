package com.liming.livestage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("deprecation")
public final class CameraPreviewView extends TextureView implements TextureView.SurfaceTextureListener {
    public static final int SWITCH_UNAVAILABLE = -1;
    public static final int SWITCHED_BACK = 0;
    public static final int SWITCHED_FRONT = 1;

    private final Activity activity;
    private Camera camera;
    private int cameraId = -1;
    private int lensFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private boolean starting;

    public CameraPreviewView(Context context) { this(context, null); }

    public CameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("CameraPreviewView requires an Activity context");
        }
        activity = (Activity) context;
        setSurfaceTextureListener(this);
    }

    public synchronized void start() {
        if (camera != null || starting) return;
        if (isAvailable()) openCamera(getSurfaceTexture());
    }

    public synchronized void stop() {
        starting = false;
        Camera current = camera;
        camera = null;
        cameraId = -1;
        if (current != null) {
            try { current.setPreviewCallback(null); } catch (RuntimeException ignored) { }
            try { current.stopPreview(); } catch (RuntimeException ignored) { }
            try { current.release(); } catch (RuntimeException ignored) { }
        }
    }

    public synchronized int toggleLens() {
        int wanted = lensFacing == Camera.CameraInfo.CAMERA_FACING_FRONT
                ? Camera.CameraInfo.CAMERA_FACING_BACK
                : Camera.CameraInfo.CAMERA_FACING_FRONT;
        if (findCameraId(wanted) < 0) return SWITCH_UNAVAILABLE;
        lensFacing = wanted;
        stop();
        start();
        return lensFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? SWITCHED_FRONT : SWITCHED_BACK;
    }

    public boolean hasAnyCamera() { return Camera.getNumberOfCameras() > 0; }

    private synchronized void openCamera(SurfaceTexture texture) {
        if (texture == null || camera != null || starting) return;
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        starting = true;
        try {
            int selected = findCameraId(lensFacing);
            if (selected < 0) {
                selected = Camera.getNumberOfCameras() > 0 ? 0 : -1;
                if (selected >= 0) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(selected, info);
                    lensFacing = info.facing;
                }
            }
            if (selected < 0) return;

            Camera opened = Camera.open(selected);
            camera = opened;
            cameraId = selected;
            Camera.Parameters parameters = opened.getParameters();
            Camera.Size preview = choosePreviewSize(parameters.getSupportedPreviewSizes(), getWidth(), getHeight());
            if (preview != null) parameters.setPreviewSize(preview.width, preview.height);
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            opened.setParameters(parameters);
            setDisplayOrientation(opened, selected);
            opened.setPreviewTexture(texture);
            opened.startPreview();
            opened.setErrorCallback((error, failedCamera) -> post(this::stop));
        } catch (IOException | RuntimeException ignored) {
            stop();
        } finally {
            starting = false;
        }
    }

    private int findCameraId(int facingWanted) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == facingWanted) return i;
        }
        return -1;
    }

    private Camera.Size choosePreviewSize(List<Camera.Size> choices, int viewWidth, int viewHeight) {
        if (choices == null || choices.isEmpty()) return null;
        int targetW = Math.max(viewWidth, viewHeight);
        int targetH = Math.min(viewWidth, viewHeight);
        if (targetW <= 0 || targetH <= 0) { targetW = 1280; targetH = 720; }
        float targetRatio = targetW / (float) targetH;
        Camera.Size best = choices.get(0);
        double bestScore = Double.MAX_VALUE;
        for (Camera.Size size : choices) {
            long area = (long) size.width * size.height;
            if (area > 1920L * 1080L || area < 640L * 480L) continue;
            float ratio = Math.max(size.width, size.height) / (float) Math.min(size.width, size.height);
            double score = Math.abs(ratio - targetRatio) * 2000000d + Math.abs(area - 1280L * 720L);
            if (score < bestScore) { best = size; bestScore = score; }
        }
        return best;
    }

    private void setDisplayOrientation(Camera target, int id) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees;
        switch (rotation) {
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
            default: degrees = 0;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        target.setDisplayOrientation(result);
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { start(); }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (camera != null && cameraId >= 0) setDisplayOrientation(camera, cameraId);
    }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { stop(); return true; }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
}
