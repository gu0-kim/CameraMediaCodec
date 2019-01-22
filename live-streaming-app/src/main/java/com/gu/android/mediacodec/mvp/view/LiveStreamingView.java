package com.gu.android.mediacodec.mvp.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.basemodule.log.LogUtil;
import com.gu.android.mediacodec.R;
import com.gu.android.mediacodec.mvp.contract.LiveStreamingContract.View;
import com.gu.android.mediacodec.mvp.presenter.LiveStreamingPresenter;
import com.gu.android.mediacodec.mvp.widget.StatusView;
import com.gu.android.mediacodec.service.PushStreamServer;
import com.gu.android.mediacodec.service.PushStreamServer.ServiceBinder;

import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.content.Context.BIND_AUTO_CREATE;

public class LiveStreamingView extends Fragment implements View, SurfaceHolder.Callback {

  LiveStreamingPresenter mPresenter;
  Unbinder unbinder;
  int[][] states =
      new int[][] {
        new int[] {android.R.attr.state_enabled}, // enabled
        new int[] {-android.R.attr.state_enabled}, // disabled
        new int[] {-android.R.attr.state_checked}, // unchecked
        new int[] {android.R.attr.state_pressed} // pressed
      };

  @BindView(R.id.startOrStopBtn)
  FloatingActionButton startOrStopBtn;

  @BindView(R.id.personNumTv)
  TextView personNumTv;

  @BindView(R.id.playTv)
  TextView playTv;

  @BindView(R.id.previewBtn)
  Button previewBtn;

  @BindView(R.id.surface)
  SurfaceView mSurfaceView;

  @BindView(R.id.statusLayout)
  StatusView statusLayout;

  @BindView(R.id.playBtnLayout)
  LinearLayout playBtnLayout;

  @BindView(R.id.roomNO_tv)
  TextView roomNO_tv;

  private String roomNo;

  @BindColor(R.color.start_color)
  int startColor;

  @BindColor(R.color.stop_color)
  int stopColor;

  @BindString(R.string.previewing)
  String previewStr;

  @BindString(R.string.living)
  String livingStr;

  SurfaceHolder previewHolder;

  Activity mActivity;

  private int width, height;
  private ServiceBinder mServiceBinder;

  private ServiceConnection mServiceConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          mServiceBinder = (ServiceBinder) service;
          mServiceBinder.setRoomNumber(Integer.valueOf(roomNo));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
      };

  @OnClick(R.id.previewBtn)
  public void onClickedPreviewBtn() {
    if (mPresenter.isPreviewStarted()) {
      mPresenter.stopPreview();
    } else {
      mPresenter.startPreview();
    }
  }

  @OnClick(R.id.startOrStopBtn)
  public void onClickedStartOrStopBtn() {
    if (mPresenter.isLiveStreamingStarted()) {
      mPresenter.stopLiveStreaming();
    } else {
      mPresenter.startLiveStreaming();
    }
  }

  public static LiveStreamingView newInstance(String tag) {
    LiveStreamingView fragment = new LiveStreamingView();
    Bundle data = new Bundle();
    data.putString("roomNo", tag);
    fragment.setArguments(data);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mActivity = (Activity) context;
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mActivity = null;
  }

  @Nullable
  @Override
  public android.view.View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    android.view.View parent = inflater.inflate(R.layout.layout, container, false);
    unbinder = ButterKnife.bind(this, parent);
    mPresenter = new LiveStreamingPresenter();
    mPresenter.setView(this);
    getSurfaceViewSize();
    mSurfaceView.getHolder().addCallback(this);
    roomNo = getArguments().getString("roomNo");
    roomNO_tv.setText(String.format(Locale.getDefault(), "直播间号：%s", roomNo));
    Intent service = new Intent(mActivity, PushStreamServer.class);
    mActivity.bindService(service, mServiceConnection, BIND_AUTO_CREATE);
    return parent;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mActivity.unbindService(mServiceConnection);
    mPresenter.release();
    unbinder.unbind();
    unbinder = null;
    LogUtil.log("onDestroyView");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void showPreviewing() {
    previewBtn.setVisibility(android.view.View.VISIBLE);
    previewBtn.setText(R.string.stop_preview_btn_text);
    playBtnLayout.setVisibility(android.view.View.VISIBLE);
    playTv.setText(R.string.start_live_stream);
    updateStatusImageView(0);
  }

  @Override
  public void showIdle() {
    previewBtn.setVisibility(android.view.View.VISIBLE);
    previewBtn.setText(R.string.start_preview_btn_text);
    playBtnLayout.setVisibility(android.view.View.GONE);
    playTv.setText(R.string.start_live_stream);
    personNumTv.setVisibility(android.view.View.GONE);
    statusLayout.setVisibility(android.view.View.GONE);
    startOrStopBtn.setBackgroundTintList(getColorStateList(startColor));
    startOrStopBtn.setImageResource(R.drawable.ic_action_videocam);
  }

  @Override
  public void showStartLiveStreaming() {
    startOrStopBtn.setBackgroundTintList(getColorStateList(stopColor));
    startOrStopBtn.setImageResource(R.drawable.ic_action_videocam_off);
    personNumTv.setVisibility(android.view.View.VISIBLE);
    updateRoomPeopleNumber(0);
    previewBtn.setVisibility(android.view.View.GONE);
    playTv.setText(R.string.stop_live_stream);
    updateStatusImageView(1);
  }

  @Override
  public void updateRoomPeopleNumber(int num) {
    personNumTv.setText(
        String.format(Locale.getDefault(), "%d%s", num, getString(R.string.person_num_text)));
  }

  @Override
  public void updateStatusImageView(int status) {
    statusLayout.setVisibility(android.view.View.VISIBLE);
    if (status == 0) {
      statusLayout.update(previewStr, Color.RED);
    } else {
      statusLayout.update(livingStr, Color.GREEN);
    }
  }

  @Override
  public void getSurfaceViewSize() {
    final ViewTreeObserver vto = mSurfaceView.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            width = mSurfaceView.getWidth();
            height = mSurfaceView.getHeight();
            int max = Math.max(width, height);
            int min = Math.min(width, height);
            width = max;
            height = min;
            LogUtil.log("width=" + width + ",height=" + height);
            mSurfaceView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    this.previewHolder = holder;
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    mPresenter.stopPreview();
    previewHolder = null;
  }

  @Override
  public SurfaceHolder getHolder() {
    return previewHolder;
  }

  @Override
  public int getPreviewWidth() {
    return width;
  }

  @Override
  public int getPreviewHeight() {
    return height;
  }

  @Override
  public ServiceBinder getServiceBinder() {
    return mServiceBinder;
  }

  private ColorStateList getColorStateList(int color) {
    int[] colors = new int[] {color, color, color, color};
    return new ColorStateList(states, colors);
  }
}
