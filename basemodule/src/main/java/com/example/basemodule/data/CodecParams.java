package com.example.basemodule.data;

public class CodecParams {
  public static final String MIME_TYPE_VIDEO_H264 = "video/avc";
  public static final int VIDEO_FRAME_PER_SECOND = 30;
  public static final int VIDEO_I_FRAME_INTERVAL = 1;
  public static final int VIDEO_BITRATE = 3000 * 1000;
  public static final long TIMEOUT_SEC = 1000L;

  //
  public static final String MIME_TYPE_AUDIO_AAC = "audio/mp4a-latm";
  public static final int AUDIO_SAMPLE_RATE = 44100;
  public static final int AUDIO_BIT_RATE = 96000;
  public static final int AUDIO_CHANNEL = 2;
}
