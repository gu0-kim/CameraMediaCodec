package com.gu.android.mediacodec.mvp.presenter;

import android.view.SurfaceHolder;

import com.gu.android.mediacodec.audio.AudioTask;
import com.gu.android.mediacodec.mvp.contract.LiveStreamingContract.Presenter;
import com.gu.android.mediacodec.mvp.contract.LiveStreamingContract.View;
import com.gu.android.mediacodec.service.PushStreamServer;
import com.gu.android.mediacodec.video.PreviewTask;

public class LiveStreamingPresenter
    implements Presenter,
        PreviewTask.PreviewCallback,
        AudioTask.AudioCallback,
        PushStreamServer.Callback {

  private View mView;
  private boolean previewStarted, liveStreamingStarted;
  private PreviewTask mPreviewTask;
  private AudioTask mAudioTask;

  @Override
  public View getView() {
    return mView;
  }

  @Override
  public void setView(View view) {
    this.mView = view;
  }

  @Override
  public void startPreview() {
    SurfaceHolder holder = getView().getHolder();
    if (holder != null && holder.getSurface() != null) {
      mPreviewTask =
          new PreviewTask(
              holder.getSurface(), this, getView().getPreviewWidth(), getView().getPreviewHeight());
      mPreviewTask.start();
      previewStarted = true;
      mAudioTask = new AudioTask(this);
      mAudioTask.start();
      getView().showPreviewing();
    }
  }

  @Override
  public void stopPreview() {
    if (isPreviewStarted()) {
      mPreviewTask.releasePreview();
      mAudioTask.release();
      if (getView() != null) getView().showIdle();
      previewStarted = false;
    }
  }

  @Override
  public void startLiveStreaming() {
    getView().getServiceBinder().setCallback(this);
    liveStreamingStarted = true;
    getView().getServiceBinder().startPushStream();
    getView().showStartLiveStreaming();
  }

  @Override
  public void stopLiveStreaming() {
    getView().getServiceBinder().setCallback(null);
    liveStreamingStarted = false;
    getView().getServiceBinder().stopPushStream();
    stopPreview();
  }

  @Override
  public boolean isLiveStreamingStarted() {
    return liveStreamingStarted;
  }

  @Override
  public boolean isPreviewStarted() {
    return previewStarted;
  }

  @Override
  public void release() {
    getView().getServiceBinder().stopPushStream();
    mView = null;
  }

  @Override
  public void onVideoDataReady(byte[] data, int offset, int size) {
    getView().getServiceBinder().add2VideoQueue(data, offset, size);
  }

  @Override
  public void onVideoConfigDataReady(byte[] configData) {
    getView().getServiceBinder().saveVideoConfigData(configData);
  }

  @Override
  public void onAudioDataReady(byte[] data, int offset, int size) {
    getView().getServiceBinder().add2AudioQueue(data, offset, size);
  }

  @Override
  public void onAudioConfigDataReady(byte[] configData) {
    getView().getServiceBinder().saveAudioConfigData(configData);
  }

  @Override
  public void notifyConnectChanged(int num) {
    getView().updateRoomPeopleNumber(num);
  }
}
