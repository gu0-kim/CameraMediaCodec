package com.gu.android.mediacodec.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.basemodule.data.Command;
import com.example.basemodule.data.ConfigData;
import com.example.basemodule.data.PeerData;
import com.example.basemodule.data.Port;
import com.example.basemodule.log.LogUtil;
import com.google.gson.Gson;
import com.gu.rtplibrary.rtp.RtpSenderWrapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.basemodule.data.Port.CLIENT_CONNECT_PORT;
import static com.example.basemodule.data.Port.CLIENT_DATA_PORT;

public class PushStreamServer extends Service {
  private ServiceBinder mServiceBinder = new ServiceBinder();

  private static final String TAG = PushStreamServer.class.getSimpleName();
  private Map<String, RtpSenderWrapper> peers;
  private byte[] videoConfigData;
  private byte[] audioConfigData;
  private volatile boolean videoConfigDataReady, audioConfigDataReady;
  private ExecutorService mExecutorService;
  private ServerConnectThread mServerTask;
  private ServerSend264Thread mServerSend264Thread;
  private int myRoomNumber = RESERVED;
  private static final int RESERVED = -1;
  private Callback mCallback;
  private ArrayBlockingQueue<byte[]> h264BlockingQueue;
  private final Object lock = new Object();
  private final Object configLock = new Object();
  private Handler mHandler;

  public interface Callback {
    void notifyConnectChanged(int num);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mHandler = new Handler();
    peers = new ConcurrentHashMap<>();
    h264BlockingQueue = new ArrayBlockingQueue<>(60);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mServiceBinder;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    closeClientSocket();
    peers.clear();
    mServerTask.stopThread();
    mServerSend264Thread.stopThread();
    mCallback = null;
    h264BlockingQueue.clear();
  }

  private void closeClientSocket() {
    for (String ip : peers.keySet()) {
      RtpSenderWrapper senderWrapper = peers.get(ip);
      if (senderWrapper != null) senderWrapper.close();
    }
  }

  public class ServiceBinder extends Binder {
    public void saveVideoConfigData(byte[] data) {
      synchronized (configLock) {
        videoConfigData = new byte[data.length];
        System.arraycopy(data, 0, videoConfigData, 0, data.length);
        videoConfigDataReady = true;
        configLock.notifyAll();
      }
    }

    public void saveAudioConfigData(byte[] data) {
      synchronized (configLock) {
        audioConfigData = new byte[data.length];
        System.arraycopy(data, 0, videoConfigData, 0, data.length);
        audioConfigDataReady = true;
        configLock.notifyAll();
      }
    }

    public void startPushStream() {
      mExecutorService = Executors.newFixedThreadPool(4);
      mServerTask = new ServerConnectThread();
      mServerTask.start();
      mServerSend264Thread = new ServerSend264Thread();
      mServerSend264Thread.start();
    }

    public void stopPushStream() {
      videoConfigDataReady = false;
      mServerTask.stopThread();
      mServerSend264Thread.stopThread();
      h264BlockingQueue.clear();
      synchronized (lock) {
        peers.clear();
      }
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

    public void add2BlockingQueue(byte[] data, int offset, int length) {
      byte[] h264 = new byte[length];
      System.arraycopy(data, offset, h264, 0, length);
      h264BlockingQueue.offer(h264);
    }
  }

  private class ServerSend264Thread extends Thread {
    private boolean stop;

    @Override
    public void run() {
      while (true) {
        try {
          byte[] sendByte = h264BlockingQueue.poll(100, TimeUnit.MILLISECONDS);
          if (stop) break;
          if (sendByte == null) continue;
          synchronized (lock) {
            for (String ip : peers.keySet()) {
              RtpSenderWrapper wrapper = peers.get(ip);
              if (wrapper != null) {
                wrapper.sendAvcPacket(sendByte, 0, sendByte.length, 0);
              }
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    void stopThread() {
      stop = true;
    }
  }

  private class ServerConnectThread extends Thread {
    private DatagramSocket mServerSocket;
    private byte[] data;

    ServerConnectThread() {
      try {
        mServerSocket = new DatagramSocket(Port.SERVER_CONNECT_PORT);
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
          LogUtil.log("收到用户观看请求,开始解析数据。");
          PeerData peerData = parseJson(data, 0, datagramPacket.getLength());
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
      LogUtil.log("client ip=" + ip + ",command=" + command + ",roomNum=" + roomNum);
      if (roomNum == myRoomNumber && !peers.containsKey(ip) && command.equals(Command.CONNECT)) {
        RtpSenderWrapper wrapper = new RtpSenderWrapper(ip, CLIENT_DATA_PORT, false);
        synchronized (lock) {
          peers.put(ip, wrapper);
          if (mCallback != null) {
            mHandler.post(
                new Runnable() {
                  @Override
                  public void run() {
                    mCallback.notifyConnectChanged(peers.size());
                  }
                });
          }
        }
        synchronized (configLock) {
          while (!videoConfigDataReady && !audioConfigDataReady) {
            try {
              configLock.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
        ConfigData configData = new ConfigData(videoConfigData, audioConfigData);
        mExecutorService.execute(new SendConfigRunnable(ip, parseConfigData(configData)));
      } else if (roomNum == myRoomNumber
          && peers.containsKey(ip)
          && command.equals(Command.DISCONNECT)) {
        synchronized (lock) {
          peers.remove(ip);
          if (mCallback != null) {
            mHandler.post(
                new Runnable() {
                  @Override
                  public void run() {
                    mCallback.notifyConnectChanged(peers.size());
                  }
                });
          }
        }
      }
    }
  }

  class SendConfigRunnable implements Runnable {

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
            new DatagramPacket(data, data.length, InetAddress.getByName(ip), CLIENT_CONNECT_PORT);
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

  private PeerData parseJson(byte[] data, int offset, int length) {
    String jsonStr = new String(data, offset, length);
    LogUtil.log(jsonStr);
    Gson gson = new Gson();
    PeerData peerData = null;
    try {
      peerData = gson.fromJson(jsonStr, PeerData.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return peerData;
  }

  private byte[] parseConfigData(ConfigData configData) {
    Gson gson = new Gson();
    String str = gson.toJson(configData, ConfigData.class);
    return str.getBytes();
  }

  private void log(String log) {
    Log.e(TAG, "------------------" + log + "------------------");
  }
}
