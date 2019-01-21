package com.gu.android.mediacodec.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.gu.android.mediacodec.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingActivity extends AppCompatActivity {

  @BindView(R.id.toolBar)
  Toolbar mToolbar;

  @OnClick(R.id.back_btn)
  public void onClickedBackBtn() {
    finish();
  }

  @OnClick(R.id.go2myRoomBtn)
  public void onClickedGo2myRoomBtn() {
    startActivity(new Intent(this, RenderActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.setting_layout);
    ButterKnife.bind(this);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
  }
}
