package com.vladli.android.mediacodec.tool;

import java.io.IOException;

public class WorkThread extends Thread {
  // video output dimension
  static final int OUTPUT_WIDTH = 640;
  static final int OUTPUT_HEIGHT = 480;
  SurfaceTextureManager mStManager;

  @Override
  public void run() {
    mStManager = new SurfaceTextureManager();
    CameraDevice cameraDevice = new CameraDevice();
    cameraDevice.prepareCamera(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    try {
      cameraDevice.getCamera().setPreviewTexture(mStManager.getSurfaceTexture());
      cameraDevice.getCamera().startPreview();
    } catch (IOException e) {
      e.printStackTrace();
    }
    // surface is fully initialized on the activity
    mDecoder.start();
    mEncoder.start();
  }
}
