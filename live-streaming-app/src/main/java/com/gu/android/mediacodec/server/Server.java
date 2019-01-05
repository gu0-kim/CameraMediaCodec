package com.gu.android.mediacodec.server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.basemodule.data.ConnectPeer;
import com.example.basemodule.data.PeerData;
import com.example.basemodule.data.Port;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Service {
  private ServiceBinder mServiceBinder = new ServiceBinder();

  private static final String TAG = Server.class.getSimpleName();
  private Map<String, ConnectPeer> peers;
  private byte[] configData;
  private ExecutorService mExecutorService;
  private ServerThread mServerTask;
  private int myRoomNumber = RESERVED;
  private static final int RESERVED = -1;
  private Callback mCallback;

  public interface Callback {
    void notifyConnectChanged(int num);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    peers = new ConcurrentHashMap<>();
    mExecutorService = Executors.newFixedThreadPool(4);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mServiceBinder;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    peers.clear();
    mServerTask.stopThread();
    mCallback = null;
  }

  public class ServiceBinder extends Binder {
    public void saveConfigData(byte[] data) {
      configData = new byte[data.length];
      System.arraycopy(data, 0, configData, 0, data.length);
    }

    public void startServer() {
      mServerTask = new ServerThread();
      mServerTask.start();
    }

    public int getOnLineNumbers() {
      return peers.size();
    }

    public void setRoomNumber(int roomNumber) {
      myRoomNumber = roomNumber;
    }

    public void setCallback(Callback callback) {
      mCallback = callback;
    }
  }

  private class ServerThread extends Thread {
    private DatagramSocket mServerSocket;
    private byte[] data;

    ServerThread() {
      try {
        mServerSocket = new DatagramSocket(Port.SERVER_LOCAL_CONNECT_PORT);
        mServerSocket.setBroadcast(false);
      } catch (SocketException e) {
        e.printStackTrace();
      }
      data = new byte[512];
    }

    @Override
    public void run() {
      try {
        while (true) {
          DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
          mServerSocket.receive(datagramPacket);
          PeerData peerData = parseJson(data);
          doWork(peerData);
        }
      } catch (IOException e) {
        e.printStackTrace();
        log("by socket close");
      }
      mExecutorService.shutdown();
      log("ServerThread退出");
    }

    void stopThread() {
      mServerSocket.close();
    }
  }

  private void doWork(PeerData peerData) {
    if (peerData != null) {
      String ip = peerData.getPeerInfo().getIp();
      String command = peerData.getCommand();
      int roomNum = peerData.getPeerInfo().getRoomNumber();
      if (roomNum == myRoomNumber && !peers.containsKey(ip) && command.equals("connect")) {
        peers.put(ip, peerData.getPeerInfo());
        if (configData != null) {
          mExecutorService.execute(new SendConfigRunnable(ip, configData));
        }
        mCallback.notifyConnectChanged(peers.size());
      } else if (roomNum == myRoomNumber && peers.containsKey(ip) && command.equals("disconnect")) {
        peers.remove(ip);
        mCallback.notifyConnectChanged(peers.size());
      }
    }
  }

  class SendConfigRunnable implements Runnable {
    private static final int PORT_DEST = 5006;

    String ip;
    byte[] data;

    SendConfigRunnable(String ip, byte[] data) {
      this.ip = ip;
      this.data = data;
    }

    @Override
    public void run() {
      DatagramSocket socket = null;
      try {
        socket = new DatagramSocket();
        socket.setBroadcast(false);
        DatagramPacket datagramPacket =
            new DatagramPacket(data, data.length, InetAddress.getByName(ip), PORT_DEST);
        socket.send(datagramPacket);
        log("发送csd-0数据帧");
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (socket != null) {
          socket.close();
        }
      }
    }
  }

  private PeerData parseJson(byte[] data) {
    String jsonStr = new String(data);
    Gson gson = new Gson();
    PeerData peerData = null;
    try {
      peerData = gson.fromJson(jsonStr, PeerData.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return peerData;
  }

  private void log(String log) {
    Log.e(TAG, "------------------" + log + "------------------");
  }
}
