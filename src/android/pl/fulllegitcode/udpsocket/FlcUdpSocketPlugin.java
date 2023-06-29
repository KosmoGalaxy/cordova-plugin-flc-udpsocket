package pl.fulllegitcode.udpsocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class FlcUdpSocketPlugin extends CordovaPlugin {

  private class Execution {

    String action;
    CordovaArgs args;
    CallbackContext callbackContext;

    Execution(String action, CordovaArgs args, CallbackContext callbackContext) {
      this.action = action;
      this.args = args;
      this.callbackContext = callbackContext;
    }

  }


  private static FlcUdpSocketPlugin _instance;
  private static boolean _isDebug = false;

  private static boolean _receiveFromOwnIp = true;

  public static boolean receiveFromOwnIp() {
    return _receiveFromOwnIp;
  }

  public static String getOwnIp() {
    return _instance._getOwnIp();
  }

  public static InetAddress getBroadcastAddress() throws UnknownHostException {
    return _instance._getBroadcastAddress();
  }

  public static void log(String message) {
    Log.d("FlcUdpSocket", message);
  }

  public static void logDebug(String message) {
    if (_isDebug) {
      Log.d("FlcUdpSocket", message);
    }
  }

  public static void logError(String message) {
    Log.e("FlcUdpSocket", message);
  }

  public static void logWarn(String message) {
    Log.w("FlcUdpSocket", message);
  }


  private ArrayList<Execution> _executions = new ArrayList<Execution>();
  private ArrayList<Socket> _sockets = new ArrayList<Socket>();
  private boolean _arePermissionsGranted = false;
  private boolean _werePermissionsRequested = false;
  private WifiManager.WifiLock _wifiLock = null;
  private WifiManager.MulticastLock _multicastLock = null;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    log("initialize");
    _instance = this;
    _lockWifi();
    _lockMulticast();
    if (cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE)
      && cordova.hasPermission(Manifest.permission.WAKE_LOCK)
      && cordova.hasPermission(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)) {
      _arePermissionsGranted = true;
    }
  }

  @Override
  public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) {
    if (action.equals("setDebug")) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            _isDebug = args.getBoolean(0);
            log(String.format(Locale.ENGLISH, "setDebug. value=%b", _isDebug));
            callbackContext.success();
          } catch (JSONException e) {
            callbackContext.error(e.getMessage());
          }
        }
      });
      return true;
    }
    if (action.equals("receiveFromOwnIp")) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            _receiveFromOwnIp = args.getBoolean(0);
            log(String.format(Locale.ENGLISH, "receiveFromOwnIp. value=%b", _receiveFromOwnIp));
            callbackContext.success();
          } catch (JSONException e) {
            callbackContext.error(e.getMessage());
          }
        }
      });
      return true;
    }
    if (action.equals("getBroadcastAddress")) {
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          try {
            callbackContext.success(this._getBroadcastAddress().getHostAddress());
          } catch (JSONException e) {
            callbackContext.error(e.getMessage());
          }
        }
      });
      return true;
    }
    if (action.equals("create") ||
      action.equals("send") ||
      action.equals("sendBytes") ||
      action.equals("broadcast") ||
      action.equals("broadcastBytes") ||
      action.equals("receive") ||
      action.equals("receiveBytes") ||
      action.equals("close")) {
      _executions.add(new Execution(action, args, callbackContext));
      _executeNext();
      return true;
    }
    return false;
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    boolean isAllGranted = true;
    for (int i = 0; i < permissions.length; i++) {
      String permission = permissions[i];
      int result = grantResults[i];
      log("permission " + permission + ": " + (result == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
      if (result == PackageManager.PERMISSION_DENIED) {
        isAllGranted = false;
      }
    }
    if (isAllGranted) {
      _arePermissionsGranted = true;
      _executeNext();
    }
  }

  @Override
  public void onReset() {
    log("reset");
    try {
      _closeAllSockets();
    } catch (Exception e) {
      logError(String.format(Locale.ENGLISH, "reset error. message=%s", e.getMessage()));
    }
    super.onReset();
  }

  @Override
  public void onDestroy() {
    log("destroy");
    try {
      _closeAllSockets();
      _unlockWifi();
      _unlockMulticast();
    } catch (Exception e) {
      logError(String.format(Locale.ENGLISH, "destroy error. message=%s", e.getMessage()));
    }
    super.onDestroy();
  }

  private void _lockWifi() {
    Context context = cordova.getActivity().getApplicationContext();
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    _wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FlcUdpSocket");
    _wifiLock.setReferenceCounted(true);
    _wifiLock.acquire();
    log(String.format("acquire WifiLock: %b", _wifiLock.isHeld()));
  }

  private void _unlockWifi() {
    if (_wifiLock != null && _wifiLock.isHeld()) {
      _wifiLock.release();
    }
  }

  private void _lockMulticast() {
    Context context = cordova.getActivity().getApplicationContext();
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    _multicastLock = wifiManager.createMulticastLock("FlcUdpSocket");
    _multicastLock.setReferenceCounted(true);
    _multicastLock.acquire();
    log(String.format("acquire MulticastLock: %b", _multicastLock.isHeld()));
  }

  private void _unlockMulticast() {
    if (_multicastLock != null && _multicastLock.isHeld()) {
      _multicastLock.release();
    }
  }

  private void _executeNext() {
    if (_arePermissionsGranted) {
      if (!_executions.isEmpty()) {
        Execution execution = _executions.get(0);
        _executions.remove(0);
        _execute(execution);
        _executeNext();
      }
    } else if (!_werePermissionsRequested) {
      log("requesting permissions");
      _werePermissionsRequested = true;
      cordova.requestPermissions(this, 0, new String[]{
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
      });
    }
  }

  private void _execute(Execution execution) {
    String action = execution.action;
    CordovaArgs args = execution.args;
    CallbackContext callbackContext = execution.callbackContext;
    try {
      if ("create".equals(action))
        _create(args.getInt(0), callbackContext);
      else if ("send".equals(action))
        _send(args.getInt(0), args.getString(1), args.getInt(2), args.getString(3).getBytes(), callbackContext);
      else if ("sendBytes".equals(action))
        _send(args.getInt(0), args.getString(1), args.getInt(2), args.getArrayBuffer(3), callbackContext);
      else if ("broadcast".equals(action))
        _broadcast(args.getInt(0), args.getInt(1), args.getString(2).getBytes(), callbackContext);
      else if ("broadcastBytes".equals(action))
        _broadcast(args.getInt(0), args.getInt(1), args.getArrayBuffer(2), callbackContext);
      else if ("receive".equals(action))
        _receive(args.getInt(0), args.getInt(1), ReceiveFormat.String, callbackContext);
      else if ("receiveBytes".equals(action))
        _receive(args.getInt(0), args.getInt(1), ReceiveFormat.Bytes, callbackContext);
      else if ("close".equals(action))
        _close(args.getInt(0), callbackContext);
    } catch (Exception e) {
      logError(String.format(Locale.ENGLISH, "error. message=%s", e.getMessage()));

      for (StackTraceElement element : e.getStackTrace())
        logError(element.toString());
    }
  }

  private void _create(final int id, final CallbackContext callbackContext) {
    if (_getSocket(id) != null) {
      String message = "socket already exists";
      logError(String.format(Locale.ENGLISH, "create error. id=%d message=%s", id, message));
      callbackContext.error(message);
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        try {
          Socket socket = new Socket(null, id);
          _sockets.add(socket);
          callbackContext.success();
        } catch (SocketException e) {
          logError(String.format(Locale.ENGLISH, "create error. id=%d message=%s", id, e.getMessage()));
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private void _send(final int id, final String ip, final int port, final byte[] bytes, final CallbackContext callbackContext) {
    final Socket socket = _getSocket(id);
    if (socket == null) {
      String message = "socket not found";
      logError(String.format(Locale.ENGLISH, "send error. id=%d message=%s", id, message));
      callbackContext.error(message);
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        String error = socket.send(ip, port, bytes);
        if (error == null) {
          callbackContext.success();
        } else {
          callbackContext.error(error);
        }
      }
    });
  }

  private void _broadcast(int id, final int port, final byte[] bytes, final CallbackContext callbackContext) {
    final Socket socket = _getSocket(id);
    if (socket == null) {
      String message = "socket not found";
      logError(String.format(Locale.ENGLISH, "broadcast error. id=%d message=%s", id, message));
      callbackContext.error(message);
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        String error = socket.broadcast(port, bytes);
        if (error == null) {
          callbackContext.success();
        } else {
          callbackContext.error(error);
        }
      }
    });
  }

  private void _receive(final int id, final int port, final ReceiveFormat format, final CallbackContext callbackContext) {
    final Socket socket = _getSocket(id);
    if (socket == null) {
      String message = "socket not found";
      logError(String.format(Locale.ENGLISH, "receive error. id=%d message=%s", id, message));
      callbackContext.error(message);
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        logDebug(String.format(Locale.ENGLISH, "receive thread priority: %d", Thread.currentThread().getPriority()));
        socket.receive(port, format, new Socket.ReceiveCallback() {
          @Override
          public void next(String ip, int port, byte[] bytes)
          {
            try {
              int ipLength = ip.length();
              int bytesLength = bytes.length;
              int length = 1 + ipLength + 4 + bytesLength;
              byte[] payload = new byte[length];
              ByteBuffer bb = ByteBuffer.wrap(payload);
              bb.put((byte) ipLength);
              System.arraycopy(ip.getBytes(), 0, payload, 1, ipLength);
              bb.putInt(1 + ipLength, port);
              System.arraycopy(bytes, 0, payload, 1 + ipLength + 4, bytesLength);
              PluginResult result = new PluginResult(PluginResult.Status.OK, payload);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } catch (Exception e) {
              logError(String.format(Locale.ENGLISH, "receive error. id=%d message=%s", id, e.getMessage()));
            }
          }

          @Override
          public void next(String ip, int port, String packet) {
            try {
              JSONObject payload = new JSONObject();
              payload.put("ip", ip);
              payload.put("port", port);
              payload.put("packet", packet);
              PluginResult result = new PluginResult(PluginResult.Status.OK, payload);
              result.setKeepCallback(true);
              callbackContext.sendPluginResult(result);
            } catch (Exception e) {
              logError(String.format(Locale.ENGLISH, "receive error. id=%d message=%s", id, e.getMessage()));
            }
          }

          @Override
          public void error(String message) {
            callbackContext.error(message);
          }
        });
      }
    });
  }

  private void _close(int id, CallbackContext callbackContext) {
    String error = _closeSocket(id);
    if (error == null) {
      callbackContext.success();
    } else {
      callbackContext.error(error);
    }
  }

  private Socket _getSocket(int id) {
    /*logDebug(String.format(Locale.ENGLISH, "sockets. numSockets=%d", _sockets.size()));*/
    for (int i = 0; i < _sockets.size(); i++) {
      Socket socket = _sockets.get(i);
      if (socket.id() == id) {
        return socket;
      }
    }
    return null;
  }

  private String _closeSocket(int id) {
    Socket socket = _getSocket(id);
    if (socket != null) {
      return _closeSocket(socket);
    } else {
      String reason = "socket not found";
      logError(String.format(Locale.ENGLISH, "close failed. id=%d reason=%s", id, reason));
      return reason;
    }
  }

  private String _closeSocket(Socket socket) {
    if (!socket.isClosed()) {
      log(String.format(Locale.ENGLISH, "close. id=%d", socket.id()));
      socket.close();
      _sockets.remove(socket);
      return null;
    } else {
      String reason = "socket already closed";
      logError(String.format(Locale.ENGLISH, "close failed. id=%d reason=%s", socket.id(), reason));
      return reason;
    }
  }

  private void _closeAllSockets() {
    log(String.format(Locale.ENGLISH, "close all sockets. numSockets=%d", _sockets.size()));
    for (int i = 0; i < _sockets.size(); i++) {
      _closeSocket(_sockets.get(i));
    }
  }

  private String _getOwnIp() {
    Context context = cordova.getActivity().getApplicationContext();
    WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
    int ip = wifiManager.getConnectionInfo().getIpAddress();
    return ip != 0
      ? String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff))
      : "192.168.43.1";
  }

  private InetAddress _getBroadcastAddress() throws UnknownHostException {
    try
    {
      Context context = cordova.getActivity().getApplicationContext();
      WifiManager myWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
      DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
      if (myDhcpInfo == null)
      {
        logWarn("could not resolve broadcast address. using default broadcast address");
        return InetAddress.getByName("255.255.255.255");
      }
      int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
      byte[] quads = new byte[4];
      for (int k = 0; k < 4; k++)
      {
        quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
      }
      return InetAddress.getByAddress(quads);
    }
    catch (Exception e)
    {
      logWarn("could not resolve broadcast address. using default broadcast address");
      return InetAddress.getByName("255.255.255.255");
    }
  }

}
