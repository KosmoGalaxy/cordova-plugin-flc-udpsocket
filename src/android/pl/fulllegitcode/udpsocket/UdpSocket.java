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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class UdpSocket extends CordovaPlugin {

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



    private ArrayList<Execution> _executions = new ArrayList<Execution>();
    private SparseArray<DatagramSocket> _sockets = new SparseArray<DatagramSocket>();
    private boolean _werePermissionsRequested = false;
    private WifiManager.WifiLock _wifiLock = null;
    private WifiManager.MulticastLock _multicastLock = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        _log("initialize");
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("send") || action.equals("broadcast") || action.equals("receive") || action.equals("close")) {
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
            _log("permission " + permission + ": " + (result == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
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
        _log("destroy");
        try {
            _closeAllSockets();
            _unlockWifi();
            _unlockMulticast();
        } catch (Exception e) {
            _logError(String.format(Locale.ENGLISH, "destroy error. message=%s", e.getMessage()));
        }
        super.onDestroy();
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
            if (_wifiLock == null) {
                _lockWifi();
            }
            if (_multicastLock == null) {
                _lockMulticast();
            }
        } else if (!_werePermissionsRequested) {
            _log("requesting permissions");
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
            if ("send".equals(action)) {
                _send(args.getInt(0), args.getString(1), args.getInt(2), args.getString(3));
                callbackContext.success();
                return;
            } else if ("broadcast".equals(action)) {
                _broadcast(args.getInt(0), args.getInt(1), args.getString(2));
                callbackContext.success();
                return;
            } else if ("receive".equals(action)) {
                _receive(args.getInt(0), args.getInt(1), callbackContext);
                return;
            } else if ("close".equals(action)) {
                _close(args.getInt(0));
                return;
            }
        } catch (Exception e) {
            _logError(String.format(Locale.ENGLISH, "error. message=%s", e.getMessage()));
        }
        callbackContext.error("error");
    }

    private void _send(int id, String ip, int port, String packetString) throws IOException {
        _log(String.format(Locale.ENGLISH, "send. id=%d address=%s:%d packet=%s", id, ip, port, packetString.substring(0, 100)));
        DatagramSocket socket = _getSocket(id);
        if (socket.isClosed()) {
            return;
        }
        byte[] bytes = packetString.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
        try {
            socket.send(packet);
        } catch (Exception e) {
            _logError(String.format(Locale.ENGLISH, "send error. id=%d message=%s", id, e.getMessage()));
        }
    }

    private void _broadcast(int id, int port, String packetString) throws IOException {
        DatagramSocket socket = _getSocket(id);
        if (socket.isClosed()) {
            return;
        }
        InetAddress address = _getBroadcastAddress();
        if (address != null) {
//            _log(String.format("broadcast: %s %s", address, packetString));
            byte[] bytes = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            try {
                socket.send(packet);
            } catch (Exception e) {
                _logError(String.format(Locale.ENGLISH, "broadcast error. id=%d ip=%s port=%d packet=%s message=%s", id, address, port, packetString, e.getMessage()));
            }
        } else {
            _logError(String.format(Locale.ENGLISH, "broadcast error. id=%d message=cannot resolve address", id));
        }
    }

    private void _receive(final int id, int port, final CallbackContext callbackContext) throws IOException {
        final DatagramSocket socket = _getSocket(id);
        if (socket.isClosed()) {
            return;
        }
        socket.bind(new InetSocketAddress(port));
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = new byte[8 * 1024];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                try {
                    while (!socket.isClosed()) {
                        socket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength());
                        String ip = packet.getAddress().getHostAddress();
                        int port = packet.getPort();
                        _log(String.format(Locale.ENGLISH, "receive. id=%d address=%s:%d packet=%s", id, ip, port, data.substring(0, 100)));
                        JSONObject payload = new JSONObject();
                        payload.put("packet", data);
                        payload.put("ip", ip);
                        payload.put("port", port);
                        PluginResult result = new PluginResult(PluginResult.Status.OK, payload);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                } catch (Exception e) {
                    _logError(String.format(Locale.ENGLISH, "receive error. id=%d message=%s", id, e.getMessage()));
                    callbackContext.error("error");
                }
            }
        });
    }

    private void _close(int id) throws SocketException {
        _closeSocket(id);
    }

    private boolean _socketExists(int id) {
        return _sockets.get(id) != null;
    }

    private DatagramSocket _getSocket(int id) throws SocketException {
        DatagramSocket socket = _sockets.get(id);
        if (socket == null) {
            socket = new DatagramSocket(null);
            socket.setBroadcast(true);
            socket.setReuseAddress(true);
            _sockets.put(id, socket);
        }
        return socket;
    }

    private void _closeSocket(int id) throws SocketException {
        if (_socketExists(id)) {
            DatagramSocket socket = _getSocket(id);
            _closeSocket(socket);
        } else {
            _log(String.format(Locale.ENGLISH, "close failed. id=%d reason=socket not found", id));
        }
    }

    private void _closeSocket(DatagramSocket socket) {
        int index = _sockets.indexOfValue(socket);
        int id = _sockets.keyAt(index);
        if (!socket.isClosed()) {
            _log(String.format(Locale.ENGLISH, "close. id=%d", id));
            socket.close();
        } else {
            _log(String.format(Locale.ENGLISH, "close failed. id=%d reason=socket already closed", id));
        }
    }

    private void _closeAllSockets() {
        _log(String.format(Locale.ENGLISH, "close all sockets. numSockets=%d", _sockets.size()));
        for (int i = 0; i < _sockets.size(); i++) {
            int key = _sockets.keyAt(i);
            DatagramSocket socket = _sockets.get(key);
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

    private void _lockWifi() {
        Context context = cordova.getActivity().getApplicationContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        _wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "UdpSocket");
        _wifiLock.setReferenceCounted(true);
        _wifiLock.acquire();
        _log(String.format("WifiLock: %b", _wifiLock.isHeld()));
    }

    private void _unlockWifi() {
        if (_wifiLock != null) {
            _wifiLock.release();
        }
    }

    private void _lockMulticast() {
        Context context = cordova.getActivity().getApplicationContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        _multicastLock = wifiManager.createMulticastLock("UdpSocket");
        _multicastLock.setReferenceCounted(true);
        _multicastLock.acquire();
        _log(String.format("MulticastLock: %b", _multicastLock.isHeld()));
    }

    private void _unlockMulticast() {
        if (_multicastLock != null) {
            _multicastLock.release();
        }
    }

    private void _log(String message) {
        Log.d("FlcUdpSocket", message);
    }

    private void _logError(String message) {
        Log.e("FlcUdpSocket", message);
    }

}
