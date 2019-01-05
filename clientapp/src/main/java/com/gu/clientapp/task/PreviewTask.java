package com.gu.clientapp.task;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static com.example.basemodule.data.Port.CLIENT_LOCAL_DATA_PORT;

public class PreviewTask extends Thread {
  private static final String MIME_TYPE = "video/avc";
  private static final String TAG = "ClientTextureView";
  private DatagramPacket datagramPacket = null;
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
    final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
    format.setInteger(MediaFormat.KEY_BIT_RATE, 40000);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    //    byte[] header_sps = {
    //      0, 0, 0, 1, 103, 66, -128, 30, -38, 2, -128, -10, -128, 109, 10, 19, 80, 0, 0, 0, 1,
    // 104, -50,
    //      6, -30
    //    };//最新

    //      byte[] header_sps = {
    //        0, 0, 0, 1, 103, 66, 0, 41, -115, -115, 64, 80, 30, -48, 15, 8, -124, 83, -128
    //      };
    //
    //      byte[] header_pps = {0, 0, 0, 1, 104, -54, 67, -56};
    //      byte[] header_sps = {
    //        0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31,
    // 0, 0, 3,
    //        3, -23, 0, 0, -22, 96, -108
    //      };
    //      byte[] header_pps = {0, 0, 0, 1, 104, -18, 60, -128};

    format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
    //      format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));

    decode.configure(format, surface, null, 0);
    decode.start();
  }

  private void socketInit() {
    try {
      h264Socket = new DatagramSocket(CLIENT_LOCAL_DATA_PORT); // 端口号
      h264Socket.setReuseAddress(true);
      h264Socket.setBroadcast(true);

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
          Log.e(TAG, "-------------------客户端受到数据--------------------");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      rtpData = datagramPacket.getData();
      if (rtpData != null) {
        if (rtpData[0] == -128 && rtpData[1] == 96) {
          Log.e(TAG, "run:xxx");
          int l1 = (rtpData[12] << 24) & 0xff000000;
          int l2 = (rtpData[13] << 16) & 0x00ff0000;
          int l3 = (rtpData[14] << 8) & 0x0000ff00;
          int l4 = rtpData[15] & 0x000000FF;
          h264Length = l1 + l2 + l3 + l4;
          Log.e(TAG, "run: h264Length=" + h264Length);
          System.arraycopy(rtpData, 16, h264Data, 0, h264Length);
          Log.e(
              TAG,
              "run:h264Data[0]="
                  + h264Data[0]
                  + ","
                  + h264Data[1]
                  + ","
                  + h264Data[2]
                  + ","
                  + h264Data[3]
                  + ","
                  + h264Data[4]
                  + ","
                  + h264Data[5]
                  + ","
                  + h264Data[6]
                  + ","
                  + h264Data[7]
                  + ","
                  + h264Data[8]
                  + ","
                  + h264Data[9]
                  + ","
                  + h264Data[10]
                  + ","
                  + h264Data[11]
                  + ","
                  + h264Data[12]
                  + ","
                  + h264Data[13]
                  + ","
                  + h264Data[14]
                  + ","
                  + h264Data[15]
                  + ","
                  + h264Data[16]
                  + ","
                  + h264Data[17]
                  + ","
                  + h264Data[18]
                  + ","
                  + h264Data[19]
                  + ","
                  + h264Data[20]
                  + ","
                  + h264Data[21]
                  + ","
                  + h264Data[22]); // 打印sps、pps
          offerDecoder(h264Data, h264Data.length);
          Log.e(TAG, "run: offerDecoder=");
        }
      }
    }
  }

  // 解码h264数据
  private void offerDecoder(byte[] input, int length) {
    Log.d(TAG, "offerDecoder: ");
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
