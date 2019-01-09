package com.gu.clientapp.task.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DecoderTask extends Thread {
  private MediaCodec decode;
  private static final String MIME_TYPE = "video/avc";
  private ArrayBlockingQueue<byte[]> dataQueue;
  private boolean release;
  private byte[] configData;

  public DecoderTask(ArrayBlockingQueue<byte[]> dataQueue, byte[] configData) {
    try {
      decode = MediaCodec.createDecoderByType(MIME_TYPE);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.dataQueue = dataQueue;
    this.configData = configData;
  }

  @Override
  public void run() {
    byte[] data;
    while (!release) {
      try {
        data = dataQueue.poll(30, TimeUnit.MILLISECONDS);
        LogUtil.log("解码器解析数据,data=" + data);
        if (data != null) {
          offerDecoder(data, data.length);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    LogUtil.log("decoder thread quit!");
  }

  public void stopDecoder() {
    decode.stop();
    LogUtil.log("stop decoder!");
  }

  public void releaseDecoder() {
    release = true;
    decode.stop();
    decode.release();
    dataQueue.clear();
  }

  public void configAndStart(Surface surface, int width, int height) {
    LogUtil.log("configAndStart");
    final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 40000);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
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

  // 解码h264数据
  private void offerDecoder(byte[] input, int length) {
    LogUtil.log("offerDecoder");
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
      LogUtil.log("outputBufferIndex=" + outputBufferIndex);
      while (outputBufferIndex >= 0) {
        // If a valid surface was specified when configuring the codec,
        // passing true renders this output buffer to the surface.
        decode.releaseOutputBuffer(outputBufferIndex, true);
        outputBufferIndex = decode.dequeueOutputBuffer(bufferInfo, 0);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
