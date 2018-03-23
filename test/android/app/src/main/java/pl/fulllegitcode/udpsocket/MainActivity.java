package pl.fulllegitcode.udpsocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

  private static final String[] PERMISSIONS = new String[] {
    Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.CHANGE_WIFI_STATE,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.CHANGE_NETWORK_STATE,
  };

  private static byte[] convert2Bytes(int hostAddress) {
    byte[] addressBytes = { (byte)(0xff & hostAddress),
      (byte)(0xff & (hostAddress >> 8)),
      (byte)(0xff & (hostAddress >> 16)),
      (byte)(0xff & (hostAddress >> 24)) };
    return addressBytes;
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    _requestPermissions();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    boolean allGranted = true;
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        allGranted = false;
        break;
      }
    }
    if (allGranted) {
      _test();
    }
  }

  private void _requestPermissions() {
    ActivityCompat.requestPermissions(this, PERMISSIONS, 0);
  }

  private void _test() {
    try {
      Log.d("FlcUdpSocketTest", String.format(Locale.ENGLISH, "%s %s", _getIpAddress().getHostAddress(), _getBroadcastAddress()));
    } catch (Exception e) {
      Log.e("FlcUdpSocketTest", e.getMessage());
    }
  }

  private String _getOwnIp() {
    Context context = getApplicationContext();
    WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
    int ip = wifiManager.getConnectionInfo().getIpAddress();
    return ip != 0
      ? String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff))
      : "192.168.43.1";
  }

  private InetAddress _getBroadcastAddress() throws UnknownHostException {
    Context context = getApplicationContext();
    WifiManager myWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
    DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
    if (myDhcpInfo == null) {
      return null;
    }
    int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++) {
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    }
    return InetAddress.getByAddress(quads);
  }

  public InetAddress _getIpAddress() {
    InetAddress inetAddress = null;
    InetAddress myAddr = null;

    try {
      for (Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces(); networkInterface.hasMoreElements();) {

        NetworkInterface singleInterface = networkInterface.nextElement();

        for (Enumeration < InetAddress > IpAddresses = singleInterface.getInetAddresses(); IpAddresses
          .hasMoreElements();) {
          inetAddress = IpAddresses.nextElement();

          if (!inetAddress.isLoopbackAddress() && (singleInterface.getDisplayName()
            .contains("wlan0") ||
            singleInterface.getDisplayName().contains("eth0") ||
            singleInterface.getDisplayName().contains("ap0"))) {

            myAddr = inetAddress;
          }
        }
      }

    } catch (SocketException ex) {
      Log.e("FlcUdpSocketTest", ex.toString());
    }
    return myAddr;
  }

}
