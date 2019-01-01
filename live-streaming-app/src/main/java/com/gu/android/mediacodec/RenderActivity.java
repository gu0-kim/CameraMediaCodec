package com.gu.android.mediacodec;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RenderActivity extends Activity implements SurfaceHolder.Callback {

  LiveStreamTask mTask;
  SurfaceHolder previewHolder;

  @BindView(R.id.surface)
  SurfaceView mSurfaceView;

  @BindView(R.id.startBtn)
  Button startBtn;

  @OnClick(R.id.startBtn)
  public void onClickStartBtn() {
    if (isLiveStream()) {
      mTask.stopLiveStream();
      mTask = null;
      startBtn.setText(getString(R.string.start_btn_text));
      startOrPauseBtn.setVisibility(View.GONE);
    } else {
      startLiveStream();
      startBtn.setText(getString(R.string.stop_btn_text));
      startOrPauseBtn.setVisibility(View.VISIBLE);
      startOrPauseBtn.setText(getString(R.string.pause));
    }
  }

  @BindView(R.id.startOrPauseBtn)
  Button startOrPauseBtn;

  @OnClick(R.id.startOrPauseBtn)
  public void onClickStartOrPauseBtn() {
    if (isLiveStream()) {
      mTask.triggerStatus();
      startOrPauseBtn.setText(
          mTask.isPaused() ? getString(R.string.resume) : getString(R.string.pause));
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout);
    ButterKnife.bind(this);
    mSurfaceView.getHolder().addCallback(this);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    this.previewHolder = holder;
    startBtn.setEnabled(true);
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    if (isLiveStream()) {
      mTask.stopLiveStream();
      mTask = null;
    }
    previewHolder = null;
  }

  private void startLiveStream() {
    if (previewHolder != null && previewHolder.getSurface() != null) {
      mTask = new LiveStreamTask(previewHolder.getSurface());
      mTask.start();
    }
  }

  private boolean isLiveStream() {
    return mTask != null && !mTask.isStopLive();
  }
}
