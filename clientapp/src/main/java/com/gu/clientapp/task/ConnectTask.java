package com.gu.clientapp.task;

import com.example.basemodule.data.Command;
import com.example.basemodule.data.ConnectPeer;
import com.example.basemodule.data.PeerData;
import com.example.basemodule.data.Port;
import com.example.basemodule.log.LogUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ConnectTask {

  private int roomNum;
  private InetAddress broadcastIp;
  private InetAddress localIp;
  private String userName;

  public ConnectTask(int room, String userName, InetAddress broadcastIp, InetAddress localIp) {
    this.roomNum = room;
    this.broadcastIp = broadcastIp;
    this.localIp = localIp;
    this.userName = userName;
  }

  /**
   * running in background thread
   *
   * @return the data of decoder csd-0.
   */
  public byte[] start2Connect() {
    DatagramSocket infoSocket = null;
    byte[] configData;
    try {
      infoSocket = new DatagramSocket(Port.CLIENT_CONNECT_PORT, localIp);
      byte[] data = makeJsonByte(Command.CONNECT);
      DatagramPacket datagramPacket =
          new DatagramPacket(data, 0, data.length, broadcastIp, Port.SERVER_CONNECT_PORT);
      infoSocket.send(datagramPacket);
      LogUtil.log("发送请求数据");
      LogUtil.log("等候数据...");
      DatagramPacket configPacket = startWaitingConfigData(infoSocket);
      LogUtil.log("收到csd-0数据");
      LogUtil.log("csd-0帧长度：" + configPacket.getLength());
      configData = new byte[configPacket.getLength()];
      System.arraycopy(configPacket.getData(), 0, configData, 0, configData.length);
      return configData;
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (infoSocket != null) {
        infoSocket.close();
      }
    }
    return null;
  }

  public void disconnectLiveRoom() {
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket(Port.CLIENT_CONNECT_PORT, localIp);
      byte[] data = makeJsonByte(Command.DISCONNECT);
      DatagramPacket datagramPacket =
          new DatagramPacket(data, 0, data.length, broadcastIp, Port.SERVER_CONNECT_PORT);
      socket.send(datagramPacket);
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (socket != null) {
        LogUtil.log("connect socket close!");
        socket.close();
      }
    }
  }

  /*
  等候csd-0数据
   */
  private DatagramPacket startWaitingConfigData(DatagramSocket socket) throws IOException {
    byte[] buf = new byte[1024];
    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
    socket.receive(datagramPacket);
    return datagramPacket;
  }

  /**
   * 构造PeerData,获取byte[]
   *
   * @return json string byte[]
   */
  private byte[] makeJsonByte(String command) {
    String ip = localIp.getHostAddress();
    LogUtil.log("本地ip=" + ip);
    ConnectPeer connectPeer = new ConnectPeer(ip, userName, 10, roomNum);
    PeerData peerData = new PeerData(connectPeer, command, "");
    Gson gson = new Gson();
    return gson.toJson(peerData, PeerData.class).getBytes();
  }
}
