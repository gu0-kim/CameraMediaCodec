package com.gu.clientapp.mvp.contract;

import android.app.Activity;
import android.graphics.SurfaceTexture;

import com.gu.clientapp.mvp.task.socket.ConnectServerTask;

public interface ClientContract {
  interface ClientPresenter {

    void onCreate(Activity activity);

    void connect2LiveRoom();

    void reConnect2LiveRoom(final SurfaceTexture surfaceTexture, final int width, final int height);

    void disconnect2LiveRoom();

    ConnectServerTask createConnectTask() throws Exception;

    void try2startPreview(SurfaceTexture surfaceTexture, int width, int height);

    void startPreview(SurfaceTexture surfaceTexture, int width, int height);

    void stopPreview();
  }

  interface ClientView {

    void showReconnectBtn();

    void showProgressBar();

    void hideReconnectBtn();

    void hideProgressBar();
  }
}
