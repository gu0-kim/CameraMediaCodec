package com.gu.clientapp.mvp.client.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.basemodule.log.LogUtil;
import com.gu.clientapp.R;
import com.gu.clientapp.activity.ClientActivity;
import com.gu.clientapp.mvp.client.contract.ClientContract;
import com.gu.clientapp.mvp.client.presenter.ClientPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class ClientFragment extends Fragment
    implements TextureView.SurfaceTextureListener, ClientContract.ClientView {

  Unbinder unbinder;

  @BindView(R.id.texture)
  TextureView mTextureView;

  @BindView(R.id.pb)
  ProgressBar pb;

  @BindView(R.id.reconnectBtn)
  Button reconnectBtn;

  ClientPresenter presenter;
  ClientActivity mActivity;

  SurfaceTexture availableSurface;
  int width, height;

  public static ClientFragment newInstance(String tag) {
    ClientFragment fragment = new ClientFragment();
    Bundle data = new Bundle();
    data.putString("key", tag);
    fragment.setArguments(data);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mActivity = (ClientActivity) context;
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mActivity = null;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // 保证屏幕旋转不重建fragment
    setRetainInstance(true);
    presenter = new ClientPresenter();
    presenter.setView(this);
    presenter.onCreate(mActivity);
    presenter.connect2LiveRoom();
  }

  @OnClick(R.id.reconnectBtn)
  public void onClickedReconnectBtn() {
    showProgressBar();
    hideReconnectBtn();
    presenter.reConnect2LiveRoom(availableSurface, width, height);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View parent = inflater.inflate(R.layout.client_fragment, container, false);
    unbinder = ButterKnife.bind(this, parent);
    mTextureView.setSurfaceTextureListener(this);
    showProgressBar();
    return parent;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    presenter.disconnect2LiveRoom();
    presenter.release();
    unbinder.unbind();
    unbinder = null;
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    LogUtil.log("onSurfaceTextureAvailable");
    availableSurface = surface;
    this.width = width;
    this.height = height;
    presenter.try2startPreview(surface, width, height);
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    LogUtil.log("des", "onSurfaceTextureDestroyed");
    presenter.stopPlaying();
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

  @Override
  public void showReconnectBtn() {
    reconnectBtn.setVisibility(View.VISIBLE);
  }

  @Override
  public void showProgressBar() {
    pb.setVisibility(View.VISIBLE);
  }

  @Override
  public void hideReconnectBtn() {
    reconnectBtn.setVisibility(View.GONE);
  }

  @Override
  public void hideProgressBar() {
    pb.setVisibility(View.GONE);
  }
}
