package pl.fulllegitcode.udpsocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.util.SparseArray;

import org.apache.cordova.CallbackContext;
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
import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class FlcUdpSocketPlugin extends CordovaPlugin {

    private class Execution {

        String action;
        JSONArray args;
        CallbackContext callbackContext;

        Execution(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }

    }



    private static FlcUdpSocketPlugin _instance;
    private static boolean _isDebug = false;

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



    private ArrayList<Execution> _executions = new ArrayList<Execution>();
    private SparseArray<Socket> _sockets = new SparseArray<Socket>();
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
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("setDebug")) {
            _isDebug = args.getBoolean(0);
            callbackContext.success();
            return true;
        }
        if (action.equals("create") || action.equals("send") || action.equals("broadcast") || action.equals("receive") || action.equals("close")) {
            _executions.add(new Execution(action, args, callbackContext));
            _executeNext();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        boolean canExecute = true;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = grantResults[i];
            log("permission " + permission + ": " + (result == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (result == PackageManager.PERMISSION_DENIED) {
                canExecute = false;
            }
        }
        if (canExecute) {
            _executeNext();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            _closeAllSockets();
            _unlockWifi();
            _unlockMulticast();
        } catch (Exception e) {
            logError(String.format(Locale.ENGLISH, "destroy error. message=%s", e.getMessage()));
        }
    }

    private void _lockWifi() {
        Context context = cordova.getActivity().getApplicationContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        _wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "UdpSocket");
        _wifiLock.setReferenceCounted(true);
        _wifiLock.acquire();
        log(String.format("acquire WifiLock: %b", _wifiLock.isHeld()));
    }

    private void _unlockWifi() {
        if (_wifiLock != null) {
            if (_wifiLock.isHeld()) {
                _wifiLock.release();
                log(String.format(Locale.ENGLISH, "release WifiLock: %b", _wifiLock.isHeld()));
            } else {
                logError("WifiLock was not held");
            }
        }
    }

    private void _lockMulticast() {
        Context context = cordova.getActivity().getApplicationContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        _multicastLock = wifiManager.createMulticastLock("UdpSocket");
        _multicastLock.setReferenceCounted(true);
        _multicastLock.acquire();
        log(String.format("acquire MulticastLock: %b", _multicastLock.isHeld()));
    }

    private void _unlockMulticast() {
        if (_multicastLock != null) {
            if (_multicastLock.isHeld()) {
                _multicastLock.release();
                log(String.format(Locale.ENGLISH, "release MulticastLock: %b", _wifiLock.isHeld()));
            } else {
                logError("MulticastLock was not held");
            }
        }
    }

    private void _executeNext() {
        if (cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE)
            && cordova.hasPermission(Manifest.permission.WAKE_LOCK)
            && cordova.hasPermission(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)) {
            if (!_executions.isEmpty()) {
                Execution execution = _executions.get(0);
                _executions.remove(0);
                _execute(execution);
                _executeNext();
            }
        } else if (!_werePermissionsRequested) {
            log("requesting permissions");
            _werePermissionsRequested = true;
            cordova.requestPermissions(this, 0, new String[] {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
            });
        }
    }

    private void _execute(Execution execution) {
        String action = execution.action;
        JSONArray args = execution.args;
        CallbackContext callbackContext = execution.callbackContext;
        try {
            if ("create".equals(action)) {
                _create(args.getInt(0), callbackContext);
            } else if ("send".equals(action)) {
                _send(args.getInt(0), args.getString(1), args.getInt(2), args.getString(3), callbackContext);
            } else if ("broadcast".equals(action)) {
                _broadcast(args.getInt(0), args.getInt(1), args.getString(2), callbackContext);
            } else if ("receive".equals(action)) {
                _receive(args.getInt(0), args.getInt(1), callbackContext);
            } else if ("close".equals(action)) {
                _close(args.getInt(0), callbackContext);
            }
        } catch (Exception e) {
            logError(String.format(Locale.ENGLISH, "error. message=%s", e.getMessage()));
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
                    Socket socket = new Socket(id);
                    _sockets.put(id, socket);
                    callbackContext.success();
                } catch (SocketException e) {
                    logError(String.format(Locale.ENGLISH, "create error. id=%d message=%s", id, e.getMessage()));
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void _send(final int id, final String ip, final int port, final String packetString, final CallbackContext callbackContext) {
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
                String error = socket.send(ip, port, packetString);
                if (error == null) {
                    callbackContext.success();
                } else {
                    callbackContext.error(error);
                }
            }
        });
    }

    private void _broadcast(int id, final int port, final String packetString, final CallbackContext callbackContext) {
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
                String error = socket.broadcast(port, packetString);
                if (error == null) {
                    callbackContext.success();
                } else {
                    callbackContext.error(error);
                }
            }
        });
    }

    private void _receive(final int id, final int port, final CallbackContext callbackContext) {
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
                socket.receive(port, new Socket.ReceiveCallback() {
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

    private boolean _socketExists(int id) {
        return _sockets.get(id) != null;
    }

    private Socket _getSocket(int id) {
        return _sockets.get(id);
    }

    private String _closeSocket(int id) {
        if (_socketExists(id)) {
            Socket socket = _getSocket(id);
            return _closeSocket(socket);
        } else {
            String reason = "socket not found";
            logError(String.format(Locale.ENGLISH, "close failed. id=%d reason=%s", id, reason));
            return reason;
        }
    }

    private String _closeSocket(Socket socket) {
        int index = _sockets.indexOfValue(socket);
        int id = _sockets.keyAt(index);
        if (!socket.isClosed()) {
            log(String.format(Locale.ENGLISH, "close. id=%d", id));
            socket.close();
            return null;
        } else {
            String reason = "socket already closed";
            logError(String.format(Locale.ENGLISH, "close failed. id=%d reason=%s", id, reason));
            return reason;
        }
    }

    private void _closeAllSockets() {
        log(String.format(Locale.ENGLISH, "close all sockets. numSockets=%d", _sockets.size()));
        for (int i = 0; i < _sockets.size(); i++) {
            int key = _sockets.keyAt(i);
            Socket socket = _sockets.get(key);
            _closeSocket(socket);
        }
    }

    private InetAddress _getBroadcastAddress() throws UnknownHostException {
        Context context = cordova.getActivity().getApplicationContext();
        WifiManager myWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null) {
            return null;
        }
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask) | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte)((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

}
