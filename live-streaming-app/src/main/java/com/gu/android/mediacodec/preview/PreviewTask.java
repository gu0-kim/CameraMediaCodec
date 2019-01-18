package com.gu.android.mediacodec.preview;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.view.Surface;

import com.gu.android.mediacodec.preview.camera.CameraDevice;
import com.gu.android.mediacodec.preview.mediacodec.VideoDecoder;
import com.gu.android.mediacodec.preview.mediacodec.VideoEncoder;
import com.gu.android.mediacodec.preview.opengl.CodecInputSurface;
import com.gu.android.mediacodec.preview.opengl.SurfaceTextureManager;
import com.gu.rtplibrary.utils.ByteUtil;

public class PreviewTask extends Thread implements VideoEncoder.EncoderCallback {

  private SurfaceTextureManager mStManager;
  private CameraDevice cameraDevice;

  private VideoEncoder mEncoder;
  private VideoDecoder mDecoder;

  private boolean stopLive;
  private PreviewCallback mCallback;
  private int previewWidth, previewHeight;

  public interface PreviewCallback {
    void onVideoDataReady(byte[] data, int offset, int size);

    void onVideoConfigDataReady(byte[] configData);
  }

  public PreviewTask(Surface outputSurface, PreviewCallback callback, int width, int height) {
    this.mCallback = callback;
    Camera.Size size = initCamera(width, height);
    previewWidth = size.width;
    previewHeight = size.height;
    mDecoder = new VideoDecoder(outputSurface);
    mEncoder = new VideoEncoder(previewWidth, previewHeight);
    mEncoder.setCallback(this);
  }

  private Camera.Size initCamera(int width, int height) {
    cameraDevice = new CameraDevice();
    return cameraDevice.prepareCamera(width, height);
  }

  public void releasePreview() {
    stopLive = true;
    mCallback = null;
  }

  @Override
  public void run() {
    mDecoder.start();
    mEncoder.start();
    CodecInputSurface mInputSurface = mEncoder.getCodecInputSurface();

    try {
      mInputSurface.makeCurrent();
      mStManager = new SurfaceTextureManager();
      cameraDevice.getCamera().setPreviewTexture(mStManager.getSurfaceTexture());
      cameraDevice.getCamera().startPreview();
      SurfaceTexture st = mStManager.getSurfaceTexture();

      while (!stopLive) {
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
      mEncoder.setCallback(null);
      mDecoder.stop();
      mInputSurface.release();
      mCallback = null;
      //      mRtpSenderWrapper.close();
    }
  }
  /** Stops camera preview, and releases the camera to the system. */
  private void releaseCamera() {
    if (cameraDevice.getCamera() != null) {
      cameraDevice.getCamera().stopPreview();
      cameraDevice.releaseCamera();
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
  public void onVideoEncoderDataReady(byte[] data, MediaCodec.BufferInfo info) {
    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
      // this is the first and only config sample, which contains information about codec
      // like H.264, that let's configure the decoder
      byte[] config = new byte[info.size];
      System.arraycopy(data, 0, config, 0, info.size);
      ByteUtil.printByte(config);
      // {,0,0,0,1,103,66,-128,30,-38,2,-128,-10,-128,109,10,19,80,0,0,0,1,104,-50,6,-30}
      mDecoder.configure(previewWidth, previewHeight, config, 0, info.size);
      if (mCallback != null) mCallback.onVideoConfigDataReady(config);
    } else if (mCallback != null) {
      mCallback.onVideoDataReady(data, 0, info.size);
    }
    mDecoder.decodeSample(data, 0, info.size, info.presentationTimeUs, info.flags);
  }
}
