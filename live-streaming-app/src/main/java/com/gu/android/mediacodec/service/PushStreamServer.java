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
  private ServerSendDataThread mServerSendDataThread;
  private int myRoomNumber = RESERVED;
  private static final int RESERVED = -1;
  private Callback mCallback;
  private ArrayBlockingQueue<byte[]> videoDataQueue, audioDataQueue;
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
    videoDataQueue = new ArrayBlockingQueue<>(60);
    audioDataQueue = new ArrayBlockingQueue<>(60);
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
    if (mServerTask != null) mServerTask.stopThread();
    if (mServerSendDataThread != null) mServerSendDataThread.stopThread();
    mCallback = null;
    videoDataQueue.clear();
    audioDataQueue.clear();
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
        System.arraycopy(data, 0, audioConfigData, 0, data.length);
        audioConfigDataReady = true;
        configLock.notifyAll();
      }
    }

    public void startPushStream() {
      mExecutorService = Executors.newFixedThreadPool(4);
      mServerTask = new ServerConnectThread();
      mServerTask.start();
      mServerSendDataThread = new ServerSendDataThread();
      mServerSendDataThread.start();
    }

    public void stopPushStream() {
      videoConfigDataReady = false;
      if (mServerTask != null) mServerTask.stopThread();
      if (mServerSendDataThread != null) mServerSendDataThread.stopThread();
      videoDataQueue.clear();
      audioDataQueue.clear();
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

    public void add2VideoQueue(byte[] data, int offset, int length) {
      byte[] h264 = new byte[length];
      System.arraycopy(data, offset, h264, 0, length);
      videoDataQueue.offer(h264);
    }

    public void add2AudioQueue(byte[] data, int offset, int length) {
      byte[] audioData = new byte[length];
      System.arraycopy(data, offset, audioData, 0, length);
      audioDataQueue.offer(audioData);
    }
  }

  private class ServerSendDataThread extends Thread {
    private boolean stop;

    @Override
    public void run() {
      while (true) {
        byte[] videoData = videoDataQueue.poll();
        byte[] audioData = audioDataQueue.poll();
        if (stop) break;
        if (videoData == null && audioData == null) continue;
        synchronized (lock) {
          for (String ip : peers.keySet()) {
            RtpSenderWrapper wrapper = peers.get(ip);
            if (wrapper != null) {
              if (videoData != null) wrapper.sendAvcPacket(videoData, 0, videoData.length, 0);
              if (audioData != null) wrapper.sendAacPacket(audioData, 0, audioData.length, 0);
            }
          }
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
