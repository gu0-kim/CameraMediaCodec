package com.gu.clientapp.task.socket;

import com.example.basemodule.log.LogUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

import static com.example.basemodule.data.Port.CLIENT_DATA_PORT;

public class SocketClient extends Thread {
  private DatagramPacket datagramPacket;
  private DatagramSocket h264Socket;
  private boolean stop;
  ArrayBlockingQueue<byte[]> dataQueue;

  public SocketClient(ArrayBlockingQueue<byte[]> dataQueue) {
    socketInit();
    this.dataQueue = dataQueue;
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
          rtpData = datagramPacket.getData();
          if (rtpData != null && rtpData[0] == -128 && rtpData[1] == 96) {
            int l1 = (rtpData[12] << 24) & 0xff000000;
            int l2 = (rtpData[13] << 16) & 0x00ff0000;
            int l3 = (rtpData[14] << 8) & 0x0000ff00;
            int l4 = rtpData[15] & 0x000000FF;
            h264Length = l1 + l2 + l3 + l4;
            LogUtil.log("h264 length=" + h264Length);
            byte[] rdata = new byte[h264Length];
            System.arraycopy(rtpData, 16, rdata, 0, h264Length);
            //          offerDecoder(h264Data, h264Data.length);
            try {
              dataQueue.put(rdata);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void stopSocket() {
    stop = true;
    if (h264Socket != null) {
      h264Socket.close();
      h264Socket = null;
    }
  }
}
