package com.gu.clientapp.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

public class ClientTextureView extends TextureView implements TextureView.SurfaceTextureListener {

  private SurfaceCallback mCallback;

  public void setCallback(SurfaceCallback callback) {
    this.mCallback = callback;
  }

  public interface SurfaceCallback {
    void onSurfaceTextureAvailable(Surface surface, int width, int height);

    boolean onSurfaceTextureDestroyed();
  }

  public ClientTextureView(Context context, AttributeSet attrs) {

    super(context, attrs);
    setSurfaceTextureListener(this);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    if (mCallback != null) {
      mCallback.onSurfaceTextureAvailable(new Surface(surface), 640, 480);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    if (mCallback != null) {
      mCallback.onSurfaceTextureDestroyed();
    }
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
}
