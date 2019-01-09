package com.gu.clientapp;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.gu.clientapp.mvp.view.ClientFragment;

public class ClientActivity extends AppCompatActivity {
  ClientFragment fragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    FragmentManager fragmentManager = getSupportFragmentManager();
    fragment = (ClientFragment) fragmentManager.findFragmentById(R.id.content_layout);
    if (fragment == null) {
      String tag = "client";
      fragment = ClientFragment.newInstance(tag);
      fragmentManager.beginTransaction().add(R.id.content_layout, fragment).commit();
    }
  }
}
