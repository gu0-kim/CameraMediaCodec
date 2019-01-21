package com.gu.android.mediacodec.video.mediacodec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import com.example.basemodule.data.CodecParams;
import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.example.basemodule.data.CodecParams.TIMEOUT_SEC;

public class VideoDecoder {
  private DecoderThread mWorker;
  private Surface mSurface;

  public VideoDecoder(Surface surface) {
    this.mSurface = surface;
  }

  public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
    if (mWorker != null) {
      mWorker.decodeSample(data, offset, size, presentationTimeUs, flags);
    }
  }

  public void configure(int width, int height, byte[] csd0, int offset, int size) {
    if (mWorker != null) {
      mWorker.configure(mSurface, width, height, ByteBuffer.wrap(csd0, offset, size));
    }
  }

  public void start() {
    if (mWorker == null) {
      mWorker = new DecoderThread();
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

  class DecoderThread extends Thread {

    volatile boolean mRunning;
    MediaCodec mCodec;
    volatile boolean mConfigured;

    public void setRunning(boolean running) {
      mRunning = running;
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0) {
      if (mConfigured) {
        throw new IllegalStateException("Decoder is already configured");
      }
      MediaFormat format =
          MediaFormat.createVideoFormat(CodecParams.MIME_TYPE_VIDEO_H264, width, height);
      // little tricky here, csd-0 is required in order to configure the codec properly
      // it is basically the first sample from encoder with flag: BUFFER_FLAG_CODEC_CONFIG
      format.setByteBuffer("csd-0", csd0);
      try {
        mCodec = MediaCodec.createDecoderByType(CodecParams.MIME_TYPE_VIDEO_H264);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create codec", e);
      }
      mCodec.configure(format, surface, null, 0);
      mCodec.start();
      mConfigured = true;
    }

    @SuppressWarnings("deprecation")
    public void decodeSample(
        byte[] data, int offset, int size, long presentationTimeUs, int flags) {
      if (mConfigured && mRunning) {
        int index = mCodec.dequeueInputBuffer(TIMEOUT_SEC);
        if (index >= 0) {
          ByteBuffer buffer;
          // since API 21 we have new API to use
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            buffer = mCodec.getInputBuffers()[index];
            buffer.clear();
          } else {
            buffer = mCodec.getInputBuffer(index);
          }
          if (buffer != null) {
            buffer.put(data, offset, size);
            mCodec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
          }
        }
      }
    }

    @Override
    public void run() {
      try {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (mRunning) {
          if (mConfigured) {
            int index = mCodec.dequeueOutputBuffer(info, TIMEOUT_SEC);
            if (index >= 0) {
              // setting true is telling system to render frame onto Surface
              mCodec.releaseOutputBuffer(index, true);
              if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                  == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                break;
              }
            }
          } else {
            // just waiting to be configured, then decode and render
            try {
              Thread.sleep(10);
            } catch (InterruptedException ignore) {
            }
          }
        }
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } finally {
        if (mConfigured) {
          LogUtil.log("abc","VideoDecoder release!");
          try {
            mCodec.stop();
          } catch (IllegalStateException e) {
            e.printStackTrace();
          }
          mCodec.release();
        }
      }
    }
  }
}
