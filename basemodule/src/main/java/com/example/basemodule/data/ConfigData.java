package com.example.basemodule.data;

public class ConfigData {
  private byte[] videoConfigData;
  private byte[] audioConfigData;

  public ConfigData(byte[] videoConfigData, byte[] audioConfigData) {
    this.videoConfigData = videoConfigData;
    this.audioConfigData = audioConfigData;
  }

  public byte[] getVideoConfigData() {
    return videoConfigData;
  }

  public void setVideoConfigData(byte[] videoConfigData) {
    this.videoConfigData = videoConfigData;
  }

  public byte[] getAudioConfigData() {
    return audioConfigData;
  }

  public void setAudioConfigData(byte[] audioConfigData) {
    this.audioConfigData = audioConfigData;
  }
}
