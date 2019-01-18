package com.gu.clientapp.mvp.task;

import android.view.Surface;

import com.example.basemodule.data.CodecParams;
import com.gu.clientapp.mvp.task.decoder.AudioDecoderTask;
import com.gu.clientapp.mvp.task.decoder.VideoDecoderTask;
import com.gu.clientapp.mvp.task.socket.SocketClient;

import java.util.concurrent.ArrayBlockingQueue;

public class PlayTask {
  private SocketClient mSocketClient;
  private VideoDecoderTask mVideoDecoderTask;
  private AudioDecoderTask mAudioDecoderTask;

  public PlayTask(byte[] videoConfigData, byte[] audioConfigData) {
    ArrayBlockingQueue<byte[]> videoDataQueue = new ArrayBlockingQueue<>(60);
    ArrayBlockingQueue<byte[]> audioDataQueue = new ArrayBlockingQueue<>(60);
    mSocketClient = new SocketClient(videoDataQueue, audioDataQueue);
    mAudioDecoderTask = new AudioDecoderTask(audioDataQueue, audioConfigData);
    mVideoDecoderTask =
        new VideoDecoderTask(videoDataQueue, videoConfigData, CodecParams.MIME_TYPE_VIDEO_H264, true);
  }

  public void startReceiveData() {
    mSocketClient.start();
  }

  public void stopReceiveData() {
    mSocketClient.stopSocket();
  }

  public void configVideo(Surface surface, int width, int height) {
    mVideoDecoderTask.configAndStart(surface, width, height);
  }

  public void configAudio() {
    mAudioDecoderTask.prepare();
  }

  public void startPlaying() {
    mSocketClient.setPauseOffer(false);
    if (!mVideoDecoderTask.isStarted()) {
      mVideoDecoderTask.start();
    } else {
      mVideoDecoderTask.startDecoder();
    }

    if (!mAudioDecoderTask.isStarted()) {
      mAudioDecoderTask.start();
    } else {
      mAudioDecoderTask.startDecoder();
    }
  }

  public void stopPlaying() {
    mSocketClient.setPauseOffer(true);
    mVideoDecoderTask.stopDecoder();
    mAudioDecoderTask.stopThread();
  }

  public void release() {
    mVideoDecoderTask.releaseDecoder();
    mAudioDecoderTask.releaseThread();
  }
}
