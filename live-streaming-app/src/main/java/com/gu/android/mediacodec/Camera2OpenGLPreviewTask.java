package com.gu.android.mediacodec;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.view.Surface;

import com.gu.android.mediacodec.camera.CameraDevice;
import com.gu.android.mediacodec.mediacodec.VideoDecoder;
import com.gu.android.mediacodec.mediacodec.VideoEncoder;
import com.gu.android.mediacodec.opengl.CodecInputSurface;
import com.gu.android.mediacodec.opengl.SurfaceTextureManager;
import com.gu.rtplibrary.rtp.RtpSenderWrapper;

import static com.gu.android.mediacodec.mediacodec.CodecParams.OUTPUT_HEIGHT;
import static com.gu.android.mediacodec.mediacodec.CodecParams.OUTPUT_WIDTH;

public class Camera2OpenGLPreviewTask extends Thread implements VideoEncoder.EncoderCallback {

  private SurfaceTextureManager mStManager;
  private static final long DURATION_SEC = 8; // 8 seconds of video
  private CameraDevice cameraDevice;
  private static final String IP_ADDRESS = "192.168.1.102";
  private static final int PORT = 5004;

  private VideoEncoder mEncoder;
  private VideoDecoder mDecoder;
  private RtpSenderWrapper mRtpSenderWrapper;

  private boolean paused;
  private boolean stopLive;

  public Camera2OpenGLPreviewTask(Surface outputSurface) {
    mDecoder = new VideoDecoder(outputSurface);
    mEncoder = new VideoEncoder(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    mEncoder.setCallback(this);
    mRtpSenderWrapper = new RtpSenderWrapper(IP_ADDRESS, PORT, false);
  }

  @Override
  public void run() {
    mDecoder.start();
    mEncoder.start();
    CodecInputSurface mInputSurface = mEncoder.getCodecInputSurface();

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
      mEncoder.stop();
      mDecoder.stop();
      mInputSurface.release();
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

  // 编码器编码一帧数据结束回调
  @Override
  public void onEncoderDataReady(byte[] data, MediaCodec.BufferInfo info) {
    //    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) ==
    // MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
    //      // this is the first and only config sample, which contains information about codec
    //      // like H.264, that let's configure the decoder
    //      mDecoder.configure(OUTPUT_WIDTH, OUTPUT_HEIGHT, data, 0, info.size);
    //    } else {
    //      // pass byte[] to decoder's queue to render asap
    //      mDecoder.decodeSample(data, 0, info.size, info.presentationTimeUs, info.flags);
    //    }
    mRtpSenderWrapper.sendAvcPacket(data, 0, info.size, 0);
  }
}