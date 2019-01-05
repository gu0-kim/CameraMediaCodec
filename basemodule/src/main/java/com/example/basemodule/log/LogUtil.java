package com.example.basemodule.log;

import android.util.Log;

public class LogUtil {
  private static final String TAG = "LogUtil";

  public static void log(String log) {
    Log.e(TAG, "-----------------" + log + "-----------------");
  }
}
