package com.example.basemodule.data;

public class ConnectPeer {
  private String ip;
  private String userName;
  private int level;
  private int roomNumber;

  public ConnectPeer(String ip, String userName, int level, int roomNumber) {
    this.ip = ip;
    this.userName = userName;
    this.level = level;
    this.roomNumber = roomNumber;
  }

  public String getIp() {
    return ip;
  }

  public String getUserName() {
    return userName;
  }

  public int getLevel() {
    return level;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public void setRoomNumber(int roomNumber) {
    this.roomNumber = roomNumber;
  }

  public int getRoomNumber() {
    return roomNumber;
  }
}
