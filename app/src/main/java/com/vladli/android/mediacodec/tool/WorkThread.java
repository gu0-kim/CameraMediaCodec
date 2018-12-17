package com.vladli.android.mediacodec.tool;

import java.io.IOException;

public class WorkThread extends Thread {

  @Override
  public void run() {
    SurfaceTextureManager st = new SurfaceTextureManager();
    CameraDevice cameraDevice = new CameraDevice();
    cameraDevice.prepareCamera(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    try {
      cameraDevice.getCamera().setPreviewTexture(st.getSurfaceTexture());
    } catch (IOException e) {
      e.printStackTrace();
    }
    // surface is fully initialized on the activity
    mDecoder.start();
    mEncoder.start();
  }
}
