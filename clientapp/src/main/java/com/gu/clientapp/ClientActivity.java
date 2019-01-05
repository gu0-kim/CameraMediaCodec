package com.gu.clientapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;

import com.gu.clientapp.view.ClientTextureView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ClientActivity extends Activity implements ClientTextureView.SurfaceCallback {
  @BindView(R.id.tv)
  ClientTextureView tv;

  CompositeDisposable mCompositeDisposable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.client_main);
    ButterKnife.bind(this);
    mCompositeDisposable = new CompositeDisposable();
    tv.setCallback(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    tv.setCallback(null);
  }

  @Override
  public void onSurfaceTextureAvailable(Surface surface, int width, int height) {}

  @Override
  public boolean onSurfaceTextureDestroyed() {
    return false;
  }

  private void connect2LiveRoom(Surface surface, int width, int height) {
    mCompositeDisposable.add(
        Observable.just(1000)
            .observeOn(Schedulers.newThread())
            .map(
                new Function<Integer, byte[]>() {
                  @Override
                  public byte[] apply(Integer integer) throws Exception {
                    return new byte[0];
                  }
                })
            .subscribe(new Consumer<byte[]>() {
                @Override
                public void accept(byte[] configBytes) throws Exception {

                }
            }));
  }
}
