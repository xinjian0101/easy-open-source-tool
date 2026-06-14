package com.liming.livestage;
import android.app.Activity;import android.content.Context;import android.content.pm.PackageManager;import android.graphics.SurfaceTexture;import android.hardware.Camera;import android.view.TextureView;import java.io.IOException;
@SuppressWarnings("deprecation")
public final class CameraPreview extends TextureView implements TextureView.SurfaceTextureListener{
 private final Activity activity;private Camera camera;private int facing=Camera.CameraInfo.CAMERA_FACING_FRONT;
 public CameraPreview(Context c){super(c);activity=(Activity)c;setSurfaceTextureListener(this);}
 public void start(){if(isAvailable())open(getSurfaceTexture());}
 public void stop(){if(camera!=null){camera.stopPreview();camera.release();camera=null;}}
 public boolean switchLens(){facing=facing==Camera.CameraInfo.CAMERA_FACING_FRONT?Camera.CameraInfo.CAMERA_FACING_BACK:Camera.CameraInfo.CAMERA_FACING_FRONT;stop();start();return facing==Camera.CameraInfo.CAMERA_FACING_FRONT;}
 private int id(){Camera.CameraInfo i=new Camera.CameraInfo();for(int n=0;n<Camera.getNumberOfCameras();n++){Camera.getCameraInfo(n,i);if(i.facing==facing)return n;}return Camera.getNumberOfCameras()>0?0:-1;}
 private void open(SurfaceTexture s){if(camera!=null||activity.checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;int id=id();if(id<0)return;try{camera=Camera.open(id);camera.setDisplayOrientation(90);camera.setPreviewTexture(s);camera.startPreview();setScaleX(facing==Camera.CameraInfo.CAMERA_FACING_FRONT?-1f:1f);}catch(IOException|RuntimeException e){stop();}}
 public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){open(s);}public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}public boolean onSurfaceTextureDestroyed(SurfaceTexture s){stop();return true;}public void onSurfaceTextureUpdated(SurfaceTexture s){}
}
