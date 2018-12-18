package com.vladli.android.mediacodec.tool;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public class WorkThread extends Thread {
  // video output dimension
  private static final int OUTPUT_WIDTH = 640;
  private static final int OUTPUT_HEIGHT = 480;
  private SurfaceTextureManager mStManager;
  private static final long DURATION_SEC = 8; // 8 seconds of video
  private CodecInputSurface mInputSurface;
  private CameraDevice cameraDevice;

  public WorkThread(Surface surface) {
    mInputSurface = new CodecInputSurface(surface);
  }

  @Override
  public void run() {

    cameraDevice = new CameraDevice();
    cameraDevice.prepareCamera(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    try {
      mInputSurface.makeCurrent();
      mStManager = new SurfaceTextureManager();
      cameraDevice.getCamera().setPreviewTexture(mStManager.getSurfaceTexture());
      cameraDevice.getCamera().startPreview();
      long startWhen = System.nanoTime();
      long desiredEnd = startWhen + DURATION_SEC * 1000000000L;
      SurfaceTexture st = mStManager.getSurfaceTexture();

      while (System.nanoTime() < desiredEnd) {
        mStManager.awaitNewImage();
        mStManager.drawImage();
        mInputSurface.setPresentationTime(st.getTimestamp());
        // Submit it to the encoder.  The eglSwapBuffers call will block if the input
        // is full, which would be bad if it stayed full until we dequeued an output
        // buffer (which we can't do, since we're stuck here).  So long as we fully drain
        // the encoder before supplying additional input, the system guarantees that we
        // can supply another frame without blocking.
        mInputSurface.swapBuffers();
      }
      // send end-of-stream to encoder, and drain remaining output
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // release everything we grabbed
      releaseCamera();
      releaseSurfaceTexture();
    }
  }
  /** Stops camera preview, and releases the camera to the system. */
  private void releaseCamera() {
    if (cameraDevice.getCamera() != null) {
      cameraDevice.getCamera().stopPreview();
      cameraDevice.getCamera().release();
    }
  }

  private void releaseSurfaceTexture() {
    if (mStManager != null) {
      mStManager.release();
      mStManager = null;
    }
  }
}
