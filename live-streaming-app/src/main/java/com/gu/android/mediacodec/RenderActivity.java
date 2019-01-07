package com.gu.android.mediacodec;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.gu.android.mediacodec.server.Server;
import com.gu.android.mediacodec.server.Server.ServiceBinder;

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

  private ServiceBinder mServiceBinder;
  private ServiceConnection mServiceConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          mServiceBinder = (ServiceBinder) service;
          mServiceBinder.setRoomNumber(1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
      };

  @OnClick(R.id.startBtn)
  public void onClickStartBtn() {
    if (isLiveStream()) {
      mServiceBinder.stopServer();
      mTask.stopLiveStream();
      mTask = null;
      startBtn.setText(getString(R.string.start_btn_text));
      startOrPauseBtn.setVisibility(View.GONE);
    } else {
      mServiceBinder.startServer();
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
    Intent service = new Intent(this, Server.class);
    bindService(service, mServiceConnection, BIND_AUTO_CREATE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(mServiceConnection);
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
      mTask = new LiveStreamTask(previewHolder.getSurface(), mServiceBinder);
      mTask.start();
    }
  }

  private boolean isLiveStream() {
    return mTask != null && !mTask.isStopLive();
  }
}
