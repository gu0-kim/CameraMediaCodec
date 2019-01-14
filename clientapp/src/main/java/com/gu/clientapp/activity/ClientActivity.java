package com.gu.clientapp.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.gu.clientapp.R;
import com.gu.clientapp.mvp.client.view.ClientFragment;

public class ClientActivity extends AppCompatActivity {
  ClientFragment fragment;
  private FrameLayout contentLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    contentLayout = findViewById(R.id.content_layout);
    ifPortraitResize();
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragment = (ClientFragment) fragmentManager.findFragmentById(R.id.content_layout);
    if (fragment == null) {
      String tag = "client";
      fragment = ClientFragment.newInstance(tag);
      fragmentManager.beginTransaction().add(R.id.content_layout, fragment).commit();
    }
  }

  private void ifPortraitResize() {
    Configuration mConfiguration = getResources().getConfiguration(); // 获取设置的配置信息
    int ori = mConfiguration.orientation; // 获取屏幕方向
    if (ori == Configuration.ORIENTATION_PORTRAIT) {
      // 竖屏
      int height = getScreenWidth() / 3 * 2;
      FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) contentLayout.getLayoutParams();
      params.height = height;
      contentLayout.setLayoutParams(params);
    }
  }

  private int getScreenWidth() {
    WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
    return wm.getDefaultDisplay().getWidth();
  }
}
