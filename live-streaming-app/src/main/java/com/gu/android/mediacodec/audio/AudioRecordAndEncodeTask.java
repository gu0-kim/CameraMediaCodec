package com.gu.android.mediacodec.audio;

public class AudioRecordAndEncodeTask extends Thread
    implements AudioRecorderDevice.AudioRecorderCallback, AudioEncoder.AudioEncoderDataCallback {

  private AudioRecorderDevice mRecorderDevice;
  private AudioEncoder mAudioEncoder;
  private AudioCallback mCallback;

  public interface AudioCallback {
    void onAudioDataReady(byte[] data, int offset, int size);

    void onAudioConfigDataReady(byte[] configData);
  }

  public AudioRecordAndEncodeTask(AudioCallback callback) {
    mCallback = callback;
    mRecorderDevice = new AudioRecorderDevice(this);
    mAudioEncoder = new AudioEncoder(this);
  }

  @Override
  public void onAudioRecorderDataReady(byte[] data, int offset, int size) {
    mAudioEncoder.encode(data, offset, size);
  }

  @Override
  public void onAudioConfigDataReady(byte[] configData) {
    mCallback.onAudioConfigDataReady(configData);
  }

  @Override
  public void onAudioEncoderDataReady(byte[] data) {
    mCallback.onAudioDataReady(data, 0, data.length);
  }

  @Override
  public void run() {
    mRecorderDevice.startRecord();
  }

  public void stopTask() {
    mRecorderDevice.stopRecord();
  }

  public void release() {
    mCallback = null;
    mRecorderDevice.release();
    mAudioEncoder.release();
  }
}
