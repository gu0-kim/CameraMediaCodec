package com.gu.android.mediacodec.sp;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtil {
  private SharedPreferences sp;
  private static SpUtil instance;

  private SpUtil(Context context, String name) {
    sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
  }

  public static SpUtil getInstance(Context context, String name) {
    if (instance == null) {
      instance = new SpUtil(context, name);
    }
    return instance;
  }

  public void putData(String key, String value) {
    SharedPreferences.Editor editor = sp.edit();
    editor.putString(key, value);
    editor.apply();
  }

  public String getData(String key, String value) {
    return sp.getString(key, null);
  }
}
