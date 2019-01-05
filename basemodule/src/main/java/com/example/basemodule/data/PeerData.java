package com.example.basemodule.data;

public class PeerData {
  private ConnectPeer peerInfo;
  private String command;
  private String appendData;

  public PeerData(ConnectPeer peerInfo, String command, String appendData) {
    this.peerInfo = peerInfo;
    this.command = command;
    this.appendData = appendData;
  }

  public ConnectPeer getPeerInfo() {
    return peerInfo;
  }

  public void setPeerInfo(ConnectPeer peerInfo) {
    this.peerInfo = peerInfo;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getAppendData() {
    return appendData;
  }

  public void setAppendData(String appendData) {
    this.appendData = appendData;
  }
}
