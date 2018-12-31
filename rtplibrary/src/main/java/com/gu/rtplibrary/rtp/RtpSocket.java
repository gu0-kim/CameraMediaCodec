package com.gu.rtplibrary.rtp;

import java.io.IOException;

public interface RtpSocket {

  void sendPacket(byte[] data, int offset, int size) throws IOException;

  void close();
}
