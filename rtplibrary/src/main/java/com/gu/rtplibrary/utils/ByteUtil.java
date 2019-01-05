package com.gu.rtplibrary.utils;

import android.util.Log;

public class ByteUtil {
  private static final String TAG = ByteUtil.class.getSimpleName();

  public static void printByte(byte[] data) {
    Log.e(TAG, "----------------------- data size: " + data.length);
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (byte b : data) {
      sb.append(",").append(b);
    }
    sb.append("}");
    Log.e(TAG, "-------------------- data =" + sb.toString());
  }
}
