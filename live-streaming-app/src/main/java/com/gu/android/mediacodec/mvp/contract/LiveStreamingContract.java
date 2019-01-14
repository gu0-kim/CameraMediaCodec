package com.gu.android.mediacodec.mvp.contract;

import android.app.Activity;
import android.view.SurfaceHolder;

import com.gu.android.mediacodec.service.PushStreamServer.ServiceBinder;

public interface LiveStreamingContract {
  interface View {
    void showPreviewing();

    void showIdle();

    void updateStatusImageView(int status);

    void showStartLiveStreaming();

    void updateRoomPeopleNumber(int num);

    void getSurfaceViewSize();

    SurfaceHolder getHolder();

    int getPreviewWidth();

    int getPreviewHeight();

    ServiceBinder getServiceBinder();

    Activity getActivity();

  }

  interface Presenter {
    View getView();

    void setView(View view);

    void startPreview();

    void stopPreview();

    void startLiveStreaming();

    void stopLiveStreaming();

    boolean isLiveStreamingStarted();

    boolean isPreviewStarted();

    void release();
  }
}
