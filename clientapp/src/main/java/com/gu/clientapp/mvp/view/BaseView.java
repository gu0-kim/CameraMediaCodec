package com.gu.clientapp.mvp.view;

import android.support.v4.app.Fragment;

import com.gu.clientapp.mvp.presenter.BasePresenter;

public abstract class BaseView extends Fragment {
  BasePresenter mPresenter;

  public void setPresenter(BasePresenter presenter) {
    this.mPresenter = presenter;
  }

  public void release() {
    mPresenter.release();
    mPresenter = null;
  }
}
