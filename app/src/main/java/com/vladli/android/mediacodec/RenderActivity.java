package com.vladli.android.mediacodec;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.vladli.android.mediacodec.tool.WorkThread;

/** Created by vladlichonos on 6/5/15. */
public class RenderActivity extends Activity implements SurfaceHolder.Callback {

  // video output dimension
  static final int OUTPUT_WIDTH = 640;
  static final int OUTPUT_HEIGHT = 480;

  EncoderThread mEncoder;
  VideoDecoder mDecoder;
  WorkThread mWorkThread;
  SurfaceView mSurfaceView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout);

    mSurfaceView = (SurfaceView) findViewById(R.id.surface);
    mSurfaceView.getHolder().addCallback(this);
    //    mEncoder = new MyEncoder();
    //    mDecoder = new VideoDecoder();
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    mDecoder = new VideoDecoder(holder.getSurface());
    mDecoder.start();
    mEncoder = new EncoderThread(mDecoder);
    mEncoder.start();
    mWorkThread = new WorkThread(mEncoder.getCodecInputSurface());
    mWorkThread.start();
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {}

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    mEncoder.stop();
    mDecoder.stop();
  }

  //
  //  // All drawing is happening here
  //  // We draw on virtual surface size of 640x480
  //  // it will be automatically encoded into H.264 stream
  //  class MyRenderer extends SurfaceRenderer {
  //
  //    TextPaint mPaint;
  //    long mTimeStart;
  //
  //    public MyRenderer(Surface surface) {
  //      super(surface);
  //    }
  //
  //    @Override
  //    public void start() {
  //      super.start();
  //      mTimeStart = System.currentTimeMillis();
  //    }
  //
  //    String formatTime() {
  //      int now = (int) (System.currentTimeMillis() - mTimeStart);
  //      int minutes = now / 1000 / 60;
  //      int seconds = now / 1000 % 60;
  //      int millis = now % 1000;
  //      return String.format("%02d:%02d:%03d", minutes, seconds, millis);
  //    }
  //
  //    @Override
  //    protected void onDraw(Canvas canvas) {
  //      // non-UI thread
  //      canvas.drawColor(Color.BLACK);
  //
  //      // setting some text paint
  //      if (mPaint == null) {
  //        mPaint = new TextPaint();
  //        mPaint.setAntiAlias(true);
  //        mPaint.setColor(Color.WHITE);
  //        mPaint.setTextSize(30f * getResources().getConfiguration().fontScale);
  //        mPaint.setTextAlign(Paint.Align.CENTER);
  //      }
  //
  //      canvas.drawText(formatTime(), OUTPUT_WIDTH / 2, OUTPUT_HEIGHT / 2, mPaint);
  //    }
  //  }
}
