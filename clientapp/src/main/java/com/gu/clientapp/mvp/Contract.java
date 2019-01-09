package com.gu.clientapp.mvp;

import android.graphics.SurfaceTexture;

import com.gu.clientapp.task.socket.ConnectServerTask;

public interface Contract {
  interface ClientPresenter {

    void onCreate();

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
