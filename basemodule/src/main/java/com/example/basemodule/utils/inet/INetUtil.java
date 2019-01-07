package com.example.basemodule.utils.inet;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;

public class INetUtil {
  private static final String LOG_TAG = INetUtil.class.getSimpleName();

  /*
  获取广播地址
   */
  public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
    WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
    DhcpInfo dhcp = wifi.getDhcpInfo();
    if (dhcp == null) {
      return InetAddress.getByName("255.255.255.255");
    }
    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++) quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    return InetAddress.getByAddress(quads);
  }

  /*
  获取本机ip地址
   */
  public static InetAddress getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
          en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
            enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
            return inetAddress;
          }
        }
      }
    } catch (SocketException ex) {
      Log.e(LOG_TAG, ex.toString());
    }
    return null;
  }

  public static String getIP(Context context) {
    WifiManager wifiService =
        (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
    WifiInfo wifiinfo = wifiService.getConnectionInfo();
    return intToIp(wifiinfo.getIpAddress());
  }

  private static String intToIp(int i) {
    return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
  }
}
