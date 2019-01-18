package com.gu.clientapp.mvp.task.socket;

import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

import static com.example.basemodule.data.Port.CLIENT_DATA_PORT;

public class SocketClient extends Thread {
  private DatagramSocket h264Socket;
  private boolean stop;
  private ArrayBlockingQueue<byte[]> videoDataQueue;
  private ArrayBlockingQueue<byte[]> audioDataQueue;
  private volatile boolean pauseOffer;

  public SocketClient(
      ArrayBlockingQueue<byte[]> videoDataQueue, ArrayBlockingQueue<byte[]> audioDataQueue) {
    socketInit();
    this.videoDataQueue = videoDataQueue;
    this.audioDataQueue = audioDataQueue;
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

    while (!stop) {
      if (h264Socket != null) {
        try {
          DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
          h264Socket.receive(datagramPacket); // 接收数据
          byte[] rtpData = datagramPacket.getData();
          if (isValidData(rtpData) && !pauseOffer) {
            h264Length = getPackageLength(rtpData);
            byte[] copyData = new byte[h264Length];
            System.arraycopy(rtpData, 16, copyData, 0, h264Length);
            try {
              if (isVideoData(rtpData)) {
                videoDataQueue.put(copyData);
              } else if (isAudioData(rtpData)) {
                audioDataQueue.put(copyData);
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    LogUtil.log("socket thread quit!");
  }

  private boolean isVideoData(byte[] data) {
    return data[1] == 96;
  }

  private boolean isAudioData(byte[] data) {
    return data[1] == 97;
  }

  private boolean isValidData(byte[] data) {
    return data != null && data[0] == -128;
  }

  private int getPackageLength(byte[] data) {
    int l1 = (data[12] << 24) & 0xff000000;
    int l2 = (data[13] << 16) & 0x00ff0000;
    int l3 = (data[14] << 8) & 0x0000ff00;
    int l4 = data[15] & 0x000000FF;
    return l1 + l2 + l3 + l4;
  }

  public void stopSocket() {
    stop = true;
    if (h264Socket != null) {
      h264Socket.close();
      h264Socket = null;
    }
  }

  /**
   * pause produce data to decoder thread.
   *
   * @param pause
   */
  public void setPauseOffer(boolean pause) {
    pauseOffer = pause;
    if (pause) {
      videoDataQueue.clear();
    }
  }
}
