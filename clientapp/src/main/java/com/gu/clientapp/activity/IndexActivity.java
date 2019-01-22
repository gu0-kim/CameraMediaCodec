package com.gu.clientapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gu.clientapp.R;
import com.jakewharton.rxbinding2.widget.RxTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class IndexActivity extends AppCompatActivity {
  @BindView(R.id.toolBar)
  Toolbar mToolbar;

  @BindView(R.id.go2myRoomBtn)
  Button mButton;

  @BindView(R.id.roomNo_ed)
  EditText roomNoEd;

  private CompositeDisposable mCompositeDisposable;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.index_layout);
    ButterKnife.bind(this);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    mCompositeDisposable = new CompositeDisposable();
    mCompositeDisposable.add(
        RxTextView.textChanges(roomNoEd)
            .subscribe(
                new Consumer<CharSequence>() {
                  @Override
                  public void accept(CharSequence charSequence) throws Exception {
                    mButton.setEnabled(charSequence.length() != 0);
                  }
                }));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mCompositeDisposable.dispose();
  }

  @OnClick(R.id.go2myRoomBtn)
  public void onClickedGo2myRoomBtn() {
    try {
      Intent intent = new Intent(this, ClientActivity.class);
      int roomNo = Integer.valueOf(roomNoEd.getText().toString());
      intent.putExtra("roomNo", roomNo);
      startActivity(intent);
      return;
    } catch (Exception e) {
      e.printStackTrace();
    }
    Toast.makeText(getApplicationContext(), "请输入正确直播间号", Toast.LENGTH_LONG).show();
  }
}
