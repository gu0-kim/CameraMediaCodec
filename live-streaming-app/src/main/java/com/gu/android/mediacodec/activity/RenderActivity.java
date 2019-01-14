package com.gu.android.mediacodec.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.basemodule.log.LogUtil;
import com.gu.android.mediacodec.R;
import com.gu.android.mediacodec.mvp.view.LiveStreamingView;

public class RenderActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LogUtil.log("Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      findViewById(android.R.id.content)
          .setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    setContentView(R.layout.main);
    LiveStreamingView fragment =
        (LiveStreamingView) getSupportFragmentManager().findFragmentById(R.id.content_layout);
    if (fragment == null) {
      fragment = LiveStreamingView.newInstance("tag");
      getSupportFragmentManager().beginTransaction().add(R.id.content_layout, fragment).commit();
    }
  }
}
