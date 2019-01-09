package com.gu.clientapp.mvp.presenter;

import com.gu.clientapp.mvp.view.BaseView;

public abstract class BasePresenter {
  protected BaseView mView;

  public void setView(BaseView view) {
    this.mView = view;
    if (view != null) view.setPresenter(this);
  }

  public BaseView getView() {
    return mView;
  }

  public void release() {
    mView = null;
  }
}
