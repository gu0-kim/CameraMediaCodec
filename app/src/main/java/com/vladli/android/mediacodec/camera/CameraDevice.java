package com.vladli.android.mediacodec.camera;

import android.hardware.Camera;
import android.util.Log;

public class CameraDevice {
  private static final String TAG = "CameraDevice";
  private Camera mCamera;

  public Camera getCamera() {
    return mCamera;
  }
  /**
   * Configures Camera for video capture. Sets mCamera.
   *
   * <p>Opens a Camera and sets parameters. Does not start preview.
   */
  public void prepareCamera(int encWidth, int encHeight) {
    if (mCamera != null) {
      throw new RuntimeException("camera already initialized");
    }

    Camera.CameraInfo info = new Camera.CameraInfo();

    // Try to find a front-facing camera (e.g. for videoconferencing).
    int numCameras = Camera.getNumberOfCameras();
    for (int i = 0; i < numCameras; i++) {
      Camera.getCameraInfo(i, info);
      if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        mCamera = Camera.open(i);
        break;
      }
    }
    if (mCamera == null) {
      Log.d(TAG, "No front-facing camera found; opening default");
      mCamera = Camera.open(); // opens first back-facing camera
    }
    if (mCamera == null) {
      throw new RuntimeException("Unable to open camera");
    }

    Camera.Parameters parms = mCamera.getParameters();

    choosePreviewSize(parms, encWidth, encHeight);
    // leave the frame rate set to default
    mCamera.setParameters(parms);

    Camera.Size size = parms.getPreviewSize();
    Log.d(TAG, "Camera preview size is " + size.width + "x" + size.height);
  }

  /**
   * Attempts to find a preview size that matches the provided width and height (which specify the
   * dimensions of the encoded video). If it fails to find a match it just uses the default preview
   * size.
   *
   * <p>TODO: should do a best-fit match.
   */
  private static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
    // We should make sure that the requested MPEG size is less than the preferred
    // size, and has the same aspect ratio.
    Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();

    for (Camera.Size size : parms.getSupportedPreviewSizes()) {
      if (size.width == width && size.height == height) {
        parms.setPreviewSize(width, height);
        return;
      }
    }

    Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
    if (ppsfv != null) {
      parms.setPreviewSize(ppsfv.width, ppsfv.height);
    }
  }

  /** Stops camera preview, and releases the camera to the system. */
  public void releaseCamera() {
    Log.d(TAG, "releasing camera");
    if (mCamera != null) {
      mCamera.stopPreview();
      mCamera.release();
      mCamera = null;
    }
  }
}
