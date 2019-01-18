package com.gu.android.mediacodec.mvp.presenter;

import android.view.SurfaceHolder;

import com.gu.android.mediacodec.audio.AudioRecordAndEncodeTask;
import com.gu.android.mediacodec.mvp.contract.LiveStreamingContract.Presenter;
import com.gu.android.mediacodec.mvp.contract.LiveStreamingContract.View;
import com.gu.android.mediacodec.preview.PreviewTask;
import com.gu.android.mediacodec.service.PushStreamServer;

public class LiveStreamingPresenter
    implements Presenter,
        PreviewTask.PreviewCallback,
        AudioRecordAndEncodeTask.AudioCallback,
        PushStreamServer.Callback {

  private View mView;
  private boolean previewStarted, liveStreamingStarted;
  private PreviewTask mPreviewTask;

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
      getView().showPreviewing();
    }
  }

  @Override
  public void stopPreview() {
    if (isPreviewStarted()) {
      mPreviewTask.releasePreview();
      getView().showIdle();
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
    getView().getServiceBinder().add2BlockingQueue(data, 0, size);
  }

  @Override
  public void onVideoConfigDataReady(byte[] configData) {
    getView().getServiceBinder().saveVideoConfigData(configData);
  }

  @Override
  public void onAudioDataReady(byte[] data, int offset, int size) {
      dd
  }

  @Override
  public void onAudioConfigDataReady(byte[] configData) {
      dd
  }

  @Override
  public void notifyConnectChanged(int num) {
    getView().updateRoomPeopleNumber(num);
  }
}
