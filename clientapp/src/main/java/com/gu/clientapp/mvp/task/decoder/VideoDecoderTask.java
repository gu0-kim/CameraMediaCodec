package com.gu.clientapp.mvp.task.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.example.basemodule.data.CodecParams;
import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VideoDecoderTask extends Thread {
  private ArrayBlockingQueue<byte[]> dataQueue;
  private boolean release;
  private volatile boolean started;
  private boolean render;
  MediaCodec decode;
  String mMimeType;
  byte[] configData;

  public VideoDecoderTask(
      ArrayBlockingQueue<byte[]> dataQueue, byte[] configData, String mimeType, boolean render) {
    try {
      decode = MediaCodec.createDecoderByType(mimeType);
      this.mMimeType = mimeType;
      this.render = render;
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.dataQueue = dataQueue;
    this.configData = configData;
  }

  @Override
  public void run() {
    byte[] data;
    started = true;
    while (!release) {
      try {
        data = dataQueue.poll(30, TimeUnit.MILLISECONDS);
        if (data != null) {
          offerDecoder(data, data.length);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    LogUtil.log("decoder thread quit!");
  }

  public synchronized void stopDecoder() {
    decode.stop();
    LogUtil.log("release decoder!");
  }

  public void releaseDecoder() {
    release = true;
    decode.stop();
    decode.release();
    dataQueue.clear();
  }

  public void startDecoder() {
    decode.start();
  }

  public boolean isStarted() {
    return started;
  }

  // 解码h264数据
  private synchronized void offerDecoder(byte[] input, int length) {
    try {
      ByteBuffer[] inputBuffers = decode.getInputBuffers();
      int inputBufferIndex = decode.dequeueInputBuffer(0);
      if (inputBufferIndex >= 0) {
        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
        inputBuffer.clear();
        try {
          inputBuffer.put(input, 0, length);
        } catch (Exception e) {
          e.printStackTrace();
        }
        decode.queueInputBuffer(inputBufferIndex, 0, length, 0, 0);
      }
      MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

      int outputBufferIndex = decode.dequeueOutputBuffer(bufferInfo, 0);
      while (outputBufferIndex >= 0) {
        // If a valid surface was specified when configuring the codec,
        // passing true renders this output buffer to the surface.
        decode.releaseOutputBuffer(outputBufferIndex, render);
        outputBufferIndex = decode.dequeueOutputBuffer(bufferInfo, 0);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void configAndStart(Surface surface, int width, int height) {
    LogUtil.log("configVideo");
    final MediaFormat format = MediaFormat.createVideoFormat(mMimeType, width, height);
    format.setInteger(MediaFormat.KEY_BIT_RATE, CodecParams.VIDEO_BITRATE);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, CodecParams.VIDEO_FRAME_PER_SECOND);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, CodecParams.VIDEO_I_FRAME_INTERVAL);
    //    byte[] header_sps = {
    //      0, 0, 0, 1, 103, 66, -128, 30, -38, 2, -128, -10, -128, 109, 10, 19, 80, 0, 0, 0, 1,
    // 104, -50,
    //      6, -30
    //    };//最新
    format.setByteBuffer("csd-0", ByteBuffer.wrap(configData));
    //      format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
    decode.stop();
    decode.configure(format, surface, null, 0);
    decode.start();
  }
}
