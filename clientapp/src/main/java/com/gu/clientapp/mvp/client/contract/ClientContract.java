package com.gu.clientapp.mvp.client.contract;

import android.app.Activity;
import android.graphics.SurfaceTexture;

import com.gu.clientapp.mvp.task.socket.ConnectServerTask;

public interface ClientContract {
  interface Presenter {
    ClientView getView();

    void setView(ClientView clientView);

    void onCreate(Activity activity);

    void connect2LiveRoom(int roomNo);

    void reConnect2LiveRoom(
        int roomNo, final SurfaceTexture surfaceTexture, final int width, final int height);

    void disconnect2LiveRoom(int roomNo);

    ConnectServerTask createConnectTask(int roomNo) throws Exception;

    void try2startPreview(SurfaceTexture surfaceTexture, int width, int height);

    void startPlaying(SurfaceTexture surfaceTexture, int width, int height);

    void stopPlaying();

    void release();
  }

  interface ClientView {

    void showReconnectBtn();

    void showProgressBar();

    void hideReconnectBtn();

    void hideProgressBar();
  }
}
