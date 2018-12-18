package com.vladli.android.mediacodec;

public interface DecoderCallBack {
  void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags);

  void configure(int width, int height, byte[] csd0, int offset, int size);
}
