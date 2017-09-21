package pl.fulllegitcode.udpsocket;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.SparseArray;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

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

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("send") || action.equals("broadcast")) {
            _executions.add(new Execution(action, args, callbackContext));
            _executeNext();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            _executeNext();
        }
    }

    private void _executeNext() {
        if (cordova.hasPermission(Manifest.permission.ACCESS_WIFI_STATE)) {
            if (!_executions.isEmpty()) {
                Execution execution = _executions.get(0);
                _executions.remove(0);
                _execute(execution);
                _executeNext();
            }
        } else if (!_werePermissionsRequested) {
            _werePermissionsRequested = true;
            cordova.requestPermission(this, 0, Manifest.permission.ACCESS_WIFI_STATE);
        }
    }

    private void _execute(Execution execution) {
        String action = execution.action;
        JSONArray args = execution.args;
        CallbackContext callbackContext = execution.callbackContext;
        try {
            DatagramSocket socket = _getSocket(args.getInt(0));
            if (socket != null) {
                if ("send".equals(action)) {
                    if (_send(socket, args.getString(1), args.getInt(2), args.getString(3))) {
                        callbackContext.success();
                        return;
                    }
                } else if ("broadcast".equals(action)) {
                    if (_broadcast(socket, args.getInt(1), args.getString(2))) {
                        callbackContext.success();
                        return;
                    }
                }
            }
        } catch (Exception e) {}
        callbackContext.error("error");
    }

    private DatagramSocket _getSocket(int id) {
        DatagramSocket socket = _sockets.get(id);
        if (socket == null) {
            try {
                socket = new DatagramSocket();
                _sockets.put(id, socket);
            } catch (SocketException e) {
                return null;
            }
        }
        return socket;
    }

    private boolean _send(DatagramSocket socket, String ip, int port, String packetString) {
        try {
            byte[] bytes = packetString.getBytes();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(ip), port);
            socket.send(packet);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean _broadcast(DatagramSocket socket, int port, String packetString) {
        InetAddress address = _getBroadcastAddress();
        if (address != null) {
            try {
                byte[] bytes = packetString.getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
                socket.send(packet);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private InetAddress _getBroadcastAddress() {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }
}
