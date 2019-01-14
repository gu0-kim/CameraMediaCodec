package com.gu.clientapp.mvp.task;

import android.view.Surface;

import com.gu.clientapp.mvp.task.decoder.DecoderTask;
import com.gu.clientapp.mvp.task.socket.SocketClient;

import java.util.concurrent.ArrayBlockingQueue;

public class PreviewTask {
  private SocketClient mSocketClient;
  private DecoderTask mDecoderTask;
  private boolean decoderStart;

  public PreviewTask(byte[] configData) {
    ArrayBlockingQueue<byte[]> dataQueue = new ArrayBlockingQueue<>(60);
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
    mSocketClient.setPauseOffer(false);
    if (!mDecoderTask.isStarted()) {
      mDecoderTask.start();
    } else {
      mDecoderTask.startDecoder();
    }
  }

  public void stopPreview() {
    mSocketClient.setPauseOffer(true);
    mDecoderTask.stopDecoder();
  }

  public void releasePreview() {
    mDecoderTask.releaseDecoder();
  }
}
