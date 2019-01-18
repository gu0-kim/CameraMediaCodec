package com.gu.android.mediacodec.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.example.basemodule.data.CodecParams;

public class AudioRecorderDevice {
  private AudioRecord mRecord;
  private int bufferSize; // 最小缓冲区大小
  private AudioRecorderCallback mCallback;
  private volatile boolean stop;

  public interface AudioRecorderCallback {
    void onAudioRecorderDataReady(byte[] data, int offset, int size);
  }

  public AudioRecorderDevice(AudioRecorderCallback callback) {
    this.mCallback = callback;
    bufferSize =
        AudioRecord.getMinBufferSize(
            CodecParams.AUDIO_SAMPLE_RATE,
            CodecParams.AUDIO_CHANNEL,
            AudioFormat.ENCODING_PCM_16BIT); // 计算最小缓冲区
    mRecord =
        new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            CodecParams.AUDIO_SAMPLE_RATE,
            CodecParams.AUDIO_CHANNEL,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize);
  }

  public void startRecord() {
    stop = false;
    byte[] buffer = new byte[bufferSize];
    int bufferReadResult;
    mRecord.startRecording();
    while (!stop) {
      bufferReadResult = mRecord.read(buffer, 0, bufferSize);
      if (mCallback != null && bufferReadResult > 0) {
        mCallback.onAudioRecorderDataReady(buffer, 0, bufferReadResult);
      }
    }
  }

  public void stopRecord() {
    stop = true;
    mRecord.stop();
  }

  public void release() {
    stop = true;
    mRecord.release();
  }
}
