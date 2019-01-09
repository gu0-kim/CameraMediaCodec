package com.gu.clientapp.task;

import android.view.Surface;

import com.gu.clientapp.task.decoder.DecoderTask;
import com.gu.clientapp.task.socket.SocketClient;

import java.util.concurrent.ArrayBlockingQueue;

public class PreviewTask {
  private SocketClient mSocketClient;
  private DecoderTask mDecoderTask;
  private boolean decoderStart;

  public PreviewTask(byte[] configData) {
    ArrayBlockingQueue<byte[]> dataQueue = new ArrayBlockingQueue<>(20);
    mSocketClient = new SocketClient(dataQueue);
    mDecoderTask = new DecoderTask(dataQueue, configData);
  }

  public void startReceiveData() {
    mSocketClient.start();
  }

  public void stopReceiveData() {
    mSocketClient.stopSocket();
  }

  public void configAndStart(Surface surface, int width, int height) {
    mDecoderTask.configAndStart(surface, width, height);
  }

  public void startPreview() {
    mSocketClient.setDataNoConsumer(false);
    if (!decoderStart) {
      mDecoderTask.start();
      decoderStart = true;
    }
  }

  public boolean isDecoderStart() {
    return decoderStart;
  }

  public void stopPreview() {
    mSocketClient.setDataNoConsumer(true);
    mDecoderTask.stopDecoder();
  }

  public void releasePreview() {
    mDecoderTask.releaseDecoder();
  }
}
