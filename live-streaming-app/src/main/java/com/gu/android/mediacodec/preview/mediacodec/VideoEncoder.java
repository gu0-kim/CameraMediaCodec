package com.gu.android.mediacodec.preview.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import com.example.basemodule.data.CodecParams;
import com.example.basemodule.log.LogUtil;
import com.gu.android.mediacodec.preview.opengl.CodecInputSurface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.example.basemodule.data.CodecParams.TIMEOUT_SEC;

public class VideoEncoder {

  public interface EncoderCallback {
    void onVideoEncoderDataReady(byte[] data, MediaCodec.BufferInfo info);
  }

  private CodecInputSurface mCodecInputSurface;
  private EncoderCallback mCallback;
  private EncoderThread mWorker;
  private int mWidth, mHeight;
  private byte[] mBuffer = new byte[0];

  public VideoEncoder(int width, int height) {
    mWidth = width;
    mHeight = height;
  }

  public void setCallback(EncoderCallback callback) {
    this.mCallback = callback;
  }

  public CodecInputSurface getCodecInputSurface() {
    return mCodecInputSurface;
  }

  private void onSurfaceCreated(Surface surface) {
    mCodecInputSurface = new CodecInputSurface(surface);
  }

  private void onSurfaceDestroyed(Surface surface) {}

  private void onEncodeFrame(MediaCodec.BufferInfo info, ByteBuffer data) {
    // Here we could have just used ByteBuffer, but in real life case we might need to
    // send sample over network, etc. This requires byte[]
    if (mCallback != null) {
      if (mBuffer.length < info.size) {
        mBuffer = new byte[info.size];
      }
      data.position(info.offset);
      data.limit(info.offset + info.size);
      data.get(mBuffer, 0, info.size);
      mCallback.onVideoEncoderDataReady(mBuffer, info);
    }
  }

  public void start() {
    if (mWorker == null) {
      mWorker = new EncoderThread();
      mWorker.prepare();
      mWorker.setRunning(true);
      mWorker.start();
    }
  }

  public void stop() {
    if (mWorker != null) {
      mWorker.setRunning(false);
      mWorker = null;
    }
  }

  class EncoderThread extends Thread {

    MediaCodec.BufferInfo mBufferInfo;
    MediaCodec mCodec;
    volatile boolean mRunning;
    Surface mSurface;

    EncoderThread() {
      mBufferInfo = new MediaCodec.BufferInfo();
    }

    public void setRunning(boolean running) {
      mRunning = running;
    }

    @Override
    public void run() {
      try {
        while (mRunning) {
          encode();
        }
        encode();
      } finally {
        release();
      }
    }

    @SuppressWarnings("deprecation")
    void encode() {
      if (!mRunning) {
        // if not running anymore, complete stream
        mCodec.signalEndOfInputStream();
      }
      // New api is nicer, see below
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
        for (; ; ) {
          // MediaCodec is asynchronous, that's why we have a blocking check
          // to see if we have something to do
          int status = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_SEC);
          if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (!mRunning) break;
          } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            outputBuffers = mCodec.getOutputBuffers();
          } else if (status >= 0) {
            // encoded sample
            ByteBuffer data = outputBuffers[status];
            data.position(mBufferInfo.offset);
            data.limit(mBufferInfo.offset + mBufferInfo.size);
            final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
            // pass to whoever listens to
            if (endOfStream == 0) onEncodeFrame(mBufferInfo, data);
            // releasing buffer is important
            mCodec.releaseOutputBuffer(status, false);
            if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) break;
          }
        }
      } else {
        for (; ; ) {
          int status = mCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_SEC);
          if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (!mRunning) break;
          } else if (status >= 0) {
            // encoded sample
            ByteBuffer data = mCodec.getOutputBuffer(status);
            if (data != null) {
              final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
              // pass to whoever listens to
              if (endOfStream == 0) onEncodeFrame(mBufferInfo, data);
              // releasing buffer is important
              mCodec.releaseOutputBuffer(status, false);
              if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) break;
            }
          }
        }
      }
    }

    void release() {
      // notify about destroying surface first before actually destroying it
      // otherwise unexpected exceptions can happen, since we working in multiple threads
      // simultaneously
      onSurfaceDestroyed(mSurface);
      mCodec.stop();
      mCodec.release();
      mSurface.release();
      LogUtil.log("VideoEncoder release!");
    }

    void prepare() {
      // configure video output
      MediaFormat format = MediaFormat.createVideoFormat(CodecParams.MIME_TYPE_VIDEO_H264, mWidth, mHeight);
      format.setInteger(
          MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
      format.setInteger(MediaFormat.KEY_BIT_RATE, CodecParams.VIDEO_BITRATE);
      format.setInteger(MediaFormat.KEY_FRAME_RATE, CodecParams.VIDEO_FRAME_PER_SECOND);
      format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, CodecParams.VIDEO_I_FRAME_INTERVAL);

      try {
        mCodec = MediaCodec.createEncoderByType(CodecParams.MIME_TYPE_VIDEO_H264);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      // create surface associated with code
      mSurface = mCodec.createInputSurface();
      // notify codec to start watch surface and encode samples
      mCodec.start();

      onSurfaceCreated(mSurface);
    }
  }
}
