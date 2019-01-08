package com.gu.clientapp.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static com.example.basemodule.data.Port.CLIENT_DATA_PORT;

public class PreviewTask extends Thread {
  private static final String MIME_TYPE = "video/avc";
  private DatagramPacket datagramPacket;
  private MediaCodec decode;
  private boolean stop;
  private DatagramSocket h264Socket;

  private byte[] h264Data = new byte[80000];

  public PreviewTask() {
    socketInit();
    try {
      decode = MediaCodec.createDecoderByType(MIME_TYPE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void configAndStart(Surface surface, int width, int height, byte[] header_sps) {
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
    format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
    //      format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));

    decode.configure(format, surface, null, 0);
    decode.start();
    start();
  }

  private void socketInit() {
    try {
      h264Socket = new DatagramSocket(CLIENT_DATA_PORT); // 端口号
      h264Socket.setReuseAddress(true);
      h264Socket.setBroadcast(false);
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    byte[] data = new byte[80000];
    int h264Length;
    byte[] rtpData;

    while (!stop) {
      if (h264Socket != null) {
        try {
          datagramPacket = new DatagramPacket(data, data.length);
          h264Socket.receive(datagramPacket); // 接收数据
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      rtpData = datagramPacket.getData();
      if (rtpData != null && rtpData[0] == -128 && rtpData[1] == 96) {
        int l1 = (rtpData[12] << 24) & 0xff000000;
        int l2 = (rtpData[13] << 16) & 0x00ff0000;
        int l3 = (rtpData[14] << 8) & 0x0000ff00;
        int l4 = rtpData[15] & 0x000000FF;
        h264Length = l1 + l2 + l3 + l4;
        System.arraycopy(rtpData, 16, h264Data, 0, h264Length);
        offerDecoder(h264Data, h264Data.length);
      }
    }
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

  public void stopPreview() {
    stop = true;
    if (h264Socket != null) {
      h264Socket.close();
      h264Socket = null;
    }
    decode.stop();
    decode.release();
  }
}
