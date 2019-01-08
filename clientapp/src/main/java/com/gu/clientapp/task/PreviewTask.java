package com.gu.clientapp.task;

import com.gu.clientapp.task.decoder.DecoderTask;
import com.gu.clientapp.task.socket.SocketClient;

import java.util.concurrent.ArrayBlockingQueue;

public class PreviewTask extends Thread {
  private SocketClient mSocketClient;
  private DecoderTask mDecoderTask;

  private ArrayBlockingQueue<byte[]> dataQueue;

  public PreviewTask(byte[] configData) {
    dataQueue = new ArrayBlockingQueue<>(20);
    mSocketClient = new SocketClient(dataQueue);
    mDecoderTask = new DecoderTask(dataQueue, configData);
  }

  public void stopPreview() {
    mSocketClient.stopSocket();
    mDecoderTask.stopDecoder();
  }
}
