package com.gu.clientapp.mvp.presenter;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.example.basemodule.log.LogUtil;
import com.example.basemodule.utils.inet.INetUtil;
import com.gu.clientapp.mvp.contract.ClientContract;
import com.gu.clientapp.mvp.task.PreviewTask;
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

public class ClientPresenter extends BasePresenter implements ClientContract.ClientPresenter {
  private PreviewTask mPreviewTask;
  private CompositeDisposable mCompositeDisposable;
  private byte[] configData;
  private volatile boolean connected;
  private final Object lock = new Object();
  private InetAddress broadcastIP;
  private InetAddress localIp;

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
                    ((ClientContract.ClientView) getView()).hideProgressBar();
                    if (connected) {
                      startPreview(surfaceTexture, width, height);
                    } else {
                      ((ClientContract.ClientView) getView()).showReconnectBtn();
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
          lock.wait();
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
  public void startPreview(SurfaceTexture surfaceTexture, int width, int height) {
    LogUtil.log("开始解码");
    mPreviewTask.configAndStart(new Surface(surfaceTexture), width, height);
    mPreviewTask.startPreview();
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
                    configData = data;
                    mPreviewTask = new PreviewTask(configData);
                    mPreviewTask.startReceiveData();
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
                    configData = data;
                    mPreviewTask = new PreviewTask(configData);
                    mPreviewTask.startReceiveData();
                    connected = true;
                    startPreview(surfaceTexture, width, height);
                    ((ClientContract.ClientView) getView()).hideProgressBar();
                    ((ClientContract.ClientView) getView()).hideReconnectBtn();
                  }
                },
                new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable throwable) throws Exception {
                    ((ClientContract.ClientView) getView()).showReconnectBtn();
                    ((ClientContract.ClientView) getView()).hideProgressBar();
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
  public void stopPreview() {
    mPreviewTask.stopPreview();
  }

  @Override
  public void release() {
    super.release();
    if (mPreviewTask != null) {
      mPreviewTask.stopReceiveData();
      mPreviewTask.releasePreview();
    }
    if (!mCompositeDisposable.isDisposed()) mCompositeDisposable.dispose();
  }
}
