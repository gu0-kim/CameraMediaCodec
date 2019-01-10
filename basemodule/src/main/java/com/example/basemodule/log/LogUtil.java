package com.example.basemodule.log;

import android.util.Log;

public class LogUtil {
  private static final String TAG = "LogUtil";

  public static void log(String log) {
    log(TAG, log);
  }

  public static void log(String tag, String log) {
    Log.e(tag, "-----------------" + log + "-----------------");
  }
}
