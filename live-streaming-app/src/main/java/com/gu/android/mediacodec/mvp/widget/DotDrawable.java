package com.gu.android.mediacodec.mvp.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class DotDrawable extends Drawable {
  private Paint mPaint;
  private int mSize;

  public DotDrawable(int size, int color) {
    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setColor(color);
    mSize = size;
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2, mPaint);
  }

  @Override
  public int getIntrinsicWidth() {
    return mSize;
  }

  @Override
  public int getIntrinsicHeight() {
    return mSize;
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
  }

  public void setColor(int color) {
    mPaint.setColor(color);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
