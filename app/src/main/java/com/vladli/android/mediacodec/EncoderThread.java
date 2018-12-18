package com.vladli.android.mediacodec;

import android.media.MediaCodec;
import android.view.Surface;

import com.vladli.android.mediacodec.tool.CodecInputSurface;

import java.nio.ByteBuffer;

public class EncoderThread extends VideoEncoder {
  private static final int OUTPUT_WIDTH = 640;
  private static final int OUTPUT_HEIGHT = 480;
  //    SurfaceRenderer mRenderer;
  private byte[] mBuffer = new byte[0];
  private CodecInputSurface mCodecInputSurface;
  private DecoderCallBack decoderCallBack;

  public EncoderThread(DecoderCallBack decoderCallBack) {
    super(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    this.decoderCallBack = decoderCallBack;
  }

  // Both of onSurfaceCreated and onSurfaceDestroyed are called from codec's thread,
  // non-UI thread

  public CodecInputSurface getCodecInputSurface() {
    return mCodecInputSurface;
  }

  @Override
  protected void onSurfaceCreated(Surface surface) {
    // surface is created and codec is ready to accept input (Canvas)
    //      mRenderer = new MyRenderer(surface);
    //      mRenderer.start();
    mCodecInputSurface = new CodecInputSurface(surface);
  }

  @Override
  protected void onSurfaceDestroyed(Surface surface) {
    // need to make sure to block this thread to fully complete drawing cycle
    // otherwise unpredictable exceptions will be thrown (aka IllegalStateException)
    //      mRenderer.stopAndWait();
    //      mRenderer = null;
  }

  @Override
  protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
    // Here we could have just used ByteBuffer, but in real life case we might need to
    // send sample over network, etc. This requires byte[]
    if (mBuffer.length < info.size) {
      mBuffer = new byte[info.size];
    }
    data.position(info.offset);
    data.limit(info.offset + info.size);
    data.get(mBuffer, 0, info.size);

    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
      // this is the first and only config sample, which contains information about codec
      // like H.264, that let's configure the decoder
      decoderCallBack.configure(OUTPUT_WIDTH, OUTPUT_HEIGHT, mBuffer, 0, info.size);
    } else {
      // pass byte[] to decoder's queue to render asap
      decoderCallBack.decodeSample(mBuffer, 0, info.size, info.presentationTimeUs, info.flags);
    }
  }
}
