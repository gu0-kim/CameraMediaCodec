package com.vladli.android.mediacodec;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RenderActivity extends Activity implements SurfaceHolder.Callback {

  Camera2OpenGLPreviewTask mTask;

  @BindView(R.id.surface)
  SurfaceView mSurfaceView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout);
    ButterKnife.bind(this);
    mSurfaceView.getHolder().addCallback(this);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    mTask = new Camera2OpenGLPreviewTask(holder.getSurface());
    mTask.start();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {}
}
