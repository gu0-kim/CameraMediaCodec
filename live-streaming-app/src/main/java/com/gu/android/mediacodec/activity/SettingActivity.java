package com.gu.android.mediacodec.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gu.android.mediacodec.R;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class SettingActivity extends AppCompatActivity {

  @BindView(R.id.toolBar)
  Toolbar mToolbar;

  @BindView(R.id.roomNo_ed)
  EditText roomNoEd;

  @OnClick(R.id.back_btn)
  public void onClickedBackBtn() {
    finish();
  }

  @BindView(R.id.go2myRoomBtn)
  Button go2myRoomBtn;

  @OnClick(R.id.go2myRoomBtn)
  public void onClickedGo2myRoomBtn() {
    String roomStr = roomNoEd.getText().toString();
    try {
      int roomNo = Integer.valueOf(roomStr);
      Intent intent = new Intent(this, RenderActivity.class);
      intent.putExtra("roomNo", roomNo);
      startActivity(intent);
      return;
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    Toast.makeText(getApplicationContext(), "请正确输入房间号", Toast.LENGTH_LONG).show();
  }

  private CompositeDisposable mCompositeDisposable;
  private boolean permissionExist;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.setting_layout);
    ButterKnife.bind(this);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    checkPermission();
    mCompositeDisposable = new CompositeDisposable();
    mCompositeDisposable.add(
        RxTextView.textChanges(roomNoEd)
            .subscribe(
                new Consumer<CharSequence>() {
                  @Override
                  public void accept(CharSequence charSequence) throws Exception {
                    go2myRoomBtn.setEnabled(permissionExist && charSequence.length() != 0);
                  }
                }));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mCompositeDisposable.dispose();
  }

  private void checkPermission() {
    if (!AndPermission.hasPermissions(this, Permission.RECORD_AUDIO, Permission.CAMERA)) {
      AndPermission.with(this)
          .runtime()
          .permission(Permission.CAMERA, Permission.RECORD_AUDIO)
          .onGranted(
              new Action<List<String>>() {
                @Override
                public void onAction(List<String> data) {
                  permissionExist = true;
                }
              })
          .onDenied(
              new Action<List<String>>() {
                @Override
                public void onAction(List<String> data) {
                  permissionExist = false;
                }
              })
          .start();
    } else {
      permissionExist = true;
    }
  }
}
