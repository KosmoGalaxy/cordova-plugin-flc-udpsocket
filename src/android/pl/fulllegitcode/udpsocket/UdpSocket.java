package pl.fulllegitcode.udpsocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.util.SparseArray;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
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

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("send") || action.equals("broadcast") || action.equals("receive")) {
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

    private void _executeNext() {
        if (cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE)
            && cordova.hasPermission(Manifest.permission.WAKE_LOCK)) {
            if (!_executions.isEmpty()) {
                Execution execution = _executions.get(0);
                _executions.remove(0);
                _execute(execution);
                _executeNext();
            }
            if (_wifiLock == null) {
                _lockWifi();
            }
        } else if (!_werePermissionsRequested) {
            _log("requesting permissions");
            _werePermissionsRequested = true;
            cordova.requestPermissions(this, 0, new String[] {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK
            });
        }
    }

    private void _execute(Execution execution) {
        String action = execution.action;
        JSONArray args = execution.args;
        CallbackContext callbackContext = execution.callbackContext;
        try {
//            _log(String.format(Locale.ENGLISH, "execute: id=%d action=%s", args.getInt(0), action));
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
            }
        } catch (Exception e) {
            _logError(e.getMessage());
        }
        callbackContext.error("error");
    }

    private void _send(int id, String ip, int port, String packetString) throws IOException {
        DatagramSocket socket = _getSocket(id);
        byte[] bytes = packetString.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
        socket.send(packet);
    }

    private void _broadcast(int id, int port, String packetString) throws IOException {
        DatagramSocket socket = _getSocket(id);
        InetAddress address = _getBroadcastAddress();
        if (address != null) {
            byte[] bytes = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            socket.send(packet);
        }
    }

    private void _receive(int id, int port, final CallbackContext callbackContext) throws IOException {
        final DatagramSocket socket = _getSocket(id);
        socket.bind(new InetSocketAddress(port));
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
            byte[] bytes = new byte[10 * 1024];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            try {
                while (true) {
                    socket.receive(packet);
                    JSONObject payload = new JSONObject();
                    payload.put("packet", new String(packet.getData(), 0, packet.getLength()));
                    payload.put("ip", packet.getAddress().getHostName());
                    payload.put("port", packet.getPort());
                    PluginResult result = new PluginResult(PluginResult.Status.OK, payload);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
            } catch (Exception e) {
                callbackContext.error("error");
            }
            }
        });
    }

    private DatagramSocket _getSocket(int id) throws SocketException {
        DatagramSocket socket = _sockets.get(id);
        if (socket == null) {
            socket = new DatagramSocket(null);
            _sockets.put(id, socket);
        }
        return socket;
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
        _wifiLock.acquire();
        _log(String.format("WifiLock: %b", _wifiLock.isHeld()));
    }

    private void _log(String message) {
        Log.d("FlcUdpSocket", message);
    }

    private void _logError(String message) {
        Log.e("FlcUdpSocket", message);
    }

}
