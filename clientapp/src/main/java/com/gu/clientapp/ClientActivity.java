package com.gu.clientapp;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.example.basemodule.log.LogUtil;
import com.example.basemodule.utils.inet.INetUtil;
import com.gu.clientapp.task.ConnectTask;
import com.gu.clientapp.task.PreviewTask;

import java.net.InetAddress;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ClientActivity extends Activity implements TextureView.SurfaceTextureListener {
  private static final String TAG = ClientActivity.class.getSimpleName();

  @BindView(R.id.tv)
  TextureView tv;

  CompositeDisposable mCompositeDisposable;
  PreviewTask mPreviewTask;
  Surface mSurface;

  @OnClick(R.id.start2ConnectRoomBtn)
  public void onClickedBtn() {
    connect2LiveRoom(mSurface);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.client_main);
    ButterKnife.bind(this);
    mCompositeDisposable = new CompositeDisposable();
    tv.setSurfaceTextureListener(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    tv.setSurfaceTextureListener(null);
    mCompositeDisposable.dispose();
  }

  private void connect2LiveRoom(final Surface surface) {
    mCompositeDisposable.add(
        Observable.just(1000)
            .observeOn(Schedulers.newThread())
            .map(
                new Function<Integer, byte[]>() {
                  @Override
                  public byte[] apply(Integer integer) throws Exception {
                    ConnectTask task = createConnectTask();
                    return task.start2Connect();
                  }
                })
            .subscribe(
                new Consumer<byte[]>() {
                  @Override
                  public void accept(byte[] configBytes) throws Exception {
                    mPreviewTask = new PreviewTask();
                    printByte(configBytes);
                    mPreviewTask.configAndStart(surface, 640, 480, configBytes);
                  }
                }));
  }

  private void disconnect2LiveRoom() {
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

  private ConnectTask createConnectTask() throws Exception {
    InetAddress broadcastIP = INetUtil.getBroadcastAddress(getApplication());
    InetAddress localIp = InetAddress.getByName(INetUtil.getIP(getApplication()));
    LogUtil.log(broadcastIP.getHostAddress());
    LogUtil.log(localIp.getHostAddress());
    return new ConnectTask(1000, "gu", broadcastIP, localIp);
  }

  public static void printByte(byte[] data) {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (byte b : data) {
      sb.append(",").append(b);
    }
    sb.append("}");
    Log.e(TAG, "--------------------config data =" + sb.toString());
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    this.mSurface = new Surface(surface);
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    mPreviewTask.stopPreview();
    disconnect2LiveRoom();
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
}
