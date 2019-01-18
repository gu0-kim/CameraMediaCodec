package com.gu.clientapp.mvp.client.presenter;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.example.basemodule.data.ConfigData;
import com.example.basemodule.log.LogUtil;
import com.example.basemodule.utils.inet.INetUtil;
import com.google.gson.Gson;
import com.gu.clientapp.mvp.client.contract.ClientContract;
import com.gu.clientapp.mvp.client.contract.ClientContract.ClientView;
import com.gu.clientapp.mvp.client.contract.ClientContract.Presenter;
import com.gu.clientapp.mvp.task.PlayTask;
import com.gu.clientapp.mvp.task.socket.ConnectServerTask;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ClientPresenter implements Presenter {
  private PlayTask mPlayTask;
  private CompositeDisposable mCompositeDisposable;
  private byte[] videoConfigData;
  private byte[] audioConfigData;
  private volatile boolean connected;
  private final Object lock = new Object();
  private InetAddress broadcastIP;
  private InetAddress localIp;

  private ClientView mClientView;

  @Override
  public void onCreate(Activity activity) {
    mCompositeDisposable = new CompositeDisposable();
    try {
      broadcastIP = INetUtil.getBroadcastAddress(activity);
      localIp = InetAddress.getByName(INetUtil.getIP(activity));
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void try2startPreview(
      final SurfaceTexture surfaceTexture, final int width, final int height) {
    mCompositeDisposable.add(
        Observable.create(
                new ObservableOnSubscribe<Boolean>() {
                  @Override
                  public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                    waitingUntilConnected();
                    LogUtil.log("syn", "wait quit");
                    e.onNext(connected);
                  }
                })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                new Consumer<Boolean>() {
                  @Override
                  public void accept(Boolean connected) throws Exception {
                    getView().hideProgressBar();
                    if (connected) {
                      startPlaying(surfaceTexture, width, height);
                    } else {
                      getView().showReconnectBtn();
                    }
                  }
                },
                new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable throwable) throws Exception {
                    LogUtil.log("exception!");
                  }
                }));
  }

  private void waitingUntilConnected() {
    synchronized (lock) {
      if (!connected) {
        try {
          lock.wait(3000);
        } catch (InterruptedException ee) {
          ee.printStackTrace();
        }
      }
    }
  }

  private void notifyConnected() {
    synchronized (lock) {
      connected = true;
      lock.notifyAll();
    }
  }

  private void notifyWaite() {
    synchronized (lock) {
      lock.notifyAll();
    }
  }

  @Override
  public void startPlaying(SurfaceTexture surfaceTexture, int width, int height) {
    LogUtil.log("开始解码");
    mPlayTask.configVideo(new Surface(surfaceTexture), width, height);
    mPlayTask.configAudio();
    mPlayTask.startPlaying();
  }

  @Override
  public void connect2LiveRoom() {
    mCompositeDisposable.add(
        Observable.just(1000)
            .observeOn(Schedulers.newThread())
            .map(
                new Function<Integer, byte[]>() {
                  @Override
                  public byte[] apply(Integer integer) throws Exception {
                    ConnectServerTask task = createConnectTask();
                    return task.start2Connect();
                  }
                })
            .subscribe(
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] data) throws Exception {
                    ConfigData configData = parseFromByteArray(data);
                    videoConfigData = configData.getVideoConfigData();
                    audioConfigData = configData.getAudioConfigData();
                    mPlayTask = new PlayTask(videoConfigData, audioConfigData);
                    mPlayTask.startReceiveData();
                    notifyConnected();
                  }
                },
                new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable throwable) throws Exception {
                    // 超时或者连接异常
                    notifyWaite();
                  }
                }));
  }

  @Override
  public void reConnect2LiveRoom(
      final SurfaceTexture surfaceTexture, final int width, final int height) {
    mCompositeDisposable.add(
        Observable.just(1000)
            .observeOn(Schedulers.newThread())
            .map(
                new Function<Integer, byte[]>() {
                  @Override
                  public byte[] apply(Integer integer) throws Exception {
                    ConnectServerTask task = createConnectTask();
                    return task.start2Connect();
                  }
                })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] data) throws Exception {
                    ConfigData configData = parseFromByteArray(data);
                    videoConfigData = configData.getVideoConfigData();
                    audioConfigData = configData.getAudioConfigData();
                    mPlayTask = new PlayTask(videoConfigData, audioConfigData);
                    mPlayTask.startReceiveData();
                    connected = true;
                    startPlaying(surfaceTexture, width, height);
                    getView().hideProgressBar();
                    getView().hideReconnectBtn();
                  }
                },
                new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable throwable) throws Exception {
                    getView().showReconnectBtn();
                    getView().hideProgressBar();
                  }
                }));
  }

  @Override
  public void disconnect2LiveRoom() {
    mCompositeDisposable.add(
        Observable.just(1000)
            .observeOn(Schedulers.newThread())
            .subscribe(
                new Consumer<Integer>() {
                  @Override
                  public void accept(Integer integer) throws Exception {
                    createConnectTask().disconnectLiveRoom();
                  }
                }));
  }

  @Override
  public ConnectServerTask createConnectTask() throws Exception {
    return new ConnectServerTask(1000, "gu", broadcastIP, localIp);
  }

  @Override
  public void stopPlaying() {
    if (mPlayTask != null) mPlayTask.stopPlaying();
  }

  @Override
  public void release() {
    if (mPlayTask != null) {
      mPlayTask.stopReceiveData();
      mPlayTask.release();
    }
    if (!mCompositeDisposable.isDisposed()) mCompositeDisposable.dispose();
    mClientView = null;
  }

  @Override
  public ClientContract.ClientView getView() {
    return mClientView;
  }

  @Override
  public void setView(ClientContract.ClientView clientView) {
    mClientView = clientView;
  }

  private ConfigData parseFromByteArray(byte[] data) {
    Gson gson = new Gson();
    String str = new String(data);
    return gson.fromJson(str, ConfigData.class);
  }
}
